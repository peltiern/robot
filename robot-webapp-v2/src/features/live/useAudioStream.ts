import { useCallback, useEffect, useRef, useState } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { useTopic } from '../../shared/websocket/useTopic'

/** Décode une chaîne base64 en ArrayBuffer (sans dépendance externe). */
function base64ToArrayBuffer(b64: string): ArrayBuffer {
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes.buffer
}

/**
 * Lit le flux audio du robot (topic `/audio`, trames `audioContentBase64`) via la Web Audio API.
 * Muet par défaut : les navigateurs interdisent de démarrer un AudioContext sans geste utilisateur,
 * donc le son ne s'active qu'au clic sur le bouton (qui réveille le contexte). Les trames sont
 * enchaînées séquentiellement (chacune démarre à la fin de la précédente) pour éviter les
 * chevauchements et les trous.
 */
export function useAudioStream() {
  const [muted, setMuted] = useState(true)
  const ctxRef = useRef<AudioContext | null>(null)
  const nextStartRef = useRef(0)
  const mutedRef = useRef(true)
  mutedRef.current = muted

  const onAudio = useCallback((msg: IMessage) => {
    if (mutedRef.current) return
    const ctx = ctxRef.current
    if (!ctx) return
    let payload: { audioContentBase64?: string }
    try {
      payload = JSON.parse(msg.body)
    } catch {
      return
    }
    if (!payload.audioContentBase64) return

    ctx
      .decodeAudioData(base64ToArrayBuffer(payload.audioContentBase64))
      .then((buffer) => {
        const source = ctx.createBufferSource()
        source.buffer = buffer
        source.connect(ctx.destination)
        const start = Math.max(ctx.currentTime, nextStartRef.current)
        source.start(start)
        nextStartRef.current = start + buffer.duration
      })
      .catch(() => {
        /* trame non décodable : ignorée */
      })
  }, [])

  useTopic('/audio', onAudio)

  const toggleMute = useCallback(() => {
    setMuted((m) => {
      const next = !m
      if (!next) {
        // Activation du son : (ré)veille l'AudioContext — ce clic est le geste utilisateur requis.
        if (!ctxRef.current) ctxRef.current = new AudioContext()
        void ctxRef.current.resume()
        nextStartRef.current = ctxRef.current.currentTime
      } else {
        void ctxRef.current?.suspend()
      }
      return next
    })
  }, [])

  // Fermeture du contexte au démontage.
  useEffect(() => {
    return () => {
      void ctxRef.current?.close()
      ctxRef.current = null
    }
  }, [])

  return { muted, toggleMute }
}
