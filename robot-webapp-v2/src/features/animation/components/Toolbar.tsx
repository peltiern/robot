import { Icone } from '../../../shared/components/Icone'
import { useAnimationStore } from '../store/animationStore'
import { fmtMs } from '../utils/convert'
import styles from './Toolbar.module.css'

const TAILLE = 18

interface Props {
  onPlay:  () => void
  onPause: () => void
  onStop:  () => void
  onSave:  () => void
  /** L'appelant décide de ce qu'il faut arrêter en chemin, la barre ne fait que demander. */
  onChangerDestination: (surRobot: boolean) => void
  robotJoignable: boolean
}

export function Toolbar({ onPlay, onPause, onStop, onSave, onChangerDestination, robotJoignable }: Props) {
  const {
    playing, looping, playhead, totalMs, animationName, surRobot, modifie,
    setPlaying, toggleLoop, setTotalMs, setAnimationName,
    zoomerAutourDuCurseur, toutVoir, past, future, undo, redo,
  } = useAnimationStore()

  return (
    <div className={styles.toolbar}>
      {/* Nom de l'animation */}
      <input
        className={styles.nameInput}
        value={animationName}
        onChange={e => setAnimationName(e.target.value)}
        title="Nom de l'animation"
      />
      {/* À côté du nom, puisque c'est sous ce nom qu'elle part. Allumée tant qu'il reste des
          modifications à perdre. */}
      <button
        className={`${styles.iconBtn} ${modifie ? styles.active : ''}`}
        onClick={onSave}
        title={modifie ? 'Sauvegarder — modifications non enregistrées' : 'Sauvegarder'}
      ><Icone nom="disquette" taille={TAILLE} /></button>

      <div className={styles.sep} />

      {/* Undo / Redo */}
      <button className={styles.iconBtn} onClick={undo} disabled={past.length === 0} title="Annuler (Ctrl+Z)"><Icone nom="annuler" taille={TAILLE} /></button>
      <button className={styles.iconBtn} onClick={redo} disabled={future.length === 0} title="Rétablir (Ctrl+Y)"><Icone nom="retablir" taille={TAILLE} /></button>

      <div className={styles.sep} />

      {/* Transport */}
      <button className={styles.iconBtn} onClick={() => { setPlaying(false); useAnimationStore.getState().setPlayhead(0) }} title="Retour début (Home)"><Icone nom="debut" taille={TAILLE} /></button>
      <button className={styles.iconBtn} onClick={playing ? onPause : onPlay} title="Lecture / Pause (Espace)">
        <Icone nom={playing ? 'pause' : 'reprise'} taille={TAILLE} />
      </button>
      <button className={styles.iconBtn} onClick={onStop} title="Stop"><Icone nom="stop" taille={TAILLE} /></button>
      <button
        className={`${styles.iconBtn} ${looping ? styles.active : ''}`}
        onClick={toggleLoop}
        title="Boucle"
      ><Icone nom="boucle" taille={TAILLE} /></button>

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

      {/* Zoom : en double de Ctrl+Molette, qui n'existe pas sur la tablette. */}
      <label className={styles.dim}>Zoom</label>
      <button className={styles.iconBtn} onClick={() => zoomerAutourDuCurseur(1 / 1.3)} title="Dézoomer (Ctrl+Molette)"><Icone nom="moins" taille={TAILLE} /></button>
      <button className={styles.iconBtn} onClick={() => zoomerAutourDuCurseur(1.3)} title="Zoomer autour du curseur (Ctrl+Molette)"><Icone nom="plus" taille={TAILLE} /></button>
      <button className={styles.iconBtn} onClick={toutVoir} title="Voir toute l'animation"><Icone nom="toutVoir" taille={TAILLE} /></button>

      <div className={styles.spacer} />

      {/* Deux boutons plutôt qu'une case à cocher : on voit toujours où part la lecture. */}
      <div className={styles.destination}>
        <button
          className={`${styles.btn} ${surRobot ? '' : styles.actif}`}
          onClick={() => onChangerDestination(false)}
          title="La lecture et le curseur ne se jouent que dans l'aperçu 3D"
        ><Icone nom="ecran" taille={15} />Simulation</button>
        <button
          className={`${styles.btn} ${surRobot ? styles.actif : ''}`}
          onClick={() => onChangerDestination(true)}
          disabled={!robotJoignable}
          title={robotJoignable ? 'La lecture et le curseur font bouger le robot' : 'Robot déconnecté'}
        ><Icone nom="robot" taille={15} />Robot</button>
      </div>
    </div>
  )
}
