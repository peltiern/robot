import { lazy, Suspense, useEffect, useRef, useState, useCallback } from 'react'
import { useAnimationStore } from './store/animationStore'
import { Toolbar }         from './components/Toolbar'
import { Timeline }        from './components/Timeline'
import { PropertiesPanel } from './components/PropertiesPanel'
import { LibraryPanel }    from './components/LibraryPanel'
import { PanneauAvertissements } from './components/PanneauAvertissements'
import { toAnimation, versEtapesEditeur, versSonsEditeur } from './utils/convert'
import { couperLeNavigateur, jouerDansLeNavigateur, useSonsAtelier } from './utils/sonsAtelier'
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

  // Quitter l'Atelier en pleine lecture ne doit pas laisser un bruitage sonner sur une autre page.
  useEffect(() => couperLeNavigateur, [])
  const originRef  = useRef<number>(0)   // performance.now() quand playhead = 0
  // Dernier instant dont les sons ont été lancés, en simulation : un son part quand la tête le
  // franchit, une fois, et non à chaque image où elle se trouve après lui.
  const derniereTeteRef = useRef<number>(-1)
  // En mode Robot, la tête de lecture attend la réponse du robot, puis le temps que ses moteurs
  // attendent la bande-son : sans ça elle le devancerait d'autant. Elle reste à l'instant de départ.
  const enAttenteDuRobotRef = useRef(false)
  const departRef = useRef(0)
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

    if (enAttenteDuRobotRef.current) {
      rafRef.current = requestAnimationFrame(tick)
      return
    }
    const elapsed = Math.max(departRef.current, performance.now() - originRef.current)
    if (!surRobot) lancerLesSonsFranchis(Math.min(elapsed, totalMs))
    if (elapsed >= totalMs) {
      if (looping) {
        originRef.current = performance.now()
        derniereTeteRef.current = -1
        departRef.current = 0
        setPlayhead(0)
        // Relancer aussi le robot : sa lecture, elle, ne boucle pas. Sans ça la timeline
        // repartait indéfiniment pendant que la tête ne bougeait plus qu'une fois.
        if (surRobot) lancerSurLeRobot(0)
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

  /**
   * En simulation, c'est le navigateur qui fait entendre la piste Son. En mode Robot, jamais : le
   * robot la joue lui-même, et l'entendre deux fois, décalée de la latence du réseau, embrouillerait
   * justement ce qu'on cherche à caler.
   */
  function lancerLesSonsFranchis(tete: number) {
    const { sons } = useAnimationStore.getState()
    for (const son of sons) {
      if (son.t > derniereTeteRef.current && son.t <= tete) jouerDansLeNavigateur(son.nom)
    }
    derniereTeteRef.current = tete
  }

  /** Une lecture reprise au milieu d'un son l'entend à partir de là, pas du début ni pas du tout. */
  function reprendreLesSonsEnCours(tete: number) {
    const { sons } = useAnimationStore.getState()
    const etats = useSonsAtelier.getState().sons
    for (const son of sons) {
      const etat = etats[son.nom]
      const duree = etat?.etat === 'pret' ? etat.duree * 1000 : 0
      if (son.t < tete && tete < son.t + duree) jouerDansLeNavigateur(son.nom, (tete - son.t) / 1000)
    }
    derniereTeteRef.current = tete
  }

  /**
   * Lance l'animation sur le robot à partir de `depart`, et ne fait partir la tête de lecture
   * qu'avec lui : à sa réponse, plus le temps que ses moteurs attendent la bande-son.
   */
  function lancerSurLeRobot(depart: number) {
    enAttenteDuRobotRef.current = true
    departRef.current = depart
    animationApi.jouerBrouillon(animationPourLeRobot(), depart)
      .then(reponse => {
        originRef.current = performance.now() + (reponse.attenteDuSon ?? 0) - depart
        enAttenteDuRobotRef.current = false
      })
      .catch(() => {
        enAttenteDuRobotRef.current = false
        pause()
        setStatusMsg('Le robot a refusé de jouer')
      })
  }

  function play() {
    const { playhead, totalMs, surRobot } = useAnimationStore.getState()
    const depart = playhead >= totalMs ? 0 : playhead
    originRef.current = performance.now() - depart
    departRef.current = depart
    store.setPlaying(true)
    rafRef.current = requestAnimationFrame(tick)
    if (!surRobot) {
      // Juste avant le départ, pour qu'un son posé exactement là parte bien.
      derniereTeteRef.current = depart - 1
      reprendreLesSonsEnCours(depart)
      derniereTeteRef.current = depart - 1
      return
    }
    // Envoyer au robot (pistes désactivées exclues), en brouillon : l'animation en cours
    // d'écriture n'a aucune raison d'être enregistrée pour être essayée. À partir du curseur, et
    // non du début : c'était le cas jusqu'ici, et la tête repartait de zéro sous une timeline au
    // milieu.
    lancerSurLeRobot(depart)
  }

  function pause() {
    store.setPlaying(false)
    enAttenteDuRobotRef.current = false
    couperLeNavigateur()
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    if (useAnimationStore.getState().surRobot) animationApi.arreter().catch(() => {})
  }

  function stop() {
    store.setPlaying(false)
    store.setPlayhead(0)
    enAttenteDuRobotRef.current = false
    couperLeNavigateur()
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
        const { selectedKf, deleteKf, clearSel, selectedSon, deleteSon } = useAnimationStore.getState()
        if (selectedKf) { deleteKf(selectedKf.trackId, selectedKf.kfId); clearSel() }
        else if (selectedSon) deleteSon(selectedSon)
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
      // Dans un champ texte — le nom de l'animation, la durée —, copier et coller restent ceux du
      // texte : un son collé à la place d'un nom tapé serait une mauvaise surprise.
      const cible = e.target as HTMLElement
      const dansUnChamp = cible.tagName === 'INPUT' || cible.tagName === 'TEXTAREA' || cible.isContentEditable
      if (key === 'c' && !dansUnChamp) {
        if (useAnimationStore.getState().copierSon()) e.preventDefault()
      } else if (key === 'v' && !dansUnChamp) {
        if (useAnimationStore.getState().collerSon()) e.preventDefault()
      } else if (key === 'z' && !e.shiftKey) {
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
    return toAnimation(store.animationName, store.totalMs, store.tracks, store.sons)
  }

  /**
   * Seulement les pistes actives — ce qui part au robot, pour la lecture comme pour le curseur.
   *
   * Lu par getState() et non depuis `store` : cette fonction est appelée depuis la boucle
   * d'animation, dont la clôture date du premier rendu. Passer par le rendu y renverrait
   * indéfiniment la première version de la timeline.
   */
  function animationPourLeRobot() {
    const { animationName, totalMs, tracks, sons } = useAnimationStore.getState()
    return toAnimation(animationName, totalMs, tracks.filter(t => t.enabled), sons)
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
    store.chargerEtapes(versEtapesEditeur(anim), anim.dureeTotale ?? store.totalMs, versSonsEditeur(anim))
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
        <span>Clic = ajouter · Drag = déplacer · Dbl-clic = supprimer · « + » de la piste Son = poser un son · Ctrl+C / Ctrl+V = copier / coller un son au curseur · Ctrl+Molette = zoom · Espace = play</span>
        <div className={styles.footerRight}>
          {statusMsg && <span className={styles.statusMsg}>{statusMsg}</span>}
        </div>
      </footer>
    </div>
  )
}
