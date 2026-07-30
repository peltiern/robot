import { useCallback, useEffect, useRef } from 'react'
import { useTopic } from '../websocket/useTopic'
import { horodatageRobotEnMs } from '../websocket/horodatageRobot'
import type { ConversationEvent } from '../types/events'
import { MAX_MESSAGES, useConversationStore, type MessageRecu } from './conversationStore'

/**
 * Composant sans rendu : alimente le fil de discussion depuis `/events/conversation`,
 * indépendamment de la page affichée. À monter une seule fois, au niveau du Layout (durée de vie
 * de l'application) — voir `conversationStore` pour le pourquoi.
 *
 * Les messages sont regroupés avant d'être ingérés : après un à-coup réseau, le retard accumulé
 * est livré d'un coup, et un rendu par message enchaînait autant de défilements animés dans le
 * fil. Aucun message n'est perdu, ils sont simplement ajoutés en une seule fois.
 */
export function ConversationProvider() {
  const ingerer = useConversationStore((s) => s.ingerer)

  const enAttente = useRef<MessageRecu[]>([])
  const rafEnCours = useRef<number | null>(null)

  const vider = useCallback(() => {
    rafEnCours.current = null
    const lot = enAttente.current
    enAttente.current = []
    if (lot.length > 0) ingerer(lot)
  }, [ingerer])

  useTopic(
    '/events/conversation',
    useCallback(
      (msg) => {
        let evenement: ConversationEvent
        try {
          evenement = JSON.parse(msg.body)
        } catch {
          return
        }
        if (!evenement?.texte) return

        enAttente.current.push({
          texte: evenement.texte,
          fromRobot: evenement.idLocuteur === -1,
          time: horodatageRobotEnMs(evenement.dateTime, Date.now()),
        })
        // L'onglet en arrière-plan suspend les rAF : au-delà de la profondeur du fil, les plus
        // anciens seraient de toute façon écartés à l'ingestion.
        if (enAttente.current.length > MAX_MESSAGES) {
          enAttente.current = enAttente.current.slice(-MAX_MESSAGES)
        }
        if (rafEnCours.current === null) {
          rafEnCours.current = requestAnimationFrame(vider)
        }
      },
      [vider],
    ),
  )

  useEffect(() => {
    return () => {
      if (rafEnCours.current !== null) cancelAnimationFrame(rafEnCours.current)
    }
  }, [])

  return null
}
