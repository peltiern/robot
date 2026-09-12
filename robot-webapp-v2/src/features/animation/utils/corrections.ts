import type { Axe } from '../../../shared/types/animation'
import type { EditorKeyframe, EditorTrack } from '../store/animationStore'
import { dureeMinimale, pireDepassement, verifier, type Avertissement, type Depassement } from './verificateur'

/**
 * Les corrections des avertissements du vérificateur, une par nature.
 *
 * Chacune repart de l'état COURANT et non des chiffres de l'avertissement : une correction en
 * déplace d'autres — allonger le temps décale les instants de toutes les pistes — et un retard
 * calculé avant l'une serait faux après.
 */

/** Ce qu'une correction modifie : les pistes, et la durée quand elle allonge le temps. */
export interface EtatEditeur {
  tracks: EditorTrack[]
  totalMs: number
}

/** La grille des images-clés : un allongement tombe dessus, comme tout ce qu'on pose à la souris. */
const GRILLE_MS = 50

/** Garde-fou de la correction d'un dépassement, qui procède par petits pas. */
const ESSAIS_MAX = 40

export function corriger(etat: EtatEditeur, avertissement: Avertissement): EtatEditeur {
  if (avertissement.nature === 'horsButee') return ramenerDansLesButees(etat, avertissement)
  if (avertissement.nature === 'depassement') return baisserLeSommet(etat, avertissement.axe)
  return allongerLeTemps(etat, avertissement)
}

/**
 * Corrige tant que ça avance. Une correction peut en rendre d'autres inutiles — allonger une
 * transition détend ses voisines — d'où la revérification à chaque passe, et l'arrêt dès qu'une
 * passe ne gagne plus rien : ce qui reste n'a pas de correction automatique.
 */
export function corrigerTout(etat: EtatEditeur): EtatEditeur {
  let courant = etat
  let restants = verifier(courant.tracks).length
  for (let passe = 0; passe < 10 && restants > 0; passe++) {
    for (const avertissement of verifier(courant.tracks)) courant = corriger(courant, avertissement)
    const apres = verifier(courant.tracks).length
    if (apres >= restants) break
    restants = apres
  }
  return courant
}

/** Ce que fera le bouton, dit avant d'appuyer. */
export function explication(avertissement: Avertissement): string {
  if (avertissement.nature === 'horsButee') return "Ramène l'image-clé sur la butée"
  if (avertissement.nature === 'depassement') {
    return "Rapproche du centre les images-clés du sommet, jusqu'à ce que la courbe tienne dans les butées"
  }
  const decalage = Math.ceil((avertissement.retardMs ?? 0) / GRILLE_MS) * GRILLE_MS
  return `Ajoute ${decalage} ms à partir de ${avertissement.instantFin} ms, sur toutes les pistes`
}

/**
 * L'image-clé revient sur la butée. Posée pile dessus après une montée, elle peut faire déborder
 * la spline : le dépassement est corrigé dans la foulée, sans quoi le bouton ferait apparaître
 * l'avertissement suivant au lieu de régler le problème.
 */
function ramenerDansLesButees(etat: EtatEditeur, avertissement: Avertissement): EtatEditeur {
  const tracks = etat.tracks.map(tr => tr.id !== avertissement.axe ? tr : {
    ...tr,
    kfs: tr.kfs.map(k => k.id !== avertissement.idImageCle ? k
      : { ...k, v: Math.max(tr.min, Math.min(tr.max, k.v)) }),
  })
  return baisserLeSommet({ ...etat, tracks }, avertissement.axe)
}

/**
 * Rapproche du centre les images-clés qui encadrent le pire dépassement, pas à pas, jusqu'à ce que
 * la courbe tienne. Seules bougent celles du sommet : l'autre encadrante, plus loin de la butée,
 * n'y est pour rien. C'est ce qui a été fait à la main pour Salut, clés passées de 5,5 à 3,0.
 */
function baisserLeSommet(etat: EtatEditeur, axe: Axe): EtatEditeur {
  const initiale = etat.tracks.find(t => t.id === axe)
  if (!initiale) return etat
  let piste: EditorTrack = initiale
  for (let essai = 0; essai < ESSAIS_MAX; essai++) {
    const pire: Depassement | null = pireDepassement(piste)
    if (!pire) break
    const instant = pire.instant
    const triees: EditorKeyframe[] = [...piste.kfs].sort((a, b) => a.t - b.t)
    const i = triees.findIndex(k => k.t > instant)
    if (i < 1) break
    const encadrantes: EditorKeyframe[] = [triees[i - 1], triees[i]]
    const versLeHaut: boolean = pire.valeur > piste.max
    const valeurs: number[] = encadrantes.map(k => k.v)
    const sommet: number = versLeHaut ? Math.max(...valeurs) : Math.min(...valeurs)
    const aDeplacer: Set<string> = new Set(encadrantes.filter(k => Math.abs(k.v - sommet) < 0.05).map(k => k.id))
    const pas: number = Math.ceil((pire.ecart + 0.05) * 10) / 10
    piste = {
      ...piste,
      kfs: piste.kfs.map(k => !aDeplacer.has(k.id) ? k
        : { ...k, v: Math.round((versLeHaut ? k.v - pas : k.v + pas) * 10) / 10 }),
    }
  }
  const corrigee = piste
  return { ...etat, tracks: etat.tracks.map(t => t.id === axe ? corrigee : t) }
}

/**
 * Donne à la transition le temps qui lui manque, en décalant son arrivée et tout ce qui suit —
 * sur toutes les pistes : décaler un seul axe désynchroniserait le geste, la tête finissant de
 * tourner après que les yeux ont fini de se lever. Le geste garde sa forme, il ralentit là.
 */
function allongerLeTemps(etat: EtatEditeur, avertissement: Avertissement): EtatEditeur {
  const piste = etat.tracks.find(t => t.id === avertissement.axe)
  if (!piste) return etat
  const triees = [...piste.kfs].sort((a, b) => a.t - b.t)
  const i = triees.findIndex(k => k.id === avertissement.idImageCle)
  if (i < 1) return etat
  const depart = triees[i - 1], arrivee = triees[i]
  const necessaireMs = dureeMinimale(Math.abs(arrivee.v - depart.v), piste.defaultVelocity, piste.defaultAcceleration) * 1000
  const manqueMs = necessaireMs - (arrivee.t - depart.t)
  if (!(manqueMs > 0) || !Number.isFinite(manqueMs)) return etat
  const decalage = Math.ceil(manqueMs / GRILLE_MS) * GRILLE_MS
  const seuil = arrivee.t
  return {
    tracks: etat.tracks.map(tr => ({ ...tr, kfs: tr.kfs.map(k => k.t >= seuil ? { ...k, t: k.t + decalage } : k) })),
    totalMs: etat.totalMs + decalage,
  }
}
