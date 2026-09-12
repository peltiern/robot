import { useState, useEffect, useCallback } from 'react'
import { Icone } from '../../../shared/components/Icone'
import { animationApi } from '../../../shared/api/animationApi'
import type { Animation } from '../../../shared/types/animation'
import { useAnimationStore } from '../store/animationStore'
import styles from './LibraryPanel.module.css'

interface Props {
  refreshKey: number
  onLoadAnimation: (anim: Animation) => void
  onSave: () => Promise<void>
  /** Joue depuis le début ce que l'éditeur vient de charger — la lecture en Simulation. */
  onJouer: () => void
}

export function LibraryPanel({ refreshKey, onLoadAnimation, onSave, onJouer }: Props) {
  const store = useAnimationStore()
  const [names, setNames]               = useState<string[]>([])
  const [loading, setLoading]           = useState(false)
  const [busy, setBusy]                 = useState<string | null>(null)
  const [error, setError]               = useState<string | null>(null)
  const [pendingDelete, setPendingDelete] = useState<string | null>(null)
  const [pendingLoad, setPendingLoad]     = useState<string | null>(null)
  const [pendingNew, setPendingNew]       = useState(false)
  const [jouerApresChargement, setJouerApresChargement] = useState(false)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await animationApi.noms()
      setNames([...list].sort((a, b) => a.localeCompare(b, 'fr')))
    } catch {
      setError('Backend non disponible')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh, refreshKey])

  function doNew() {
    store.reset()
    store.setAnimationName('NouvelleAnimation')
  }

  function handleNew() {
    if (busy || pendingDelete || pendingLoad) return
    setPendingNew(true)
  }

  async function confirmNew(saveFirst: boolean) {
    setPendingNew(false)
    if (saveFirst) {
      try { await onSave() } catch { /* erreur déjà gérée dans onSave */ }
    }
    doNew()
  }

  function requestLoad(name: string, puisJouer = false) {
    if (busy || pendingDelete || pendingNew) return
    // La question ne se pose que s'il y a quelque chose à perdre. La poser à chaque clic — y
    // compris juste après avoir chargé une animation qu'on n'a pas touchée — faisait passer le
    // chargement pour cassé : on cliquait, un panneau s'ouvrait, et rien ne semblait se passer.
    if (!store.modifie) {
      chargerAnimation(name, false, puisJouer)
      return
    }
    setJouerApresChargement(puisJouer)
    setPendingLoad(name)
  }

  async function confirmLoad(saveFirst: boolean) {
    const name = pendingLoad!
    setPendingLoad(null)
    await chargerAnimation(name, saveFirst, jouerApresChargement)
  }

  async function chargerAnimation(name: string, saveFirst: boolean, puisJouer: boolean) {
    setBusy(name)
    try {
      if (saveFirst) await onSave()
      const anim = await animationApi.charger(name)
      onLoadAnimation(anim)
      if (puisJouer) onJouer()
    } catch {
      setError(`Impossible de charger "${name}"`)
    } finally {
      setBusy(null)
    }
  }

  async function handlePlay(name: string, e: React.MouseEvent) {
    e.stopPropagation()
    if (busy || pendingDelete || pendingLoad) return
    // En Simulation, ce bouton ne doit pas plus faire bouger la tête que le reste de la page :
    // l'animation est chargée dans l'éditeur et jouée dans la maquette, seul endroit où elle se voit.
    if (!store.surRobot) {
      requestLoad(name, true)
      return
    }
    setBusy(name + ':play')
    try {
      await animationApi.jouer(name)
    } catch {
      setError(`Impossible de jouer "${name}"`)
    } finally {
      setBusy(null)
    }
  }

  function requestDelete(name: string, e: React.MouseEvent) {
    e.stopPropagation()
    if (busy || pendingLoad || pendingNew) return
    setPendingDelete(name)
  }

  async function confirmDelete(saveFirst: boolean) {
    const name = pendingDelete!
    setPendingDelete(null)
    setBusy(name + ':del')
    try {
      if (saveFirst) await onSave()
      await animationApi.supprimer(name)
      if (store.animationName === name) doNew()
      await refresh()
    } catch {
      setError(`Impossible de supprimer "${name}"`)
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <span className={styles.title}>BIBLIOTHÈQUE</span>
        <button className={styles.newBtn} onClick={handleNew} title="Nouvelle animation"><Icone nom="plus" taille={16} /></button>
      </div>

      {pendingNew && (
        <div className={styles.confirmPanel}>
          <span className={styles.confirmMsg}>Sauvegarder l'animation en cours ?</span>
          <div className={styles.confirmBtns}>
            <button className={`${styles.confirmBtn} ${styles.confirmSave}`} onClick={() => confirmNew(true)}>Sauvegarder</button>
            <button className={`${styles.confirmBtn} ${styles.confirmSkip}`} onClick={() => confirmNew(false)}>Ignorer</button>
            <button className={`${styles.confirmBtn} ${styles.confirmCancel}`} onClick={() => setPendingNew(false)}>Annuler</button>
          </div>
        </div>
      )}

      <div className={styles.list}>
        {loading && <div className={styles.hint}>Chargement…</div>}

        {!loading && error && <div className={styles.errorMsg}>{error}</div>}

        {!loading && !error && names.length === 0 && (
          <div className={styles.hint}>Aucune animation sauvegardée</div>
        )}

        {names.map(name => {
          const isActive  = store.animationName === name
          const isLoading = busy === name
          const isPlaying = busy === name + ':play'
          const isDel     = busy === name + ':del'
          const anyPending = !!pendingDelete || !!pendingLoad || pendingNew

          if (pendingDelete === name) {
            return (
              <div key={name} className={`${styles.item} ${styles.confirming}`}>
                <div className={styles.confirmContent}>
                  <span className={styles.confirmMsg}>Sauvegarder l'animation en cours ?</span>
                  <div className={styles.confirmBtns}>
                    <button className={`${styles.confirmBtn} ${styles.confirmSave}`} onClick={() => confirmDelete(true)}>Sauvegarder</button>
                    <button className={`${styles.confirmBtn} ${styles.confirmSkip}`} onClick={() => confirmDelete(false)}>Ignorer</button>
                    <button className={`${styles.confirmBtn} ${styles.confirmCancel}`} onClick={() => setPendingDelete(null)}>Annuler</button>
                  </div>
                </div>
              </div>
            )
          }

          if (pendingLoad === name) {
            return (
              <div key={name} className={`${styles.item} ${styles.confirming}`}>
                <div className={styles.confirmContent}>
                  <span className={styles.confirmMsg}>Sauvegarder l'animation en cours ?</span>
                  <div className={styles.confirmBtns}>
                    <button className={`${styles.confirmBtn} ${styles.confirmSave}`} onClick={() => confirmLoad(true)}>Sauvegarder</button>
                    <button className={`${styles.confirmBtn} ${styles.confirmSkip}`} onClick={() => confirmLoad(false)}>Ignorer</button>
                    <button className={`${styles.confirmBtn} ${styles.confirmCancel}`} onClick={() => setPendingLoad(null)}>Annuler</button>
                  </div>
                </div>
              </div>
            )
          }

          return (
            <div
              key={name}
              className={`${styles.item} ${isActive ? styles.active : ''} ${busy ? styles.itemBusy : ''}`}
              onClick={() => requestLoad(name)}
              title={name}
            >
              <span className={styles.itemName}>
                {isLoading && <Icone nom="sablier" taille={11} />}{name}
              </span>
              <div className={styles.actions}>
                <button
                  className={styles.actionBtn}
                  onClick={e => handlePlay(name, e)}
                  title={store.surRobot ? 'Jouer sur le robot' : 'Charger et jouer dans la maquette'}
                  disabled={!!busy || anyPending}
                >
                  <Icone nom={isPlaying ? 'sablier' : 'reprise'} taille={13} />
                </button>
                <button
                  className={`${styles.actionBtn} ${styles.deleteBtn}`}
                  onClick={e => requestDelete(name, e)}
                  title="Supprimer"
                  disabled={!!busy || anyPending}
                >
                  <Icone nom={isDel ? 'sablier' : 'croix'} taille={13} />
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
