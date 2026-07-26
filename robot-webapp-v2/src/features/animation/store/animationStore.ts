import { create } from 'zustand'
import type { TrackId } from '../../../shared/types/animation'
import { trackApi, type TrackDefinition } from '../../../shared/api/trackApi'
import { assignTrackColors } from '../../../shared/utils/trackColors'
import { uid } from '../utils/convert'

export interface EditorKeyframe { id: string; t: number; v: number }

export interface EditorTrack {
  id: TrackId
  name: string
  min: number
  max: number
  color: string
  enabled: boolean
  defaultVelocity: number
  defaultAcceleration: number
  kfs: EditorKeyframe[]
}

const SNAP = 50 // ms

// Noms de fallback — utilisés si le backend ne répond pas
const FALLBACK_NAMES: Record<string, string> = {
  OEIL_GAUCHE:          'Œil Gauche',
  OEIL_DROIT:           'Œil Droit',
  COU_GAUCHE_DROITE:    'Cou Gauche / Droite',
  COU_HAUT_BAS:         'Cou Haut / Bas',
  COU_MONTER_DESCENDRE: 'Cou Monter / Descendre',
}

// Fallback utilisé avant que le backend réponde
const FALLBACK_TRACKS: TrackDefinition[] = [
  { id: 'OEIL_GAUCHE',          name: FALLBACK_NAMES['OEIL_GAUCHE'],          minPosition: -24, maxPosition: 20,  defaultVelocity: 40,  defaultAcceleration: 60  },
  { id: 'OEIL_DROIT',           name: FALLBACK_NAMES['OEIL_DROIT'],           minPosition: -24, maxPosition: 20,  defaultVelocity: 40,  defaultAcceleration: 60  },
  { id: 'COU_GAUCHE_DROITE',    name: FALLBACK_NAMES['COU_GAUCHE_DROITE'],    minPosition: -65, maxPosition: 65,  defaultVelocity: 100, defaultAcceleration: 200 },
  { id: 'COU_HAUT_BAS',         name: FALLBACK_NAMES['COU_HAUT_BAS'],         minPosition: -25, maxPosition: 50,  defaultVelocity: 100, defaultAcceleration: 200 },
  { id: 'COU_MONTER_DESCENDRE', name: FALLBACK_NAMES['COU_MONTER_DESCENDRE'], minPosition: -30, maxPosition: 30,  defaultVelocity: 100, defaultAcceleration: 200 },
]

function buildEditorTracks(defs: TrackDefinition[], totalMs: number): EditorTrack[] {
  const colors = assignTrackColors(defs.map(d => d.id))
  return defs.map(def => ({
    id: def.id,
    name:    def.name ?? FALLBACK_NAMES[def.id] ?? def.id,
    color:   colors[def.id],
    enabled: true,
    min: def.minPosition,
    max: def.maxPosition,
    defaultVelocity:     def.defaultVelocity,
    defaultAcceleration: def.defaultAcceleration,
    kfs: [
      { id: uid(), t: 0,       v: 0 },
      { id: uid(), t: totalMs, v: 0 },
    ],
  }))
}

interface HistorySnapshot {
  tracks: EditorTrack[]
  animationName: string
  totalMs: number
}

const MAX_HISTORY = 50

export interface AnimationEditorState {
  // Données
  animationName: string
  totalMs: number
  tracks: EditorTrack[]

  // Historique
  past: HistorySnapshot[]
  future: HistorySnapshot[]

  // Lecture
  playhead: number
  playing: boolean
  looping: boolean

  // Éditeur
  pxPerMs: number
  scrollX: number
  selectedKf: { trackId: TrackId; kfId: string } | null

  // Vitesse/accel globales
  globalVelocity: Record<string, number>
  globalAcceleration: Record<string, number>

  // Actions — historique
  snapshot: () => void
  undo:     () => void
  redo:     () => void

