import { useCallback, useEffect } from 'react'
import { useTopic } from '../websocket/useTopic'
import { useAudioStore } from './audioStore'

/**
 * Composant sans rendu : alimente le store audio depuis `/audio`. À monter une
 * seule fois, au niveau du Layout — comme `TelemetryProvider` et
 * `ConversationProvider`.
 */
export function AudioProvider() {
  const traiterTrame = useAudioStore((s) => s.traiterTrame)
  const fermer = useAudioStore((s) => s.fermer)

  useTopic(
    '/audio',
    useCallback(
      (msg) => {
        let trame: { audioContentBase64?: string }
        try {
          trame = JSON.parse(msg.body)
        } catch {
          return
        }
        if (trame.audioContentBase64) traiterTrame(trame.audioContentBase64)
      },
      [traiterTrame],
    ),
  )

  useEffect(() => fermer, [fermer])

  return null
}
