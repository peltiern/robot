import { lazy, Suspense, useEffect, useRef, useState, useCallback } from 'react'
import { useAnimationStore } from './store/animationStore'
import { Toolbar }         from './components/Toolbar'
import { Timeline }        from './components/Timeline'
import { PropertiesPanel } from './components/PropertiesPanel'
import { LibraryPanel }    from './components/LibraryPanel'
import { PanneauAvertissements } from './components/PanneauAvertissements'
import { toAnimation, versEtapesEditeur } from './utils/convert'
import { animationApi }    from '../../shared/api/animationApi'
import { bibliotheque, exporterFichier } from './utils/bibliotheque'
import type { Animation }  from '../../shared/types/animation'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import styles from './AnimationPage.module.css'

/**
 * Période minimale entre deux envois du curseur, en millisecondes. Calée sur la cadence du
 * lecteur : plus vite ne servirait à rien, le robot écrête de toute façon.
 */
const PERIODE_CURSEUR_MS = 100

// three.js pèse plusieurs centaines de Ko : il n'est chargé qu'à l'affichage de l'aperçu, pour que
// le HUD — qui tourne sur la tablette et n'en a pas l'usage — n'en paie jamais le prix.
const Maquette3D = lazy(() => import('./components/Maquette3D'))

/** En haut pour régler les yeux, à droite pour voir le robot entier. */
export type Disposition = 'haut' | 'droite'
const CLE_DISPOSITION = 'atelier.disposition'

function dispositionRetenue(): Disposition {
  try {
    return localStorage.getItem(CLE_DISPOSITION) === 'haut' ? 'haut' : 'droite'
  } catch {
    return 'droite'
  }
}

