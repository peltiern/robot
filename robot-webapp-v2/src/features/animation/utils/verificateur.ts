import type { Axe } from '../../../shared/types/animation'
import type { EditorTrack } from '../store/animationStore'
import { catmullRomAt } from './catmullRom'

/**
 * Ce que le robot ne saura pas suivre — le portage de VerificateurAnimation, côté éditeur.
 *
 * Porté plutôt que demandé au robot : les avertissements doivent venir pendant qu'on dessine, en
 * Simulation, robot éteint, et chacun porte de quoi être corrigé sur place. Deux implémentations
 * donc, comme pour l'interpolation, et gardées identiques de la même façon :
 * VerificateurAnimationTest rejoue les verdicts de ce fichier et doit trouver les mêmes.
 *
 * Il avertit sans rien refuser : une animation trop rapide reste jouable, elle sera traînarde.
 */

/** Même pas que VerificateurAnimation : un écart que l'un verrait et pas l'autre les ferait diverger. */
const PAS_ECHANTILLONNAGE_MS = 10

export type NatureAvertissement = 'horsButee' | 'depassement' | 'tropRapide'

export interface Avertissement {
  nature: NatureAvertissement
  axe: Axe
  libelle: string
  instantDebut: number
  instantFin: number
  message: string
  /** L'image-clé en cause : celle qui sort des butées, ou l'arrivée d'une transition trop rapide. */
  idImageCle?: string
  /** Pour une transition trop rapide, le temps qui lui manque. */
  retardMs?: number
}

export interface Depassement {
  instant: number
  valeur: number
  ecart: number
}

/** Tous les avertissements des pistes actives, dans l'ordre des pistes puis du temps. */
export function verifier(pistes: EditorTrack[]): Avertissement[] {
  const avertissements: Avertissement[] = []
  for (const piste of pistes) {
    // Une piste désactivée ne part pas au robot : la juger ne ferait que du bruit.
    if (!piste.enabled || piste.kfs.length === 0) continue
    const triees = [...piste.kfs].sort((a, b) => a.t - b.t)
    const base = { axe: piste.id, libelle: piste.name }

    let toutesDedans = true
    for (const k of triees) {
      if (k.v < piste.min || k.v > piste.max) {
        toutesDedans = false
        avertissements.push({
          ...base, nature: 'horsButee', instantDebut: k.t, instantFin: k.t, idImageCle: k.id,
          message: `Position ${k.v.toFixed(1)}° hors des butées [${piste.min.toFixed(1)}° ; ${piste.max.toFixed(1)}°]`,
        })
      }
    }

    // Tant qu'une image-clé sort des butées, le dépassement de la courbe n'apprend rien de plus.
    if (toutesDedans) {
      const pire = pireDepassement(piste)
      if (pire) {
        avertissements.push({
          ...base, nature: 'depassement', instantDebut: pire.instant, instantFin: pire.instant,
          message: `La courbe dépasse jusqu'à ${pire.valeur.toFixed(1)}° vers ${pire.instant} ms, hors des butées `
            + `[${piste.min.toFixed(1)}° ; ${piste.max.toFixed(1)}°], alors que les images-clés y tiennent`,
        })
      }
    }

    for (let i = 0; i < triees.length - 1; i++) {
      const depart = triees[i], arrivee = triees[i + 1]
      const distance = Math.abs(arrivee.v - depart.v)
      if (distance === 0) continue
      const disponibleMs = arrivee.t - depart.t
      const necessaire = dureeMinimale(distance, piste.defaultVelocity, piste.defaultAcceleration)
      if (necessaire * 1000 > disponibleMs) {
        avertissements.push({
          ...base, nature: 'tropRapide', instantDebut: depart.t, instantFin: arrivee.t,
          idImageCle: arrivee.id, retardMs: necessaire * 1000 - disponibleMs,
          message: `${distance.toFixed(1)}° en ${disponibleMs} ms : il en faut ${Math.round(necessaire * 1000)} au mieux `
            + `(${piste.defaultVelocity.toFixed(0)} °/s, ${piste.defaultAcceleration.toFixed(0)} °/s²)`,
        })
      }
    }
  }
  return avertissements
}

/**
 * L'endroit où la courbe sort le plus des butées, ou null si elle y tient.
 *
 * Catmull-Rom continue sur sa lancée entre deux images-clés de même valeur précédées d'une montée :
 * deux points à 20° font passer la courbe à 23,6°. Aucune image-clé n'est alors fautive, et c'est
 * justement ce qu'on ne voit pas en regardant les points posés.
 */
export function pireDepassement(piste: EditorTrack): Depassement | null {
  const triees = [...piste.kfs].sort((a, b) => a.t - b.t)
  if (triees.length < 2) return null
  let pire: Depassement | null = null
  for (let t = triees[0].t; t <= triees[triees.length - 1].t; t += PAS_ECHANTILLONNAGE_MS) {
    const valeur = catmullRomAt(triees, t)
    const ecart = Math.max(piste.min - valeur, valeur - piste.max)
    if (ecart > (pire?.ecart ?? 0)) pire = { instant: t, valeur, ecart }
  }
  return pire
}

/**
 * Temps minimal pour parcourir une distance, rampes comprises, en secondes — profil trapézoïdal
 * quand la distance laisse atteindre la vitesse de croisière, triangulaire sinon.
 */
export function dureeMinimale(distance: number, vitesse: number, acceleration: number): number {
  if (vitesse <= 0 || acceleration <= 0) return Number.POSITIVE_INFINITY
  const distanceDesRampes = vitesse * vitesse / acceleration
  if (distance >= distanceDesRampes) return vitesse / acceleration + distance / vitesse
  return 2 * Math.sqrt(distance / acceleration)
}
