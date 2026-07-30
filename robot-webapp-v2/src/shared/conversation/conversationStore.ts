import { create } from 'zustand'

/** Profondeur du fil de discussion conservé, en nombre de messages. */
export const MAX_MESSAGES = 200

export interface ChatMessage {
  id: number
  texte: string
  fromRobot: boolean
  time: number
}

/** Message reçu, avant numérotation par le store. */
export interface MessageRecu {
  texte: string
  fromRobot: boolean
  /** Instant d'émission par le robot, en millisecondes epoch (voir `horodatageRobotEnMs`). */
  time: number
}

interface ConversationState {
  /** Fil de discussion, du plus ancien au plus récent. */
  messages: ChatMessage[]
  /** Intègre un lot de messages reçus, dans l'ordre d'arrivée, en une seule mise à jour. */
  ingerer: (recus: MessageRecu[]) => void
}

/** Numérotation stable des messages, indépendante des montages de composants. */
let prochainId = 1

/**
 * Store de conversation partagé, alimenté en continu par `ConversationProvider` (monté une seule
 * fois au niveau du Layout, donc vivant pendant toute la session — comme la connexion WebSocket
 * elle-même).
 *
 * Auparavant le fil vivait dans l'état local de `DialoguePage`, avec deux conséquences : il était
 * perdu à chaque navigation, et surtout l'abonnement à `/events/conversation` disparaissait avec
 * le composant — le robot pouvait tenir une conversation entière pendant que l'utilisateur était
 * sur une autre page sans qu'aucun message ne soit capté.
 *
 * L'historique reste volontairement en mémoire du navigateur : le robot conserve de son côté une
 * mémoire persistante des échanges (MapDB, pour le contexte du modèle), mais elle n'est pas
 * exposée par l'API. Un rechargement de page repart donc d'un fil vide.
 */
export const useConversationStore = create<ConversationState>((set, get) => ({
  messages: [],

  ingerer(recus) {
    if (recus.length === 0) return

    // L'horodatage vient du robot (`RobotEvent.dateTime`), résolu par le provider : après un
    // à-coup réseau, les messages rattrapés affichent bien l'heure à laquelle ils ont été
    // prononcés, et non celle de leur arrivée en rafale.
    const ajouts = recus.map((recu) => ({
      id: prochainId++,
      texte: recu.texte,
      fromRobot: recu.fromRobot,
      time: recu.time,
    }))

    const messages = [...get().messages, ...ajouts]
    set({ messages: messages.length > MAX_MESSAGES ? messages.slice(-MAX_MESSAGES) : messages })
  },
}))
