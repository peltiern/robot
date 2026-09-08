import type { Organe } from '../stores/organesStore'

/**
 * Correspondance articulation → évènement WebSocket à publier.
 *
 * Seul « en dur » restant côté front : tout le reste (libellés, butées, unités,
 * positions) vient de `GET /api/organes`. Ce serait le dernier à disparaître en
 * passant la commande en REST (PUT sur la position de l'articulation).
 */
export const COMMANDE: Record<string, { event: 'mouvement-yeux' | 'mouvement-cou'; field: string }> = {
  oeilGauche: { event: 'mouvement-yeux', field: 'positionOeilGauche' },
  oeilDroit: { event: 'mouvement-yeux', field: 'positionOeilDroit' },
  pan: { event: 'mouvement-cou', field: 'positionPanoramique' },
  tilt: { event: 'mouvement-cou', field: 'positionInclinaison' },
  upDown: { event: 'mouvement-cou', field: 'positionMonterDescendre' },
}

/** Vrai si l'articulation sait être pilotée depuis le front. */
export const estPilotable = (id: string) => id in COMMANDE

/**
 * Évènements de recentrage : un par organe, regroupant toutes ses articulations
 * à 0°. Regrouper évite de faire partir trois messages successifs qui feraient
 * bouger le cou en trois temps.
 */
export function evenementsRecentrage(organes: Organe[]): Record<string, unknown>[] {
  return organes
    .filter((organe) => organe.articulations.some((a) => estPilotable(a.id)))
    .map((organe) => {
      const pilotables = organe.articulations.filter((a) => estPilotable(a.id))
      const evenement: Record<string, unknown> = { eventType: COMMANDE[pilotables[0].id].event }
      for (const articulation of pilotables)
        evenement[COMMANDE[articulation.id].field] = articulation.positionInitiale ?? 0
      return evenement
    })
}
