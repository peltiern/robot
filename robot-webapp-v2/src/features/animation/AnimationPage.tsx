import { useEffect, useRef, useState, useCallback } from 'react'
import { useAnimationStore } from './store/animationStore'
import { Toolbar }         from './components/Toolbar'
import { Timeline }        from './components/Timeline'
import { PropertiesPanel } from './components/PropertiesPanel'
import { LibraryPanel }    from './components/LibraryPanel'
import { toAnimation }     from './utils/convert'
import { animationApi }    from '../../shared/api/animationApi'
import type { Animation }  from '../../shared/types/animation'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import styles from './AnimationPage.module.css'

export function AnimationPage() {
  const store      = useAnimationStore()
  const ws         = useWebSocketStore()
  const rafRef     = useRef<number | null>(null)

  // Charger les définitions de tracks depuis le backend au montage
  useEffect(() => { store.loadTracksFromBackend() }, [])
  const originRef  = useRef<number>(0)   // performance.now() quand playhead = 0
  const [modal, setModal] = useState<'export' | 'import' | null>(null)
  const [exportJson, setExportJson]   = useState('')
  const [importJson, setImportJson]   = useState('')
  const [statusMsg, setStatusMsg]     = useState('')
  const [libraryKey, setLibraryKey]   = useState(0)

  // ── Boucle de lecture (RAF) ────────────────────────────────────────────────

  const tick = useCallback(() => {
    const { playing, looping, totalMs, setPlayhead, setPlaying } = useAnimationStore.getState()
    if (!playing) return

    const elapsed = performance.now() - originRef.current
    if (elapsed >= totalMs) {
      if (looping) {
        originRef.current = performance.now()
        setPlayhead(0)
      } else {
        setPlayhead(totalMs)
        setPlaying(false)
        return
      }
    } else {
      setPlayhead(elapsed)
    }
    rafRef.current = requestAnimationFrame(tick)
  }, [])

  function play() {
    const { playhead, totalMs } = store
    originRef.current = performance.now() - (playhead >= totalMs ? 0 : playhead)
    store.setPlaying(true)
    rafRef.current = requestAnimationFrame(tick)
    // Envoyer au robot (tracks désactivées exclues)
    ws.playAnimation(buildAnimationForRobot())
  }

  function pause() {
    store.setPlaying(false)
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    ws.stopAnimation()
  }

  function stop() {
    store.setPlaying(false)
    store.setPlayhead(0)
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    ws.stopAnimation()
  }

  useEffect(() => {
    if (!store.playing && rafRef.current) {
      cancelAnimationFrame(rafRef.current)
    }
  }, [store.playing])

  // Scrubbing vers le robot quand le playhead bouge sans lecture
  useEffect(() => {
    if (!store.playing && ws.connected) {
      ws.scrubAnimation(buildAnimationForRobot(), store.playhead)
    }
  }, [store.playhead])

  // ── Raccourcis clavier ─────────────────────────────────────────────────────

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      const tag = (e.target as HTMLElement).tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
      if (e.code === 'Space')  { e.preventDefault(); store.playing ? pause() : play() }
      if (e.code === 'Home')   { store.setPlaying(false); store.setPlayhead(0) }
      if (e.code === 'End')    { store.setPlaying(false); store.setPlayhead(store.totalMs) }
      if (e.code === 'Delete' || e.code === 'Backspace') {
        const { selectedKf, deleteKf, clearSel } = useAnimationStore.getState()
        if (selectedKf) { deleteKf(selectedKf.trackId, selectedKf.kfId); clearSel() }
      }
      if (e.code === 'KeyL')   { store.toggleLoop() }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [store.playing])

  useEffect(() => {
    function onUndoRedo(e: KeyboardEvent) {
      if (!e.ctrlKey && !e.metaKey) return
      const key = e.key.toLowerCase()
      if (key === 'z' && !e.shiftKey) {
        e.preventDefault()
        useAnimationStore.getState().undo()
      } else if (key === 'y' || (key === 'z' && e.shiftKey)) {
        e.preventDefault()
        useAnimationStore.getState().redo()
      }
    }
    window.addEventListener('keydown', onUndoRedo)
    return () => window.removeEventListener('keydown', onUndoRedo)
  }, [])

  // ── Helpers ────────────────────────────────────────────────────────────────

  /** Toutes les tracks — pour la sauvegarde JSON (les désactivées restent dans le fichier) */
  function buildAnimation() {
    return toAnimation(store.animationName, store.totalMs, store.tracks)
  }

  /** Seulement les tracks actives — pour le play/scrub vers le robot */
  function buildAnimationForRobot() {
    return toAnimation(store.animationName, store.totalMs, store.tracks.filter(t => t.enabled))
  }

  // ── Import / Export ────────────────────────────────────────────────────────

  function openExport() {
    const anim = buildAnimation()
    setExportJson(JSON.stringify(anim, null, 2))
    setModal('export')
  }

  function openImport() {
    setImportJson('')
    setModal('import')
  }

  function doImport() {
    try {
      const data = JSON.parse(importJson)
      if (!data?.tracks) { alert('Format invalide : propriété "tracks" manquante.'); return }
      // Reconvertir Animation → EditorTrack
      const steps = buildStepsFromAnimation(data)
      store.loadPreset(steps, data.totalDuration ?? store.totalMs)
      if (data.name) store.setAnimationName(data.name)
      setModal(null)
    } catch (e: any) {
      alert('JSON invalide : ' + e.message)
    }
  }

  function buildStepsFromAnimation(data: any) {
    // Regrouper toutes les keyframes par temps
    const byTime = new Map<number, any>()
    ;(data.tracks ?? []).forEach((tr: any) => {
      ;(tr.keyframes ?? []).forEach((kf: any) => {
        if (!byTime.has(kf.time)) byTime.set(kf.time, {})
        byTime.get(kf.time)![tr.id] = kf.value
      })
    })
    return [...byTime.entries()].sort((a, b) => a[0] - b[0]).map(([t, vals]) => ({ t, vals }))
  }

  async function saveToServer() {
    try {
      const anim = buildAnimation()
      const res = await animationApi.update(anim.name, anim)
      const warnCount = res.warnings.length
      setStatusMsg(warnCount > 0 ? `Sauvegardé avec ${warnCount} avertissement(s)` : 'Sauvegardé ✓')
      setTimeout(() => setStatusMsg(''), 3000)
      setLibraryKey(k => k + 1)
    } catch {
      setStatusMsg('Erreur lors de la sauvegarde')
    }
  }

  function handleLoadFromLibrary(anim: Animation) {
    const steps = buildStepsFromAnimation(anim)
    store.loadPreset(steps, anim.totalDuration ?? store.totalMs)
    store.setAnimationName(anim.name)
  }

  return (
    <div className={styles.page}>
      <Toolbar
        onPlay={play} onPause={pause} onStop={stop}
        onExport={openExport} onImport={openImport}
      />

      <div className={styles.main}>
        <LibraryPanel refreshKey={libraryKey} onLoadAnimation={handleLoadFromLibrary} onSave={saveToServer} />
        <Timeline />
        <PropertiesPanel />
      </div>

      <footer className={styles.footer}>
        <span className={ws.connected ? styles.dotOn : styles.dotOff} />
        <span>{ws.connected ? 'Robot connecté' : 'Robot déconnecté'}</span>
        <span>·</span>
        <span>Clic = ajouter · Drag = déplacer · Dbl-clic = supprimer · Ctrl+Molette = zoom · Espace = play</span>
        <div className={styles.footerRight}>
          {statusMsg && <span className={styles.statusMsg}>{statusMsg}</span>}
          <button className={styles.saveBtn} onClick={saveToServer}>💾 Sauvegarder</button>
        </div>
      </footer>

      {/* Modal Export */}
      {modal === 'export' && (
        <div className={styles.overlay} onClick={() => setModal(null)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <div className={styles.modalTitle}>Exporter l'animation — JSON</div>
            <textarea className={styles.modalArea} readOnly value={exportJson} />
            <div className={styles.modalFooter}>
              <button className={styles.btn} onClick={() => navigator.clipboard.writeText(exportJson)}>📋 Copier</button>
              <button className={styles.btn} onClick={() => setModal(null)}>Fermer</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Import */}
      {modal === 'import' && (
        <div className={styles.overlay} onClick={() => setModal(null)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <div className={styles.modalTitle}>Importer une animation — JSON</div>
            <textarea
              className={styles.modalArea}
              placeholder='Collez le JSON ici…'
              value={importJson}
              onChange={e => setImportJson(e.target.value)}
            />
            <div className={styles.modalFooter}>
              <button className={styles.btn} onClick={doImport}>📥 Importer</button>
              <button className={styles.btn} onClick={() => setModal(null)}>Annuler</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
