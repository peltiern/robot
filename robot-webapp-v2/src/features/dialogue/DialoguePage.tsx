import { useCallback, useEffect, useRef, useState } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { useTopic } from '../../shared/websocket/useTopic'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import type { ConversationEvent } from '../../shared/types/events'
import styles from './DialoguePage.module.css'

interface ChatMessage {
  id: number
  texte: string
  fromRobot: boolean
  time: number
}

const MAX_MESSAGES = 200

export function DialoguePage() {
  const connected = useWebSocketStore((s) => s.connected)
  const speak = useWebSocketStore((s) => s.speak)

  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const nextId = useRef(1)
  const scrollRef = useRef<HTMLDivElement>(null)

  const onConversation = useCallback((msg: IMessage) => {
    let event: ConversationEvent
    try {
      event = JSON.parse(msg.body)
    } catch {
      return
    }
    if (!event?.texte) return
    setMessages((prev) => {
      const next = [
        ...prev,
        {
          id: nextId.current++,
          texte: event.texte,
          fromRobot: event.idLocuteur === -1,
          time: Date.now(),
        },
      ]
      return next.length > MAX_MESSAGES ? next.slice(-MAX_MESSAGES) : next
    })
  }, [])

  useTopic('/events/conversation', onConversation)

  // Auto-scroll vers le bas à chaque nouveau message.
  useEffect(() => {
    const el = scrollRef.current
    if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  }, [messages])

  const send = () => {
    const texte = draft.trim()
    if (!texte || !connected) return
    speak(texte)
    setDraft('')
  }

  const onKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.thread} ref={scrollRef}>
        {messages.length === 0 ? (
          <div className={styles.empty}>
            <span className={styles.emptyIcon}>💬</span>
            <p className={styles.emptyTitle}>Aucun message</p>
            <p className={styles.emptyHint}>
              Tapez un texte : le robot le dira à voix haute.
            </p>
          </div>
        ) : (
          messages.map((m) => (
            <div
              key={m.id}
              className={`${styles.row} ${m.fromRobot ? styles.rowRobot : styles.rowUser}`}
            >
              <span className={styles.avatar}>{m.fromRobot ? '🤖' : '🧑'}</span>
              <div className={styles.bubble}>
                <span className={styles.text}>{m.texte}</span>
                <span className={styles.time}>{formatTime(m.time)}</span>
              </div>
            </div>
          ))
        )}
      </div>

      <div className={styles.composer}>
        <textarea
          className={styles.input}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={connected ? 'Faire parler le robot…' : 'Robot déconnecté'}
          rows={1}
          disabled={!connected}
        />
        <button
          className={styles.send}
          onClick={send}
          disabled={!connected || draft.trim() === ''}
          title="Envoyer (Entrée)"
        >
          Dire
        </button>
      </div>
    </div>
  )
}

function formatTime(ms: number): string {
  return new Date(ms).toLocaleTimeString('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
  })
}
