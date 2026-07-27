import { useCallback } from 'react'
import { useTopic } from '../websocket/useTopic'
import { useTelemetryStore } from './telemetryStore'

/**
 * Composant sans rendu : alimente le store de télémétrie en continu depuis
 * `/events/telemetrie-organe`, indépendamment des pages affichées. À monter une seule fois, au
 * niveau du Layout (durée de vie de l'application) — voir `telemetryStore` pour le pourquoi.
 */
export function TelemetryProvider() {
  const ingerer = useTelemetryStore((s) => s.ingerer)

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
        if (evenement.valeurs) ingerer(evenement.valeurs)
      },
      [ingerer],
    ),
  )

  return null
}
