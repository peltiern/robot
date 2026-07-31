import { useEffect, useRef, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useConversationStore } from '../../shared/conversation/conversationStore'
import styles from './hud.module.css'

/**
 * Fil de discussion et bouton « faire parler le robot ».
 *
 * Le fil vient du store partagé (alimenté par `ConversationProvider`, monté au
 * niveau du Layout) : il survit à un passage par l'atelier, et les échanges qui
 * ont lieu pendant ce temps sont bien captés.
 */
export function PanneauDialogue() {
  const connecte = useWebSocketStore((s) => s.connected)
  const parler = useWebSocketStore((s) => s.speak)
  const messages = useConversationStore((s) => s.messages)

  const [brouillon, setBrouillon] = useState('')
  const filRef = useRef<HTMLDivElement>(null)

  // Défilement automatique vers le bas à chaque nouveau message.
  useEffect(() => {
    const fil = filRef.current
    if (fil) fil.scrollTo({ top: fil.scrollHeight, behavior: 'smooth' })
  }, [messages])

  const envoyer = () => {
    const texte = brouillon.trim()
    if (!texte || !connecte) return
    parler(texte)
    setBrouillon('')
  }

  return (
    <section className={`${styles.bloc} ${styles.blocDialogue}`}>
      <header className={styles.blocEntete}>
        <Icone nom="bulle" taille={18} />
        <span className="eyebrow">Dialogue</span>
      </header>

      <div className={styles.fil} ref={filRef}>
        {messages.length === 0 ? (
          <p className={styles.filVide}>
            Rien encore. Parle au robot, ou écris-lui ce qu'il doit dire.
          </p>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={`${styles.msg} ${message.fromRobot ? styles.msgRobot : styles.msgMoi}`}
            >
              <div className={styles.bulle}>
                {message.texte}
                <span className={`${styles.heure} data`}>{heure(message.time)}</span>
              </div>
            </div>
          ))
        )}
      </div>

      <div className={styles.composeur}>
        <input
          className={styles.saisie}
          value={brouillon}
          onChange={(e) => setBrouillon(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') envoyer()
          }}
          placeholder={connecte ? 'Faire parler le robot…' : 'Robot déconnecté'}
          disabled={!connecte}
        />
        <button
          className={styles.dire}
          onClick={envoyer}
          disabled={!connecte || brouillon.trim() === ''}
        >
          DIRE
        </button>
      </div>
    </section>
  )
}

function heure(ms: number): string {
  return new Date(ms).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}
