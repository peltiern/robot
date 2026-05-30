// Événements WebSocket (miroir des RobotEvent Java)

export interface RobotEvent {
  type: string
}

export interface VideoEvent extends RobotEvent {
  type: 'video'
  imageBase64: string
  recognizedFaces?: RecognizedFace[]
  detectedObjects?: DetectedObject[]
}

export interface ParoleEvent extends RobotEvent {
  type: 'parole'
  texte: string
}

export interface RecognizedFace {
  name: string
  x: number
  y: number
  width: number
  height: number
}

export interface DetectedObject {
  label: string
  confidence: number
  x: number
  y: number
  width: number
  height: number
}