  // Actions — lecture
  setPlayhead:    (t: number) => void
  setPlaying:     (b: boolean) => void
  toggleLoop:     () => void
  setTotalMs:     (ms: number) => void

  // Actions — vue
  setPxPerMs:  (v: number) => void
  setScrollX:  (v: number) => void
  zoom:        (factor: number, atPx: number, canvasW: number) => void

  // Actions — keyframes
  selectKf:    (trackId: TrackId, kfId: string) => void
  clearSel:    () => void
  addKf:       (trackId: TrackId, t: number, v: number) => EditorKeyframe
  moveKf:      (trackId: TrackId, kfId: string, t: number, v: number) => void
  deleteKf:    (trackId: TrackId, kfId: string) => void
  updateSelKf: (t: number, v: number) => void

  // Actions — animation
  setAnimationName:       (n: string) => void
  setGlobalParam:         (group: string, key: 'velocity' | 'acceleration', val: number) => void
  toggleTrack:            (trackId: TrackId) => void
  loadPreset:             (steps: Array<{ t: number; vals: Partial<Record<TrackId, number>> }>, totalMs: number) => void
  reset:                  () => void
  loadTracksFromBackend:  () => Promise<void>
}

export const useAnimationStore = create<AnimationEditorState>((set, get) => {

  // Fonction locale — accès direct à set/get, pas via get().snapshot()
  function pushSnapshot() {
    const { past, tracks, animationName, totalMs } = get()
    set({ past: [...past.slice(-(MAX_HISTORY - 1)), { tracks, animationName, totalMs }], future: [] })
  }

  return {
    animationName: 'NouvellAnimation',
    totalMs: 3000,
    tracks: buildEditorTracks(FALLBACK_TRACKS, 3000),
    past: [],
    future: [],
    playhead: 0,
    playing: false,
    looping: true,
    pxPerMs: 0.17,
    scrollX: 0,
    selectedKf: null,
    globalVelocity:     { eyes: 250, neck: 250 },
    globalAcceleration: { eyes: 200, neck: 200 },

    snapshot: pushSnapshot,

    undo() {
      const { past, future, tracks, animationName, totalMs } = get()
      if (past.length === 0) return
      const prev = past[past.length - 1]
      set({
        past: past.slice(0, -1),
        future: [{ tracks, animationName, totalMs }, ...future.slice(0, MAX_HISTORY - 1)],
        tracks: prev.tracks,
        animationName: prev.animationName,
        totalMs: prev.totalMs,
        selectedKf: null,
      })
    },

    redo() {
      const { past, future, tracks, animationName, totalMs } = get()
      if (future.length === 0) return
      const next = future[0]
      set({
        past: [...past.slice(-(MAX_HISTORY - 1)), { tracks, animationName, totalMs }],
        future: future.slice(1),
        tracks: next.tracks,
        animationName: next.animationName,
        totalMs: next.totalMs,
        selectedKf: null,
      })
    },

    setPlayhead:  t  => set({ playhead: Math.max(0, Math.min(t, get().totalMs)) }),
    setPlaying:   b  => set({ playing: b }),
    toggleLoop:   ()  => set(s => ({ looping: !s.looping })),
    setTotalMs(ms) {
      pushSnapshot()
      set({ totalMs: ms })
    },

    setPxPerMs: v  => set({ pxPerMs: Math.max(0.015, Math.min(3, v)) }),
    setScrollX: v  => set({ scrollX: Math.max(0, v) }),

    zoom(factor, atPx, _canvasW) {
      const { pxPerMs, scrollX } = get()
      const atT = (atPx + scrollX) / pxPerMs
      const next = Math.max(0.015, Math.min(3, pxPerMs * factor))
      set({ pxPerMs: next, scrollX: Math.max(0, atT * next - atPx) })
    },

    selectKf: (trackId, kfId) => set({ selectedKf: { trackId, kfId } }),
    clearSel:  ()              => set({ selectedKf: null }),

    addKf(trackId, tRaw, v) {
      pushSnapshot()
      const t = Math.round(tRaw / SNAP) * SNAP
      const kf: EditorKeyframe = { id: uid(), t, v }
      set(s => ({
        tracks: s.tracks.map(tr =>
          tr.id === trackId ? { ...tr, kfs: [...tr.kfs, kf] } : tr
        ),
      }))
      return kf
    },

    moveKf(trackId, kfId, tRaw, v) {
      const t = Math.max(0, Math.round(tRaw / SNAP) * SNAP)
      set(s => ({
        tracks: s.tracks.map(tr =>
          tr.id !== trackId ? tr : {
            ...tr,
            kfs: tr.kfs.map(k => k.id === kfId ? { ...k, t: Math.min(t, s.totalMs), v } : k),
          }
        ),
      }))
    },

    deleteKf(trackId, kfId) {
      pushSnapshot()
      set(s => ({
        selectedKf: s.selectedKf?.kfId === kfId ? null : s.selectedKf,
        tracks: s.tracks.map(tr =>
          tr.id !== trackId ? tr : { ...tr, kfs: tr.kfs.filter(k => k.id !== kfId) }
        ),
      }))
    },

    updateSelKf(t, v) {
      const sel = get().selectedKf
      if (!sel) return
      get().moveKf(sel.trackId, sel.kfId, t, v)
    },

    setAnimationName: n => set({ animationName: n }),

    toggleTrack(trackId) {
      pushSnapshot()
      set(s => ({
        tracks: s.tracks.map(t =>
          t.id === trackId ? { ...t, enabled: !t.enabled } : t
        ),
      }))
    },

    setGlobalParam(group, key, val) {
      if (key === 'velocity')
        set(s => ({ globalVelocity: { ...s.globalVelocity, [group]: val } }))
      else
        set(s => ({ globalAcceleration: { ...s.globalAcceleration, [group]: val } }))
    },

    loadPreset(steps, totalMs) {
      pushSnapshot()
      const currentDefs: TrackDefinition[] = get().tracks.map(t => ({
        id: t.id, name: t.name, minPosition: t.min, maxPosition: t.max,
        defaultVelocity: t.defaultVelocity, defaultAcceleration: t.defaultAcceleration,
      }))
      const tracks = buildEditorTracks(currentDefs, totalMs)
      steps.forEach(({ t, vals }) => {
        Object.entries(vals).forEach(([id, v]) => {
          if (v === undefined) return
          const tr = tracks.find(x => x.id === id)
          if (tr) tr.kfs.push({ id: uid(), t, v })
        })
      })
      tracks.forEach(tr => {
        if (tr.kfs.length < 2) {
          tr.kfs = [{ id: uid(), t: 0, v: 0 }, { id: uid(), t: totalMs, v: 0 }]
        }
      })
      set({ tracks, totalMs, playhead: 0, selectedKf: null })
    },

    reset() {
      pushSnapshot()
      const { totalMs, tracks } = get()
      const defs: TrackDefinition[] = tracks.map(t => ({
        id: t.id, name: t.name, minPosition: t.min, maxPosition: t.max,
        defaultVelocity: t.defaultVelocity, defaultAcceleration: t.defaultAcceleration,
      }))
      set({ tracks: buildEditorTracks(defs, totalMs), playhead: 0, selectedKf: null })
    },

    async loadTracksFromBackend() {
      try {
        const defs = await trackApi.getAll()
        const { totalMs, tracks: current } = get()
        const updated = buildEditorTracks(defs, totalMs).map(newTrack => {
          const existing = current.find(t => t.id === newTrack.id)
          return existing ? { ...newTrack, kfs: existing.kfs, enabled: existing.enabled } : newTrack
        })
        set({ tracks: updated })
      } catch {
        // Backend non disponible — on garde les tracks fallback
      }
    },
  }
})
