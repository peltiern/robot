import { create } from 'zustand'
import type { Axe, AxeAnimable } from '../../../shared/types/animation'
import { axeApi } from '../../../shared/api/axeApi'
import { assignTrackColors } from '../../../shared/utils/trackColors'
import { uid } from '../utils/convert'
import { corriger, corrigerTout } from '../utils/corrections'
import type { Avertissement } from '../utils/verificateur'

export interface EditorKeyframe { id: string; t: number; v: number }

export interface EditorTrack {
  id: Axe
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

/** Ce que le défilement laisse voir après la fin : sans elle, l'image-clé de fin est coupée en deux. */
export const MARGE_FIN_PX = 14

// Axes de repli, le temps que le robot réponde — et surtout quand il ne répond pas : c'est avec
// eux que la simulation tourne robot éteint. Valeurs du robot au 2026-09-11, en degrés d'organe.
// Les précédentes dataient d'avant les vrais degrés du 2026-09-08 : elles donnaient à
// l'inclinaison une course de 15° quand elle en a 93, et aux yeux l'ancien repère. Elles restent un
// pis-aller : /api/axes-animables les remplace dès qu'il répond.
const AXES_DE_REPLI: AxeAnimable[] = [
  { id: 'OEIL_GAUCHE',          libelle: 'Œil gauche',             positionMin: -33,   positionMax:  5.5, vitesseParDefaut:  79, accelerationParDefaut: 119 },
  { id: 'OEIL_DROIT',           libelle: 'Œil droit',              positionMin: -33,   positionMax:  5.5, vitesseParDefaut:  79, accelerationParDefaut: 119 },
  { id: 'COU_GAUCHE_DROITE',    libelle: 'Cou gauche / droite',    positionMin: -95,   positionMax: 95,   vitesseParDefaut:  63, accelerationParDefaut: 317 },
  { id: 'COU_HAUT_BAS',         libelle: 'Cou haut / bas',         positionMin: -39.8, positionMax: 53,   vitesseParDefaut:  50, accelerationParDefaut: 994 },
  { id: 'COU_MONTER_DESCENDRE', libelle: 'Cou monter / descendre', positionMin: -10,   positionMax: 60,   vitesseParDefaut: 100, accelerationParDefaut: 200 },
]

/** Le chemin inverse de buildEditorTracks : reconstruire les axes depuis l'état de l'éditeur. */
function versAxeAnimable(t: EditorTrack): AxeAnimable {
  return {
    id: t.id,
    libelle: t.name,
    positionMin: t.min,
    positionMax: t.max,
    vitesseParDefaut: t.defaultVelocity,
    accelerationParDefaut: t.defaultAcceleration,
  }
}

function buildEditorTracks(defs: AxeAnimable[], totalMs: number): EditorTrack[] {
  const colors = assignTrackColors(defs.map(d => d.id))
  return defs.map(def => ({
    id: def.id,
    name:    def.libelle ?? def.id,
    color:   colors[def.id],
    enabled: true,
    min: def.positionMin,
    max: def.positionMax,
    defaultVelocity:     def.vitesseParDefaut,
    defaultAcceleration: def.accelerationParDefaut,
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

  /**
   * Vrai dès qu'une modification n'a pas été enregistrée. Sert à ne demander « sauvegarder
   * d'abord ? » que quand il y a vraiment quelque chose à perdre : poser la question à chaque
   * clic dans la bibliothèque faisait passer le chargement pour cassé.
   */
  modifie: boolean
  marquerEnregistre: () => void

  // Historique
  past: HistorySnapshot[]
  future: HistorySnapshot[]

  // Lecture
  playhead: number
  playing: boolean
  looping: boolean
  /**
   * Vrai quand lecture et curseur font bouger le vrai robot. Faux au départ : ouvrir la page ou
   * tirer le curseur ne doit jamais faire bouger la tête par surprise.
   */
  surRobot: boolean

  // Éditeur
  pxPerMs: number
  scrollX: number
  /** Largeur de la zone des pistes : sans elle, impossible de savoir où s'arrête le défilement. */
  largeurVisible: number
  selectedKf: { trackId: Axe; kfId: string } | null

  // Actions — historique
  snapshot: () => void
  undo:     () => void
  redo:     () => void

  // Actions — lecture
  setPlayhead:    (t: number) => void
  setPlaying:     (b: boolean) => void
  toggleLoop:     () => void
  setSurRobot:    (b: boolean) => void
  setTotalMs:     (ms: number) => void

  // Actions — vue
  setPxPerMs:  (v: number) => void
  setScrollX:  (v: number) => void
  zoom:        (factor: number, atPx: number, canvasW: number) => void
  setLargeurVisible:     (px: number) => void
  zoomerAutourDuCurseur: (facteur: number) => void
  toutVoir:              () => void

  // Actions — keyframes
  selectKf:    (trackId: Axe, kfId: string) => void
  clearSel:    () => void
  addKf:       (trackId: Axe, t: number, v: number) => EditorKeyframe
  moveKf:      (trackId: Axe, kfId: string, t: number, v: number) => void
  deleteKf:    (trackId: Axe, kfId: string) => void
  updateSelKf: (t: number, v: number) => void

  // Actions — animation
  setAnimationName:       (n: string) => void
  toggleTrack:            (trackId: Axe) => void
  chargerEtapes:          (steps: Array<{ t: number; vals: Partial<Record<Axe, number>> }>, totalMs: number) => void
  reset:                  () => void
  /** Corrige ce qu'un avertissement signale ; une édition comme une autre, donc annulable. */
  corrigerAvertissement:  (avertissement: Avertissement) => void
  toutCorriger:           () => void
  loadTracksFromBackend:  () => Promise<void>
}

export const useAnimationStore = create<AnimationEditorState>((set, get) => {

  // Fonction locale — accès direct à set/get, pas via get().snapshot()
  function pushSnapshot() {
    const { past, tracks, animationName, totalMs } = get()
    set({
      past: [...past.slice(-(MAX_HISTORY - 1)), { tracks, animationName, totalMs }],
      future: [],
      modifie: true,
    })
  }

  // Le défilement s'arrête à la fin de l'animation : au-delà il n'y a rien à voir, et c'est là
  // qu'un dézoom laissait la timeline, vide, sans rien pour en revenir.
  function borner(scrollX: number, pxPerMs = get().pxPerMs, totalMs = get().totalMs) {
    const { largeurVisible } = get()
    const max = largeurVisible > 0 ? Math.max(0, totalMs * pxPerMs + MARGE_FIN_PX - largeurVisible) : Infinity
    return Math.max(0, Math.min(scrollX, max))
  }

  return {
    animationName: 'NouvelleAnimation',
    modifie: false,
    marquerEnregistre: () => set({ modifie: false }),
    totalMs: 3000,
    tracks: buildEditorTracks(AXES_DE_REPLI, 3000),
    past: [],
    future: [],
    playhead: 0,
    playing: false,
    looping: true,
    surRobot: false,
    pxPerMs: 0.17,
    scrollX: 0,
    largeurVisible: 0,
    selectedKf: null,

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
    setSurRobot:  b  => set({ surRobot: b }),
    setTotalMs(ms) {
      pushSnapshot()
      set({ totalMs: ms, scrollX: borner(get().scrollX, get().pxPerMs, ms) })
    },

    setPxPerMs(v) {
      const pxPerMs = Math.max(0.015, Math.min(3, v))
      set({ pxPerMs, scrollX: borner(get().scrollX, pxPerMs) })
    },
    setScrollX: v  => set({ scrollX: borner(v) }),

    setLargeurVisible(px) {
      set({ largeurVisible: px })
      set({ scrollX: borner(get().scrollX) })
    },

    // Autour du curseur plutôt que du bord gauche : on zoome pour regarder ce qu'on édite, et le
    // curseur est là où on édite. Hors de la vue, il ne peut servir de pivot : le milieu le remplace.
    zoomerAutourDuCurseur(facteur) {
      const { playhead, pxPerMs, scrollX, largeurVisible } = get()
      const xCurseur = playhead * pxPerMs - scrollX
      const pivot = xCurseur >= 0 && xCurseur <= largeurVisible ? xCurseur : largeurVisible / 2
      get().zoom(facteur, pivot, largeurVisible)
    },

    toutVoir() {
      const { largeurVisible, totalMs } = get()
      if (largeurVisible <= 0) return
      // Un peu moins que la largeur exacte : sinon l'image-clé de fin tombe sur le bord, à moitié coupée.
      set({ pxPerMs: Math.max(0.015, Math.min(3, largeurVisible * 0.97 / totalMs)), scrollX: 0 })
    },

    zoom(factor, atPx, _canvasW) {
      const { pxPerMs, scrollX } = get()
      const atT = (atPx + scrollX) / pxPerMs
      const next = Math.max(0.015, Math.min(3, pxPerMs * factor))
      set({ pxPerMs: next, scrollX: borner(atT * next - atPx, next) })
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

    corrigerAvertissement(avertissement) {
      pushSnapshot()
      const { tracks, totalMs } = get()
      set(corriger({ tracks, totalMs }, avertissement))
    },

    toutCorriger() {
      pushSnapshot()
      const { tracks, totalMs } = get()
      set(corrigerTout({ tracks, totalMs }))
    },

    toggleTrack(trackId) {
      pushSnapshot()
      set(s => ({
        tracks: s.tracks.map(t =>
          t.id === trackId ? { ...t, enabled: !t.enabled } : t
        ),
      }))
    },

    chargerEtapes(steps, totalMs) {
      pushSnapshot()
      const currentDefs = get().tracks.map(versAxeAnimable)
      const tracks = buildEditorTracks(currentDefs, totalMs)
      // Vider les images-clés que buildEditorTracks pose d'office (une à 0, une à la fin) avant
      // d'ajouter celles qu'on charge : sans ça, une animation relue en portait deux de plus,
      // toutes deux à zéro, aux instants où elle avait justement quelque chose à dire.
      tracks.forEach(tr => { tr.kfs = [] })
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
      set({ tracks, totalMs, playhead: 0, selectedKf: null, modifie: false })
    },

    reset() {
      pushSnapshot()
      const { totalMs, tracks } = get()
      const defs = tracks.map(versAxeAnimable)
      set({ tracks: buildEditorTracks(defs, totalMs), playhead: 0, selectedKf: null, modifie: false })
    },

    async loadTracksFromBackend() {
      try {
        const defs = await axeApi.tous()
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
