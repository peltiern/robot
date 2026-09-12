import { Icone } from '../../../shared/components/Icone'
import { useAnimationStore } from '../store/animationStore'
import styles from './PropertiesPanel.module.css'

export function PropertiesPanel() {
  const { tracks, selectedKf, moveKf, deleteKf, clearSel } = useAnimationStore()

  // Keyframe sélectionnée
  let selTrack = null, selKf = null
  if (selectedKf) {
    selTrack = tracks.find(t => t.id === selectedKf.trackId) ?? null
    selKf    = selTrack?.kfs.find(k => k.id === selectedKf.kfId) ?? null
  }

  return (
    <div className={styles.panel}>

      {/* Pas de réglage de vitesse ici : elles viennent du robot.
          Vitesse et accélération sont des propriétés du servo, lues dans robot.properties et
          servies par /api/axes-animables. Deux champs « Yeux » et « Cou » ne savaient de toute
          façon pas représenter les trois axes du cou, qui travaillent à 40, 10 et 100 °/s. */}

      {/* Keyframe sélectionnée */}
      <div className={`${styles.section} ${styles.flex1}`}>
        <div className={styles.sectionTitle}>Image-clé sélectionnée</div>
        {selTrack && selKf ? (
          <div className={styles.kfEditor}>
            <div className={styles.kfTrackName} style={{ color: selTrack.color }}>
              {selTrack.name}
            </div>
            <div className={styles.propRow}>
              <span className={styles.propLbl}>Temps</span>
              <input type="number" className={styles.propIn}
                value={selKf.t}
                min={0} max={useAnimationStore.getState().totalMs} step={50}
                onChange={e => moveKf(selTrack.id as any, selKf!.id, +e.target.value, selKf!.v)}
              />
              <span className={styles.propUnit}>ms</span>
            </div>
            <div className={styles.propRow}>
              <span className={styles.propLbl}>Valeur</span>
              <input type="number" className={styles.propIn}
                value={selKf.v.toFixed(1)}
                min={selTrack.min} max={selTrack.max} step={0.5}
                onChange={e => moveKf(selTrack.id as any, selKf!.id, selKf!.t, +e.target.value)}
              />
              <span className={styles.propUnit}>°</span>
            </div>
            <button
              className={styles.deleteBtn}
              onClick={() => { deleteKf(selTrack!.id as any, selKf!.id); clearSel() }}
            >
              <Icone nom="corbeille" taille={13} />Supprimer
            </button>
          </div>
        ) : (
          <div className={styles.noSel}>
            Cliquez sur une image-clé<br />pour l'éditer
          </div>
        )}
      </div>

    </div>
  )
}
