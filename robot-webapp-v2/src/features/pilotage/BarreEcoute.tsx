import { useCallback, useEffect, useState } from 'react'
import { useTopic } from '../../shared/websocket/useTopic'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useAudioStore } from '../../shared/audio/audioStore'
import { useConversationStore } from '../../shared/conversation/conversationStore'
import type { ReconnaissanceVocaleEvent } from '../../shared/types/events'
import styles from './pilotage.module.css'

/** Durée d'affichage d'une phrase reconnue avant de revenir à l'attente. */
const REMANENCE_MS = 6000

/**
 * Ce que le robot entend et ce qu'il répond, en bas de l'image.
 *
 * Le texte reconnu n'arrive qu'une fois la phrase finie : Vosk décode bien au fil
 * de la parole côté robot, mais seul le résultat final est publié
 * (`/events/reconnaissance-vocale`). Afficher le partiel demanderait un évènement
 * de plus côté backend — c'est le seul morceau de la maquette qui ne peut pas
 * être tenu tel quel aujourd'hui.
 */
export function BarreEcoute() {
  const connecte = useWebSocketStore((s) => s.connected)
  const parle = useAudioStore((s) => s.parle)
  const messages = useConversationStore((s) => s.messages)
  const [entendu, setEntendu] = useState<string | null>(null)

  useTopic(
    '/events/reconnaissance-vocale',
    useCallback((msg) => {
      let evenement: ReconnaissanceVocaleEvent
      try {
        evenement = JSON.parse(msg.body)
      } catch {
        return
      }
      if (evenement?.texteReconnu) setEntendu(evenement.texteReconnu)
    }, []),
  )

  useEffect(() => {
    if (entendu === null) return
    const minuterie = window.setTimeout(() => setEntendu(null), REMANENCE_MS)
    return () => clearTimeout(minuterie)
  }, [entendu])

  // Pendant que le robot parle, c'est sa phrase qui compte : le dernier message
  // du fil vient d'être prononcé.
  const dernierDuRobot = [...messages].reverse().find((m) => m.fromRobot)
  const enCours = parle && dernierDuRobot ? dernierDuRobot.texte : null

  // Liaison coupée : la barre disparaît. « En écoute… » alors que plus rien
  // n'arrive du robot serait un mensonge. La rémanence continue de courir pendant
  // ce temps : au retour, ou bien la phrase est encore fraîche, ou bien elle a
  // déjà été oubliée.
  if (!connecte) return null

  return (
    <div className={`${styles.ecoute} verre`}>
      <div className={`${styles.vu} ${parle ? styles.vuParle : styles.vuEcoute}`} aria-hidden="true">
        <i />
        <i />
        <i />
        <i />
        <i />
      </div>
      <div className={styles.ecouteTexte}>
        {enCours ?? entendu ?? <span className={styles.ecouteAttente}>en écoute…</span>}
      </div>
    </div>
  )
}
