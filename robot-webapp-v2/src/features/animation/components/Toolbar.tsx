import { useRef } from 'react'
import { useAnimationStore } from '../store/animationStore'
import { fmtMs } from '../utils/convert'
import styles from './Toolbar.module.css'

const PRESETS = {
  NEUTRAL:   [{ t: 200, vals: { OEIL_GAUCHE: 0, OEIL_DROIT: 0, COU_GAUCHE_DROITE: 0, COU_HAUT_BAS: 0 } }],
  THINKING:  [{ t: 200, vals: { OEIL_GAUCHE: -10, OEIL_DROIT: 0, COU_GAUCHE_DROITE: 20, COU_HAUT_BAS: 20 } }],
  SAD:       [{ t: 100, vals: { OEIL_GAUCHE: -24, OEIL_DROIT: -24, COU_GAUCHE_DROITE: 0, COU_HAUT_BAS: 0 } }],
  SURPRISED: [{ t: 100, vals: { OEIL_GAUCHE: 0, OEIL_DROIT: -20 } }],
  AMAZED:    [
    { t: 100, vals: { OEIL_GAUCHE: -3, OEIL_DROIT: -3, COU_HAUT_BAS: 20 } },
    { t: 600, vals: { OEIL_GAUCHE: 0,  OEIL_DROIT: 0,  COU_HAUT_BAS: 0  } },
  ],
} as const

interface Props {
  onPlay:   () => void
  onPause:  () => void
  onStop:   () => void
  onExport: () => void
  onImport: () => void
}

export function Toolbar({ onPlay, onPause, onStop, onExport, onImport }: Props) {
  const {
    playing, looping, playhead, totalMs, animationName,
    setPlaying, toggleLoop, setTotalMs, setAnimationName, loadPreset,
    setPxPerMs, pxPerMs,
  } = useAnimationStore()

  const selectRef = useRef<HTMLSelectElement>(null)

  function handlePreset(e: React.ChangeEvent<HTMLSelectElement>) {
    const key = e.target.value as keyof typeof PRESETS
    if (!key) return
    const steps = PRESETS[key] as any
    loadPreset(steps, totalMs)
    e.target.value = ''
  }

  return (
    <div className={styles.toolbar}>
      {/* Nom de l'animation */}
      <input
        className={styles.nameInput}
        value={animationName}
        onChange={e => setAnimationName(e.target.value)}
        title="Nom de l'animation"
      />

      <div className={styles.sep} />

      {/* Transport */}
      <button className={styles.iconBtn} onClick={() => { setPlaying(false); useAnimationStore.getState().setPlayhead(0) }} title="Retour début (Home)">⏮</button>
      <button className={styles.iconBtn} onClick={playing ? onPause : onPlay} title="Lecture / Pause (Espace)">
        {playing ? '⏸' : '▶'}
      </button>
      <button className={styles.iconBtn} onClick={onStop} title="Stop">⏹</button>
      <button
        className={`${styles.iconBtn} ${looping ? styles.active : ''}`}
        onClick={toggleLoop}
        title="Boucle"
      >🔁</button>

      <div className={styles.sep} />
      <span className={styles.timeDisp}>{fmtMs(playhead)}</span>
      <div className={styles.sep} />

      {/* Durée */}
      <label className={styles.dim}>Durée</label>
      <input
        type="number"
        className={styles.numInput}
        value={totalMs}
        min={500}
        max={30000}
        step={500}
        onChange={e => setTotalMs(Math.max(500, parseInt(e.target.value) || 3000))}
      />
      <label className={styles.dim}>ms</label>

      <div className={styles.sep} />

      {/* Zoom */}
      <label className={styles.dim}>Zoom</label>
      <button className={styles.iconBtn} onClick={() => setPxPerMs(pxPerMs / 1.3)} title="Ctrl+Molette">−</button>
      <button className={styles.iconBtn} onClick={() => setPxPerMs(pxPerMs * 1.3)} title="Ctrl+Molette">+</button>

      <div className={styles.sep} />

      {/* Présets */}
      <label className={styles.dim}>Préset</label>
      <select className={styles.select} ref={selectRef} defaultValue="" onChange={handlePreset}>
        <option value="">— choisir —</option>
        {Object.keys(PRESETS).map(k => <option key={k} value={k}>{k}</option>)}
      </select>

      <div className={styles.spacer} />

      <button className={styles.btn} onClick={onImport}>📂 Importer</button>
      <button className={styles.btn} onClick={onExport}>💾 Exporter</button>
    </div>
  )
}
