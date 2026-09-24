import { depuisSuiteBips, depuisSuiteVoix, MODELES_BIPS, MODELES_VOIX } from '../synthese/modeles'
import type { Morceau } from '../synthese/types'
import styles from './BarreHumeurs.module.css'

/**
 * Les modèles, à côté des humeurs et non dans la bibliothèque : ce sont des points de départ pour
 * créer, pas des sons rangés. Même forme de puces que les humeurs, puisqu'ils servent au même geste.
 */
export function BarreModeles({ onCharger }: { onCharger: (nom: string, morceaux: Morceau[]) => void }) {
  return (
    <div className={styles.barre}>
      <span className={styles.titre}>Modèles</span>
      <div className={styles.puces}>
        {MODELES_VOIX.map((modele) => (
          <button key={modele.nom} className={styles.puce} onClick={() => onCharger(modele.nom.toLowerCase(), depuisSuiteVoix(modele.suite))}>
            {modele.nom}
          </button>
        ))}
      </div>
      <span className={styles.separateur} />
      <div className={styles.puces}>
        {MODELES_BIPS.map((modele) => (
          <button
            key={modele.nom}
            className={`${styles.puce} ${styles.puceBip}`}
            onClick={() => onCharger(modele.nom.toLowerCase(), depuisSuiteBips(modele.suite ?? modele.tirage!()))}
          >
            {modele.nom}
          </button>
        ))}
      </div>
    </div>
  )
}
