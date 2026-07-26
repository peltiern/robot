import { useAnimationStore } from '../store/animationStore'
import { trackVal } from '../utils/catmullRom'
import styles from './PropertiesPanel.module.css'

export function PropertiesPanel() {
  const { tracks, playhead, selectedKf, globalVelocity, globalAcceleration,
          setGlobalParam, moveKf, deleteKf, clearSel } = useAnimationStore()

  // Positions interpolées pour l'aperçu SVG
  const pos = Object.fromEntries(
    tracks.map(tr => {
      const sorted = [...tr.kfs].sort((a, b) => a.t - b.t)
      return [tr.id, trackVal(sorted, playhead, tr.min, tr.max)]
    })
  )

  const oeilG = pos['OEIL_GAUCHE'] ?? 0
  const oeilD = pos['OEIL_DROIT']  ?? 0
  const couGD = pos['COU_GAUCHE_DROITE'] ?? 0
  const couHB = pos['COU_HAUT_BAS']      ?? 0

  // Calculs SVG — identiques au HTML original
  const headTx = -couGD * 0.6
  const headTy = -couHB * 0.4
  const pupLY  = 82 - oeilG * 1.2
  const pupRY  = 82 - oeilD * 1.2

  // Keyframe sélectionnée
  let selTrack = null, selKf = null
  if (selectedKf) {
    selTrack = tracks.find(t => t.id === selectedKf.trackId) ?? null
    selKf    = selTrack?.kfs.find(k => k.id === selectedKf.kfId) ?? null
  }

  return (
    <div className={styles.panel}>

      {/* Aperçu robot SVG */}
      <div className={styles.section}>
        <div className={styles.sectionTitle}>Aperçu Robot</div>
        <svg viewBox="0 0 196 196" xmlns="http://www.w3.org/2000/svg" className={styles.robotSvg}>
          {/* base */}
          <rect x="68" y="182" width="60" height="10" rx="5" fill="#1e2d3d"/>
          {/* cou */}
          <rect x="86" y="148" width="24" height="38" rx="4" fill="#1e2d3d"/>
          {/* tête — panoramique + inclinaison */}
          <g transform={`translate(${headTx}, ${headTy})`}>
            <rect x="28" y="34" width="140" height="116" rx="16" fill="#1a2a3d" stroke="#2a4a6a" strokeWidth="1.5"/>
            <circle cx="26"  cy="88" r="13" fill="#111e2d" stroke="#2a4a6a" strokeWidth="1"/>
            <circle cx="170" cy="88" r="13" fill="#111e2d" stroke="#2a4a6a" strokeWidth="1"/>
            {/* socket gauche */}
            <circle cx="74"  cy="82" r="27" fill="#0a1520" stroke="#2a4a6a" strokeWidth="1.5"/>
            {/* socket droit */}
            <circle cx="122" cy="82" r="27" fill="#0a1520" stroke="#2a4a6a" strokeWidth="1.5"/>
            {/* pupille gauche */}
            <circle cx="74" cy={pupLY} r="15" fill="#4fc3f7" opacity=".9"/>
            <circle cx="74" cy={pupLY} r="7"  fill="#1a6080"/>
            <circle cx="80" cy={pupLY - 6} r="4" fill="white" opacity=".55"/>
            {/* pupille droite */}
            <circle cx="122" cy={pupRY} r="15" fill="#4fc3f7" opacity=".9"/>
            <circle cx="122" cy={pupRY} r="7"  fill="#1a6080"/>
            <circle cx="128" cy={pupRY - 6} r="4" fill="white" opacity=".55"/>
            {/* bouche */}
            <rect x="52" y="126" width="92" height="11" rx="5" fill="#0a1e12"/>
            <rect x="55" y="128" width="86" height="7"  rx="3" fill="#3fb95077"/>
            {/* antenne */}
            <line x1="98" y1="34" x2="98" y2="16" stroke="#2a4a6a" strokeWidth="2.5"/>
            <circle cx="98" cy="11" r="6" fill="#58a6ff" opacity=".85"/>
          </g>
        </svg>
      </div>

      {/* Vitesse / Accel globales */}
      <div className={styles.section}>
        <div className={styles.sectionTitle}>Vitesse / Accélération</div>
        <div className={styles.speedGrid}>
          {([['eyes', 'Yeux'], ['neck', 'Cou']] as const).map(([g, label]) => (
            <div key={g} className={styles.speedGroup}>
              <div className={styles.speedLabel}>{label}</div>
              <div className={styles.speedRow}>
                <label>Vit.</label>
                <input type="number" className={styles.numInput}
                  value={globalVelocity[g]}
                  min={10} max={1000}
                  onChange={e => setGlobalParam(g, 'velocity', +e.target.value)} />
              </div>
              <div className={styles.speedRow}>
                <label>Acc.</label>
                <input type="number" className={styles.numInput}
                  value={globalAcceleration[g]}
                  min={10} max={1000}
                  onChange={e => setGlobalParam(g, 'acceleration', +e.target.value)} />
              </div>
            </div>
          ))}
        </div>
      </div>

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
              🗑 Supprimer
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
