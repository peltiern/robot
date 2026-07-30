import { useCallback, useEffect, useRef } from 'react'
import { useTopic } from '../websocket/useTopic'
import { NB_ECHANTILLONS_HISTORIQUE, useTelemetryStore } from './telemetryStore'

/**
 * Composant sans rendu : alimente le store de télémétrie en continu depuis
 * `/events/telemetrie-organe`, indépendamment des pages affichées. À monter une seule fois, au
 * niveau du Layout (durée de vie de l'application) — voir `telemetryStore` pour le pourquoi.
 *
 * Les messages sont regroupés avant d'être ingérés : après un à-coup réseau, le retard accumulé
 * est livré d'un coup, et un rendu par message faisait défiler jauges et courbes en accéléré.
 * Tous les échantillons sont conservés, mais publiés en une seule mise à jour.
 */
export function TelemetryProvider() {
  const ingerer = useTelemetryStore((s) => s.ingerer)

  const enAttente = useRef<Record<string, number>[]>([])
  const rafEnCours = useRef<number | null>(null)

  const vider = useCallback(() => {
    rafEnCours.current = null
    const lot = enAttente.current
    enAttente.current = []
    if (lot.length > 0) ingerer(lot)
  }, [ingerer])

  useTopic(
    '/events/telemetrie-organe',
    useCallback(
      (msg) => {
        let evenement: { valeurs?: Record<string, number> }
        try {
          evenement = JSON.parse(msg.body)
        } catch {
          return
        }
        if (!evenement.valeurs) return

        enAttente.current.push(evenement.valeurs)
        // L'onglet en arrière-plan suspend les rAF : sans cette borne, la file grossirait
        // indéfiniment. Au-delà de la profondeur de l'historique, les plus anciens seraient
        // de toute façon écartés à l'ingestion.
        if (enAttente.current.length > NB_ECHANTILLONS_HISTORIQUE) {
          enAttente.current = enAttente.current.slice(-NB_ECHANTILLONS_HISTORIQUE)
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
