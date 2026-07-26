import type { Animation, Track, TrackId } from '../../../shared/types/animation'
import type { EditorTrack } from '../store/animationStore'

/** Convertit l'état éditeur vers le modèle Animation du backend */
export function toAnimation(
  name: string,
  totalMs: number,
  tracks: EditorTrack[],
): Animation {
  const backendTracks: Track[] = tracks
    .filter(t => t.kfs.length > 0)
    .map(t => ({
      id: t.id as TrackId,
      defaultVelocity:     t.defaultVelocity,
      defaultAcceleration: t.defaultAcceleration,
      keyframes: [...t.kfs]
        .sort((a, b) => a.t - b.t)
        .map(kf => ({ time: kf.t, value: kf.v })),
    }))
  return { name, totalDuration: totalMs, tracks: backendTracks }
}

/** Formate un temps en ms → "s:cs" (ex: 1500 → "1:50") */
export function fmtMs(ms: number): string {
  const s = Math.floor(ms / 1000)
  const cs = Math.floor((ms % 1000) / 10)
  return `${s}:${String(cs).padStart(2, '0')}`
}

export function uid() { return Math.random().toString(36).slice(2, 9) }
