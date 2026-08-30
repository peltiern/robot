import { useEffect, useRef, useState, useCallback } from 'react'
import { useAnimationStore } from './store/animationStore'
import { Toolbar }         from './components/Toolbar'
import { Timeline }        from './components/Timeline'
import { PropertiesPanel } from './components/PropertiesPanel'
import { LibraryPanel }    from './components/LibraryPanel'
import { toAnimation, versEtapesEditeur } from './utils/convert'
import { animationApi }    from '../../shared/api/animationApi'
import type { Animation }  from '../../shared/types/animation'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import styles from './AnimationPage.module.css'

/**
 * Période minimale entre deux envois du curseur, en millisecondes. Calée sur la cadence du
 * lecteur : plus vite ne servirait à rien, le robot écrête de toute façon.
 */
const PERIODE_CURSEUR_MS = 100

export function AnimationPage() {
  const store      = useAnimationStore()
  const ws         = useWebSocketStore()
  const rafRef     = useRef<number | null>(null)
  const dernierCurseurRef = useRef<number>(0)

  // Charger les définitions de tracks depuis le backend au montage
  useEffect(() => { store.loadTracksFromBackend() }, [])
  const originRef  = useRef<number>(0)   // performance.now() quand playhead = 0
  const [modal, setModal] = useState<'export' | 'import' | null>(null)
  const [exportJson, setExportJson]   = useState('')
  const [importJson, setImportJson]   = useState('')
  const [statusMsg, setStatusMsg]     = useState('')
  const [avertissements, setAvertissements] = useState<string[]>([])
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
        // Relancer aussi le robot : sa lecture, elle, ne boucle pas. Sans ça la timeline
        // repartait indéfiniment pendant que la tête ne bougeait plus qu'une fois.
        animationApi.jouerBrouillon(animationPourLeRobot()).catch(() => {})
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
    // Envoyer au robot (pistes désactivées exclues), en brouillon : l'animation en cours
    // d'écriture n'a aucune raison d'être enregistrée pour être essayée.
    animationApi.jouerBrouillon(animationPourLeRobot())
      .then(lecture => afficherAvertissements(lecture.avertissements))
      .catch(() => setStatusMsg('Le robot a refusé de jouer'))
  }

  function pause() {
    store.setPlaying(false)
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    animationApi.arreter().catch(() => {})
  }

  function stop() {
    store.setPlaying(false)
    store.setPlayhead(0)
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    animationApi.arreter().catch(() => {})
  }

  useEffect(() => {
    if (!store.playing && rafRef.current) {
      cancelAnimationFrame(rafRef.current)
    }
  }, [store.playing])

  // Curseur : la tête suit la timeline quand on la tire, hors lecture.
  //
  // Bridé ici EN PLUS de l'écrêtage du lecteur, pour deux raisons distinctes : le robot se
  // protège de tout client, et l'éditeur évite d'inonder le websocket d'un message par pixel
  // de souris — l'animation entière voyage à chaque envoi.
  useEffect(() => {
    if (store.playing || !ws.connected) return
    const maintenant = performance.now()
    if (maintenant - dernierCurseurRef.current < PERIODE_CURSEUR_MS) return
    dernierCurseurRef.current = maintenant
    ws.deplacerCurseur(animationPourLeRobot(), store.playhead)
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

  /**
   * Seulement les pistes actives — ce qui part au robot, pour la lecture comme pour le curseur.
   *
   * Lu par getState() et non depuis `store` : cette fonction est appelée depuis la boucle
   * d'animation, dont la clôture date du premier rendu. Passer par le rendu y renverrait
   * indéfiniment la première version de la timeline.
   */
  function animationPourLeRobot() {
    const { animationName, totalMs, tracks } = useAnimationStore.getState()
    return toAnimation(animationName, totalMs, tracks.filter(t => t.enabled))
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
      store.loadPreset(versEtapesEditeur(data), data.dureeTotale ?? store.totalMs)
      if (data.nom) store.setAnimationName(data.nom)
      setModal(null)
    } catch (e: any) {
      alert('JSON invalide : ' + e.message)
    }
  }

  async function saveToServer() {
    try {
      const anim = buildAnimation()
      const avertissements = await animationApi.enregistrer(anim.nom, anim)
      setAvertissements(avertissements)
      store.marquerEnregistre()
      setStatusMsg(avertissements.length > 0
        ? `Sauvegardé avec ${avertissements.length} avertissement(s)`
        : 'Sauvegardé ✓')
      setTimeout(() => setStatusMsg(''), 3000)
      setLibraryKey(k => k + 1)
    } catch {
      setStatusMsg('Erreur lors de la sauvegarde')
    }
  }

  function handleLoadFromLibrary(anim: Animation) {
    store.loadPreset(versEtapesEditeur(anim), anim.dureeTotale ?? store.totalMs)
    store.setAnimationName(anim.nom)
    setAvertissements([])
  }

  /**
   * Les avertissements du vérificateur : ce que le robot ne saura pas suivre. Ils n'empêchent
   * ni d'enregistrer ni de jouer — le mouvement sera simplement en retard sur la courbe — mais
   * sans eux l'écart entre ce que la timeline montre et ce que la tête fait reste inexplicable.
   */
  function afficherAvertissements(nouveaux: string[]) {
    setAvertissements(nouveaux)
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

      {avertissements.length > 0 && (
        <div className={styles.avertissements}>
          <strong>Le robot ne suivra pas exactement :</strong>
          <ul>
            {avertissements.map((a, i) => <li key={i}>{a}</li>)}
          </ul>
          <button className={styles.btn} onClick={() => setAvertissements([])}>Masquer</button>
        </div>
      )}

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
