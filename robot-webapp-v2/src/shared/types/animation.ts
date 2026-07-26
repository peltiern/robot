// Miroir exact du modèle Java Animation / Track / Keyframe

export type TrackId =
  | 'OEIL_GAUCHE'
  | 'OEIL_DROIT'
  | 'COU_GAUCHE_DROITE'
  | 'COU_HAUT_BAS'
  | 'COU_MONTER_DESCENDRE'

export interface Keyframe {
  time: number         // ms absolu depuis le début de l'animation
  value: number        // degrés
  velocity?: number    // null = utilise Track.defaultVelocity
  acceleration?: number
}

export interface Track {
  id: TrackId
  defaultVelocity: number
  defaultAcceleration: number
  keyframes: Keyframe[]
}

export interface Animation {
  name: string
  totalDuration: number  // ms
  tracks: Track[]
}

export interface ValidationWarning {
  trackId: TrackId
  timeFrom: number
  timeTo: number
  message: string
}

export interface SaveResponse {
  animation: Animation
  warnings: ValidationWarning[]
}

// Plages physiques par track (en degrés relatifs)
export const TRACK_RANGES: Record<TrackId, { min: number; max: number; label: string; color: string }> = {
  OEIL_GAUCHE:         { min: -24, max: 20,  label: 'Œil Gauche',         color: '#4fc3f7' },
  OEIL_DROIT:          { min: -24, max: 20,  label: 'Œil Droit',          color: '#81c784' },
  COU_GAUCHE_DROITE:   { min: -65, max: 65,  label: 'Cou Gauche/Droite',  color: '#ffb74d' },
  COU_HAUT_BAS:        { min: -25, max: 50,  label: 'Cou Haut/Bas',       color: '#f06292' },
  COU_MONTER_DESCENDRE:{ min: -30, max: 30,  label: 'Cou Monter/Descendre', color: '#ce93d8' },
}

export const DEFAULT_VELOCITY = 250
export const DEFAULT_ACCELERATION = 200