export function AnimationPage() {
  const store      = useAnimationStore()
  const ws         = useWebSocketStore()
  const rafRef     = useRef<number | null>(null)
  const dernierCurseurRef = useRef<number>(0)

  // Charger les définitions de tracks depuis le backend au montage
  useEffect(() => { store.loadTracksFromBackend() }, [])
  const originRef  = useRef<number>(0)   // performance.now() quand playhead = 0
  const [statusMsg, setStatusMsg]     = useState('')
  const [libraryKey, setLibraryKey]   = useState(0)
  const [disposition, setDisposition] = useState<Disposition>(dispositionRetenue)

  function changerDisposition(nouvelle: Disposition) {
    setDisposition(nouvelle)
    try { localStorage.setItem(CLE_DISPOSITION, nouvelle) } catch { /* retenue pour la session seulement */ }
  }

  // Robot perdu en mode Robot : on repasse en simulation, sans quoi la lecture continuerait
  // d'envoyer ses ordres dans le vide pendant que l'écran ferait croire qu'il joue.
  useEffect(() => {
    if (!ws.connected) changerDestination(false)
  }, [ws.connected])

  // Le robot revient : ce qui a été enregistré pendant son absence lui part d'un coup, puis la
  // bibliothèque est relue dans tous les cas. Relue seulement après un envoi, elle restait sur
  // « Robot injoignable » quand il n'y avait rien en attente.
  useEffect(() => {
    if (!ws.connected) return
    bibliotheque.envoyerEnAttente().then(({ envoyees, remplacees }) => {
      if (envoyees.length === 0) return
      const remplace = remplacees.length > 0 ? ` (remplacées : ${remplacees.join(', ')})` : ''
      signaler(`${envoyees.length} animation${envoyees.length > 1 ? 's' : ''} envoyée${envoyees.length > 1 ? 's' : ''} au robot${remplace}`)
    }).catch(() => { /* robot pas encore prêt : la prochaine connexion réessaiera */ })
      .finally(() => setLibraryKey(k => k + 1))
  }, [ws.connected])

  // ── Boucle de lecture (RAF) ────────────────────────────────────────────────

  const tick = useCallback(() => {
    const { playing, looping, totalMs, surRobot, setPlayhead, setPlaying } = useAnimationStore.getState()
    if (!playing) return

    const elapsed = performance.now() - originRef.current
    if (elapsed >= totalMs) {
      if (looping) {
        originRef.current = performance.now()
        setPlayhead(0)
        // Relancer aussi le robot : sa lecture, elle, ne boucle pas. Sans ça la timeline
        // repartait indéfiniment pendant que la tête ne bougeait plus qu'une fois.
        if (surRobot) animationApi.jouerBrouillon(animationPourLeRobot()).catch(() => {})
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
    const { playhead, totalMs, surRobot } = useAnimationStore.getState()
    originRef.current = performance.now() - (playhead >= totalMs ? 0 : playhead)
    store.setPlaying(true)
    rafRef.current = requestAnimationFrame(tick)
    if (!surRobot) return
    // Envoyer au robot (pistes désactivées exclues), en brouillon : l'animation en cours
    // d'écriture n'a aucune raison d'être enregistrée pour être essayée.
    animationApi.jouerBrouillon(animationPourLeRobot())
      .catch(() => setStatusMsg('Le robot a refusé de jouer'))
  }

  function pause() {
    store.setPlaying(false)
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    if (useAnimationStore.getState().surRobot) animationApi.arreter().catch(() => {})
  }

  function stop() {
    store.setPlaying(false)
    store.setPlayhead(0)
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    if (useAnimationStore.getState().surRobot) animationApi.arreter().catch(() => {})
  }

  /**
   * Change la destination de la lecture. En pleine lecture, on s'arrête d'abord : basculer sans
   * arrêter laisserait le robot finir seul un geste que l'écran ne suit plus, ou l'inverse.
   */
  function changerDestination(versRobot: boolean) {
    const { surRobot, playing, setSurRobot } = useAnimationStore.getState()
    if (versRobot === surRobot) return
    if (playing) pause()
    setSurRobot(versRobot)
  }

  /** Depuis la bibliothèque, en Simulation : l'animation vient d'être chargée, elle se joue du début. */
  function jouerDepuisLeDebut() {
    if (useAnimationStore.getState().playing) pause()
    store.setPlayhead(0)
    play()
  }

  useEffect(() => {
    if (!store.playing && rafRef.current) {
      cancelAnimationFrame(rafRef.current)
    }
  }, [store.playing])

  // Curseur : la tête suit la timeline quand on la tire, hors lecture, et seulement en mode Robot.
  //
  // Bridé ici EN PLUS de l'écrêtage du lecteur, pour deux raisons distinctes : le robot se
  // protège de tout client, et l'éditeur évite d'inonder le websocket d'un message par pixel
  // de souris — l'animation entière voyage à chaque envoi.
  useEffect(() => {
    if (store.playing || !ws.connected || !store.surRobot) return
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

  function signaler(message: string, dureeMs = 5000) {
    setStatusMsg(message)
    setTimeout(() => setStatusMsg(m => m === message ? '' : m), dureeMs)
  }

  async function saveToServer() {
    const resultat = await bibliotheque.enregistrer(buildAnimation())
    store.marquerEnregistre()
    setLibraryKey(k => k + 1)
    if (resultat.ou === 'robot') {
      signaler('Sauvegardé ✓', 3000)
    } else {
      signaler(`Gardée dans le navigateur, partira au robot à son retour (${resultat.raison})`, 8000)
    }
  }

  function handleLoadFromLibrary(anim: Animation) {
    store.chargerEtapes(versEtapesEditeur(anim), anim.dureeTotale ?? store.totalMs)
    store.setAnimationName(anim.nom)
  }

  return (
    <div className={`${styles.page} ${disposition === 'haut' ? styles.enHaut : styles.aDroite}`}>
      {/* L'aperçu reste au même endroit du DOM quelle que soit la disposition : c'est la grille qui le
          place. Déplacé d'un parent à l'autre, React le démonterait et le modèle serait rechargé. */}
      <div className={styles.apercu}>
        <Suspense fallback={<div className={styles.chargement}>Chargement de la maquette…</div>}>
          <Maquette3D className={styles.maquette} disposition={disposition} onChangerDisposition={changerDisposition} />
        </Suspense>
      </div>

      <div className={styles.barre}>
        <Toolbar
          onPlay={play} onPause={pause} onStop={stop} onSave={saveToServer}
          onChangerDestination={changerDestination}
          robotJoignable={ws.connected}
        />
      </div>

      <div className={styles.main}>
        <div className={styles.gauche}>
          <LibraryPanel refreshKey={libraryKey} onLoadAnimation={handleLoadFromLibrary} onSave={saveToServer} onJouer={jouerDepuisLeDebut}
                        onExporter={() => exporterFichier(buildAnimation())} />
          <PropertiesPanel />
        </div>
        <Timeline />
      </div>

      <PanneauAvertissements />

      <footer className={styles.footer}>
        <span className={ws.connected ? styles.dotOn : styles.dotOff} />
        <span>{ws.connected ? 'Robot connecté' : 'Robot déconnecté'}</span>
        <span>·</span>
        <span>Clic = ajouter · Drag = déplacer · Dbl-clic = supprimer · Ctrl+Molette = zoom · Espace = play</span>
        <div className={styles.footerRight}>
          {statusMsg && <span className={styles.statusMsg}>{statusMsg}</span>}
        </div>
      </footer>
    </div>
  )
}
