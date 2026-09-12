import { useMemo, useState } from 'react'
import { Icone } from '../../../shared/components/Icone'
import { useAnimationStore } from '../store/animationStore'
import { explication } from '../utils/corrections'
import { verifier, type Avertissement } from '../utils/verificateur'
import styles from './PanneauAvertissements.module.css'

/**
 * Ce que le robot ne saura pas suivre, recalculé à chaque modification, avec de quoi le corriger.
 *
 * Les avertissements n'empêchent ni d'enregistrer ni de jouer — le mouvement sera simplement en
 * retard sur la courbe — mais sans eux l'écart entre ce que la timeline montre et ce que la tête
 * fait reste inexplicable.
 */
export function PanneauAvertissements() {
  const tracks = useAnimationStore(s => s.tracks)
  const playing = useAnimationStore(s => s.playing)
  const setPlayhead = useAnimationStore(s => s.setPlayhead)
  const corrigerAvertissement = useAnimationStore(s => s.corrigerAvertissement)
  const toutCorriger = useAnimationStore(s => s.toutCorriger)
  const avertissements = useMemo(() => verifier(tracks), [tracks])
  const [replie, setReplie] = useState(false)

  if (avertissements.length === 0) return null

  return (
    <div className={styles.panneau}>
      <div className={styles.entete}>
        <Icone nom="alerte" taille={15} />
        <strong>Le robot ne suivra pas exactement ({avertissements.length})</strong>
        <div className={styles.actions}>
          <button className={styles.bouton} onClick={toutCorriger} title="Applique toutes les corrections ; Ctrl+Z les annule d'un coup">
            Tout corriger
          </button>
          <button className={styles.bouton} onClick={() => setReplie(r => !r)}>{replie ? 'Afficher' : 'Masquer'}</button>
        </div>
      </div>
      {!replie && (
        <ul className={styles.liste}>
          {avertissements.map(a => (
            <li key={cle(a)} className={styles.ligne}>
              {/* En lecture, le curseur appartient à la lecture : le déplacer la ferait sauter. */}
              <button className={styles.lien} onClick={() => { if (!playing) setPlayhead(a.instantDebut) }} title="Placer le curseur ici">
                {a.libelle} ({a.instantDebut === a.instantFin ? `${a.instantDebut} ms` : `${a.instantDebut} à ${a.instantFin} ms`}) : {a.message}
              </button>
              <button className={styles.bouton} onClick={() => corrigerAvertissement(a)} title={explication(a)}>Corriger</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function cle(a: Avertissement) {
  return `${a.nature}-${a.axe}-${a.instantDebut}-${a.idImageCle ?? ''}`
}
