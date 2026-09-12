import type { Axe } from '../../../shared/types/animation'
import type { EditorTrack } from '../store/animationStore'
import { clamp, trackVal } from '../utils/catmullRom'

/** Période d'échantillonnage du lecteur du robot (LecteurAnimation, 10 Hz). */
export const PERIODE_LECTEUR_MS = 100

/**
 * Ce que les servos du robot feraient de la timeline, pour l'aperçu.
 *
 * Trois choses que le robot fait et qu'un aperçu naïf oublierait, chacune de quoi faire dessiner
 * un geste qu'il ne rendra pas pareil :
 * - il ne lit la courbe que dix fois par seconde — la suivre à 60 Hz montrerait un mouvement
 *   plus fin que le sien ;
 * - il écrête aux butées, et la spline dépasse ses images-clés ;
 * - chaque servo tient sa vitesse de travail : un saut brusque sur la courbe devient une rampe,
 *   et c'est là que le robot prend du retard.
 *
 * L'accélération n'est pas simulée : la vitesse suffit à montrer où il sera en retard.
 */
export class SimulateurServos {
  private positions = new Map<Axe, number>()

  /**
   * Fait avancer les servos de `dureeMs` vers la courbe lue à `instant`.
   *
   * Au premier appel, chaque axe démarre sur sa courbe : on ne sait pas où est la vraie tête, et
   * inventer une posture de départ ferait voir un mouvement que le robot ne ferait peut-être pas.
   * Une piste désactivée n'est pas commandée par le robot : son axe reste où il est.
   */
  avancer(pistes: EditorTrack[], instant: number, dureeMs: number): Partial<Record<Axe, number>> {
    const echantillon = Math.floor(instant / PERIODE_LECTEUR_MS) * PERIODE_LECTEUR_MS
    for (const piste of pistes) {
      const actuelle = this.positions.get(piste.id)
      if (!piste.enabled && actuelle !== undefined) continue
      const visee = trackVal(piste.kfs, echantillon, piste.min, piste.max)
      if (actuelle === undefined) {
        this.positions.set(piste.id, visee)
        continue
      }
      const pas = piste.defaultVelocity * dureeMs / 1000
      this.positions.set(piste.id, actuelle + clamp(visee - actuelle, -pas, pas))
    }
    return Object.fromEntries(this.positions)
  }
}
