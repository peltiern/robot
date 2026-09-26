import { create } from 'zustand'
import { bibliotheque as bibliothequeDesSons } from '../../studio/utils/bibliotheque'
import { rendreSon } from '../../studio/synthese/moteur'

/**
 * Les sons de la piste Son, vus de l'Atelier : leur durée, leur onde, et de quoi les entendre en
 * simulation.
 *
 * Un son vient d'abord du robot, en WAV — c'est exactement ce qu'il jouera. Robot éteint, il est
 * refait depuis sa recette, gardée dans le navigateur par le Studio : l'Atelier se travaille hors
 * ligne, et une piste Son muette ou sans longueur n'aiderait pas à caler un bruitage sur un geste.
 *
 * Chaque son n'est chargé qu'une fois par session. Un son retouché dans le Studio entre-temps
 * garde donc sa longueur d'avant jusqu'au rechargement de la page.
 */

/** Nombre de colonnes de l'onde miniature dessinée dans le bloc. */
const COLONNES = 240

export type EtatSon =
  | { etat: 'chargement' }
  | { etat: 'absent' }
  | { etat: 'pret'; duree: number; cretes: number[] }

interface EtatSons {
  sons: Record<string, EtatSon>
  demander: (nom: string) => void
}

const tampons = new Map<string, AudioBuffer>()
let contexte: AudioContext | null = null
const enCours = new Set<AudioBufferSourceNode>()

function contexteAudio(): AudioContext {
  if (!contexte) contexte = new AudioContext()
  if (contexte.state === 'suspended') void contexte.resume()
  return contexte
}

/** Le WAV du robot d'abord ; à défaut, la recette gardée par le Studio, refaite ici. */
async function chargerTampon(nom: string): Promise<AudioBuffer | null> {
  try {
    const reponse = await fetch(`/api/sons/${encodeURIComponent(nom)}/audio`)
    if (reponse.ok) return await contexteAudio().decodeAudioData(await reponse.arrayBuffer())
  } catch {
    // Robot injoignable : on tente la recette.
  }
  try {
    const recette = await bibliothequeDesSons.charger(nom)
    return await rendreSon(recette)
  } catch {
    return null
  }
}

function cretesDe(tampon: AudioBuffer): number[] {
  const echantillons = tampon.getChannelData(0)
  const parColonne = Math.max(1, Math.floor(echantillons.length / COLONNES))
  const cretes: number[] = []
  for (let c = 0; c < COLONNES; c++) {
    let crete = 0
    for (let i = c * parColonne; i < Math.min(echantillons.length, (c + 1) * parColonne); i++) {
      crete = Math.max(crete, Math.abs(echantillons[i]))
    }
    cretes.push(crete)
  }
  return cretes
}

export const useSonsAtelier = create<EtatSons>((set, get) => ({
  sons: {},

  demander(nom) {
    if (get().sons[nom]) return
    set(s => ({ sons: { ...s.sons, [nom]: { etat: 'chargement' } } }))
    void chargerTampon(nom).then(tampon => {
      if (tampon) tampons.set(nom, tampon)
      set(s => ({
        sons: {
          ...s.sons,
          [nom]: tampon ? { etat: 'pret', duree: tampon.duration, cretes: cretesDe(tampon) } : { etat: 'absent' },
        },
      }))
    })
  },
}))

/**
 * Fait entendre un son dans le navigateur, en simulation. `depuis` le prend en cours de route :
 * une lecture lancée au milieu d'un bruitage l'entend à partir de là, pas du début.
 */
export function jouerDansLeNavigateur(nom: string, depuis = 0) {
  const tampon = tampons.get(nom)
  if (!tampon || depuis >= tampon.duration) return
  const ctx = contexteAudio()
  const source = ctx.createBufferSource()
  source.buffer = tampon
  source.connect(ctx.destination)
  source.onended = () => enCours.delete(source)
  source.start(0, depuis)
  enCours.add(source)
}

/** Coupe tout ce que la simulation fait entendre : pause, arrêt, ou passage au robot. */
export function couperLeNavigateur() {
  enCours.forEach(source => {
    try {
      source.stop()
    } catch {
      // déjà fini
    }
  })
  enCours.clear()
}

/** Les sons qu'on peut poser : ceux du robot, et ceux que le Studio garde en attendant de les lui envoyer. */
export async function sonsDisponibles(): Promise<string[]> {
  const liste = await bibliothequeDesSons.lister()
  return [...new Set([...(liste.robot ?? []), ...liste.enAttente])].sort((a, b) =>
    a.toLowerCase().localeCompare(b.toLowerCase(), 'fr'),
  )
}
