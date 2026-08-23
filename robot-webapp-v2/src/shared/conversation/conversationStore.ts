import { create } from 'zustand'

/** Profondeur du fil de discussion conservé, en nombre de messages. */
export const MAX_MESSAGES = 200

export interface ChatMessage {
  id: number
  texte: string
  fromRobot: boolean
  time: number
  /** Qui a parlé, si le robot le savait. Absent quand c'est lui qui parle. */
  idPersonne?: string
  prenom?: string
}

/** Message reçu, avant numérotation par le store. */
export interface MessageRecu {
  texte: string
  fromRobot: boolean
  idPersonne?: string
  prenom?: string
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
 * Le fil de discussion, partagé par toute l'application et alimenté en continu — d'où le store
 * plutôt que l'état d'une page : le robot peut tenir une conversation entière pendant qu'on
 * regarde ailleurs, et l'abonnement ne doit pas disparaître avec le composant.
 *
 * L'historique vit en mémoire du navigateur : le robot garde le sien de son côté, pour le
 * contexte du modèle, mais l'API ne l'expose pas. Un rechargement de page repart d'un fil vide.
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
      idPersonne: recu.idPersonne,
      prenom: recu.prenom,
    }))

    const messages = [...get().messages, ...ajouts]
    set({ messages: messages.length > MAX_MESSAGES ? messages.slice(-MAX_MESSAGES) : messages })
  },
}))
