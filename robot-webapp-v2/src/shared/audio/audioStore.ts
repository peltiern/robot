import { create } from 'zustand'

/** Décode une chaîne base64 en ArrayBuffer (sans dépendance externe). */
function base64ToArrayBuffer(b64: string): ArrayBuffer {
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes.buffer
}

/** Silence (ms) au bout duquel on considère que le robot a fini sa phrase. */
const FIN_DE_PAROLE_MS = 700

// Contexte audio et curseur d'enchaînement : hors du store, ce ne sont pas des
// données d'affichage et leur mutation ne doit déclencher aucun rendu.
let contexte: AudioContext | null = null
let prochainDebut = 0
let minuterieParole: number | undefined

interface AudioState {
  /** Son coupé côté navigateur (le robot parle quand même, on ne l'entend pas). */
  muet: boolean
  /** Le robot est en train de parler : des trames arrivent en ce moment. */
  parle: boolean
  basculerSon: () => void
  /** Trame reçue sur `/audio` — appelé par `AudioProvider`. */
  traiterTrame: (audioContentBase64: string) => void
  /** Libère le contexte audio (démontage de l'application). */
  fermer: () => void
}

/**
 * Lecture du flux audio du robot (topic `/audio`, trames `audioContentBase64`).
 *
 * Muet par défaut : les navigateurs interdisent de démarrer un `AudioContext`
 * sans geste utilisateur, donc le son ne s'active qu'au toucher du bouton du
 * rail (qui réveille le contexte). Les trames sont enchaînées séquentiellement
 * — chacune démarre à la fin de la précédente — pour éviter chevauchements et
 * trous.
 *
 * Sorti de la page vidéo et remonté dans un store parce que le HUD n'a plus de
 * page : le bouton du son est dans le rail, l'indicateur « le robot parle » est
 * dans la barre d'écoute, et la réception ne doit pas s'arrêter quand on passe
 * à l'atelier.
 */
export const useAudioStore = create<AudioState>((set, get) => ({
  muet: true,
  parle: false,

  basculerSon() {
    const muet = !get().muet
    if (muet) {
      void contexte?.suspend()
    } else {
      // Ce geste utilisateur est ce qui autorise le navigateur à sortir du silence.
      contexte ??= new AudioContext()
      void contexte.resume()
      prochainDebut = contexte.currentTime
    }
    set({ muet })
  },

  traiterTrame(audioContentBase64) {
    // Le robot parle, qu'on l'écoute ou non : l'indicateur ne dépend pas du son.
    if (!get().parle) set({ parle: true })
    clearTimeout(minuterieParole)
    minuterieParole = window.setTimeout(() => set({ parle: false }), FIN_DE_PAROLE_MS)

    if (get().muet || !contexte) return
    const ctx = contexte
    ctx
      .decodeAudioData(base64ToArrayBuffer(audioContentBase64))
      .then((buffer) => {
        const source = ctx.createBufferSource()
        source.buffer = buffer
        source.connect(ctx.destination)
        const debut = Math.max(ctx.currentTime, prochainDebut)
        source.start(debut)
        prochainDebut = debut + buffer.duration
      })
      .catch(() => {
        /* trame non décodable : ignorée */
      })
  },

  fermer() {
    clearTimeout(minuterieParole)
    void contexte?.close()
    contexte = null
    set({ parle: false })
  },
}))
