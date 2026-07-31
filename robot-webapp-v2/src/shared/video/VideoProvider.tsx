import { useCallback, useEffect, useRef } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { useTopic } from '../websocket/useTopic'
import { useWebSocketStore } from '../websocket/websocketStore'
import { useVideoStore } from './videoStore'
import type { VideoEvent } from '../types/events'

/** Intervalle minimal entre deux rafraîchissements de l'indicateur FPS. */
const PERIODE_MAJ_FPS_MS = 500

/** Décode une chaîne base64 en Uint8Array (sans dépendance externe). */
function base64ToBytes(b64: string): Uint8Array<ArrayBuffer> {
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}

/**
 * Composant sans rendu : alimente le store vidéo depuis `/video`. À monter une
 * seule fois, au niveau du Layout.
 */
export function VideoProvider() {
  const poserTrame = useVideoStore((s) => s.poserTrame)
  const majFps = useVideoStore((s) => s.majFps)
  const oublier = useVideoStore((s) => s.oublier)
  const url = useVideoStore((s) => s.trame?.url)
  const connecte = useWebSocketStore((s) => s.connected)

  // Fenêtre glissante des horodatages d'affichage pour estimer le FPS.
  const horodatages = useRef<number[]>([])
  const derniereMajFps = useRef(0)

  // Dernière trame reçue, en attente d'affichage, et rAF programmé pour l'afficher.
  const trameEnAttente = useRef<VideoEvent | null>(null)
  const rafEnCours = useRef<number | null>(null)

  /**
   * Publie la trame la plus récente reçue, au rythme du navigateur.
   *
   * Le WebSocket est fiable et ordonné : après un à-coup réseau, tout le retard
   * est livré d'un coup. Afficher chaque message de la rafale fait défiler la
   * vidéo en accéléré et décode une pile d'images déjà périmées. On ne garde donc
   * que la dernière : le rattrapage se fait en sautant les images, comme
   * n'importe quel flux live.
   */
  const publierTrameEnAttente = useCallback(() => {
    rafEnCours.current = null
    const event = trameEnAttente.current
    trameEnAttente.current = null
    if (!event) return

    // Blob URL plutôt que `data:image/jpeg;base64,…` : une data-URI oblige le
    // navigateur à ré-analyser puis mettre en cache une chaîne de ~100 Ko à
    // chaque trame, ce qui saccadait l'affichage et faisait enfler la mémoire.
    // Le blob est libéré dès la trame suivante. Le décodage base64 n'a lieu que
    // pour les trames réellement publiées.
    const url = URL.createObjectURL(
      new Blob([base64ToBytes(event.imageBase64)], { type: 'image/jpeg' }),
    )
    poserTrame({ url, faces: event.faces ?? [], objects: event.objects ?? [] })

    // FPS des images affichées (et non reçues) : c'est ce que l'œil voit, et ça
    // ne s'envole plus à chaque rafale. L'état n'est rafraîchi que deux fois par
    // seconde, sinon chaque trame provoquerait un second rendu complet.
    const maintenant = performance.now()
    const fenetre = horodatages.current
    fenetre.push(maintenant)
    while (fenetre.length > 0 && maintenant - fenetre[0] > 1000) fenetre.shift()
    if (derniereMajFps.current === 0) {
      // Première trame : on amorce la fenêtre sans publier un « 1 fps » trompeur.
      derniereMajFps.current = maintenant
    } else if (maintenant - derniereMajFps.current >= PERIODE_MAJ_FPS_MS) {
      derniereMajFps.current = maintenant
      majFps(fenetre.length)
    }
  }, [poserTrame, majFps])

  useTopic(
    '/video',
    useCallback(
      (msg: IMessage) => {
        let event: VideoEvent
        try {
          event = JSON.parse(msg.body)
        } catch {
          return
        }
        if (!event?.imageBase64) return

        // La trame précédente non encore publiée est simplement écrasée.
        trameEnAttente.current = event
        if (rafEnCours.current === null) {
          rafEnCours.current = requestAnimationFrame(publierTrameEnAttente)
        }
      },
      [publierTrameEnAttente],
    ),
  )

  // Libération du Blob URL précédent : le nettoyage s'exécute quand l'URL change,
  // donc une fois la nouvelle image posée dans le DOM (le composant qui l'affiche
  // se rend dans le même commit, puisqu'il lit le même store).
  useEffect(() => {
    return () => {
      if (url) URL.revokeObjectURL(url)
    }
  }, [url])

  // Liaison coupée : la dernière image reçue disparaît. Une vue figée sur un HUD
  // de pilotage est pire que pas d'image du tout — on croit voir ce que le robot
  // voit. La scène affiche alors « Robot déconnecté », comme au lancement.
  useEffect(() => {
    if (connecte) return
    if (rafEnCours.current !== null) {
      cancelAnimationFrame(rafEnCours.current)
      rafEnCours.current = null
    }
    trameEnAttente.current = null
    horodatages.current = []
    derniereMajFps.current = 0
    oublier()
  }, [connecte, oublier])

  useEffect(() => {
    return () => {
      if (rafEnCours.current !== null) cancelAnimationFrame(rafEnCours.current)
      oublier()
    }
  }, [oublier])

  return null
}
