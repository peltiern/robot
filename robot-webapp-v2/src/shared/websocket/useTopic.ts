import { useEffect, useRef } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { useWebSocketStore } from './websocketStore'

/**
 * S'abonne à un topic STOMP et appelle `handler` à chaque message.
 * Se (ré)abonne automatiquement quand la connexion s'établit ou change,
 * et se désabonne au démontage. Le handler peut changer à chaque rendu
 * sans provoquer de ré-abonnement (capturé par référence).
 */
export function useTopic(topic: string, handler: (msg: IMessage) => void) {
  const connected = useWebSocketStore((s) => s.connected)
  const client = useWebSocketStore((s) => s.client)

  const handlerRef = useRef(handler)
  handlerRef.current = handler

  useEffect(() => {
    if (!connected || !client?.active) return
    const sub = client.subscribe(topic, (msg) => handlerRef.current(msg))
    return () => sub.unsubscribe()
  }, [connected, client, topic])
}
