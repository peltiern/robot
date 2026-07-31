import { useCallback } from 'react'
import { useTopic } from '../websocket/useTopic'
import type { ArretUrgenceEvent } from '../types/events'
import { useArretUrgenceStore } from './arretUrgenceStore'

/**
 * Composant sans rendu : suit l'état de l'arrêt d'urgence sur `/events/arret-urgence`.
 *
 * Monté au niveau du Layout : l'état vaut pour toute l'application, atelier compris — un robot
 * arrêté d'urgence ne joue pas non plus les animations qu'on lui envoie depuis l'éditeur.
 */
export function ArretUrgenceProvider() {
  const appliquer = useArretUrgenceStore((s) => s.appliquer)

  useTopic(
    '/events/arret-urgence',
    useCallback(
      (msg) => {
        let evenement: ArretUrgenceEvent
        try {
          evenement = JSON.parse(msg.body)
        } catch {
          return
        }
        if (typeof evenement?.actif !== 'boolean') return
        appliquer(evenement.actif, evenement.origine ?? null)
      },
      [appliquer],
    ),
  )

  return null
}
