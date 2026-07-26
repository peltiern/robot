import type { TrackId } from '../types/animation'

const BASE = `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api/tracks`

export interface TrackDefinition {
  id: TrackId
  name: string
  minPosition: number
  maxPosition: number
  defaultVelocity: number
  defaultAcceleration: number
}

export const trackApi = {
  getAll: (): Promise<TrackDefinition[]> =>
    fetch(BASE).then(r => { if (!r.ok) throw new Error(r.statusText); return r.json() }),
}
