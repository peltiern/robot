// Événements WebSocket — miroir des RobotEvent Java (fr.roboteek.robot.systemenerveux.event).
// Les noms de champs reflètent la sérialisation Gson côté backend.

/** Rectangle détecté (visage ou objet) : coordonnées en pixels de l'image source. */
export interface DetectedBox {
  x: number
  y: number
  width: number
  height: number
  /** Nom du visage reconnu / label de l'objet (peut être absent). */
  name?: string
}

/** VideoEvent (topic `/video`) — image JPEG base64 + détections éventuelles. */
export interface VideoEvent {
  imageBase64: string
  faceFound?: boolean
  faces?: DetectedBox[]
  objectFound?: boolean
  objects?: DetectedBox[]
}

/**
 * ConversationEvent (topic `/events/conversation`) — un tour de dialogue.
 * idLocuteur : -1 = robot, 0 = inconnu, > 0 = identifiant d'une personne.
 */
export interface ConversationEvent {
  eventType: 'conversation'
  texte: string
  idLocuteur: number
  /**
   * Instant d'émission par le robot (`RobotEvent.dateTime`), en ISO-8601 local sans fuseau et
   * avec une précision nanoseconde. À convertir via `horodatageRobotEnMs`.
   */
  dateTime?: string
}

/**
 * ReconnaissanceVocaleEvent (topic `/events/reconnaissance-vocale`) — phrase
 * comprise par le robot. Publié une fois la phrase terminée : le décodage au fil
 * de la parole reste interne au capteur vocal, les partiels ne sortent pas.
 */
export interface ReconnaissanceVocaleEvent {
  eventType: 'reconnaissance-vocale'
  texteReconnu: string
  nomRegle?: string
}

/** ParoleEvent envoyé au robot (destination `/app/robotevents`) pour le faire parler. */
export interface ParoleEventOut {
  eventType: 'parole'
  texte: string
}

/**
 * ArretUrgenceEvent (topic `/events/arret-urgence`) — état de l'arrêt d'urgence des moteurs.
 * Le même évènement sert à le demander (destination `/app/robotevents`) : `actif` à vrai pour
 * déclencher, à faux pour réarmer.
 */
export interface ArretUrgenceEvent {
  eventType: 'arret-urgence'
  actif: boolean
  /** Qui a déclenché : « manette », « interface », « watchdog — … »… */
  origine?: string
}

/**
 * SanteOrganesEvent (topic `/events/sante-organes`) — état vital de tous les organes surveillés,
 * diffusé une fois par seconde par le watchdog. C'est le relevé sur lequel il décide de
 * couper les moteurs : les pastilles montrent exactement ce qu'il voit.
 *
 * La forme de `organes` est décrite par `SanteOrgane` (`shared/sante/santeStore`).
 */
export interface SanteOrganesEvent {
  eventType: 'sante-organes'
  organes: {
    id: string
    libelle: string
    nature: 'ACTIONNEUR' | 'CAPTEUR'
    etat: 'VIVANT' | 'MUET' | 'ETEINT'
    ageMillis: number | null
    surveille: boolean
  }[]
}
