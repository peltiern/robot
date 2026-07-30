import { useEffect, useRef, useState } from 'react'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useConversationStore } from '../../shared/conversation/conversationStore'
import styles from './DialoguePage.module.css'

export function DialoguePage() {
  const connected = useWebSocketStore((s) => s.connected)
  const speak = useWebSocketStore((s) => s.speak)

  // Le fil vient du store partagé (alimenté par `ConversationProvider`, monté au niveau du
  // Layout) et non d'un état local : sinon changer de page perdrait l'historique accumulé, et
  // surtout les messages échangés hors de cette page ne seraient jamais captés.
  const messages = useConversationStore((s) => s.messages)
  const [draft, setDraft] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)

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
