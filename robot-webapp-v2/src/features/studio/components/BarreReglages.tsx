import { useRef } from 'react'
import { useStudioStore } from '../store/studioStore'
import { ETENDUE_VOLUME_DB, type Reglages } from '../synthese/types'
import styles from './BarreReglages.module.css'

/**
 * Les réglages d'ensemble, nommés par leurs deux bouts plutôt que par leur grandeur : on choisit
 * entre « grave » et « aigu », pas une fréquence.
 *
 * Le volume porte en plus un chiffre, parce que c'est le seul dont on veuille savoir de combien on
 * a bougé. Il est gradué à l'oreille sur 30 dB — voir {@code gainDe}.
 */

const CURSEURS: { cle: keyof Reglages; gauche: string; droite: string; min: number; max: number; pas: number }[] = [
  { cle: 'hauteur', gauche: 'grave', droite: 'aigu', min: 0.6, max: 1.6, pas: 0.01 },
  { cle: 'debit', gauche: 'lent', droite: 'rapide', min: 0.5, max: 2, pas: 0.01 },
  { cle: 'voix', gauche: 'bip pur', droite: 'voix', min: 0, max: 1, pas: 0.01 },
  { cle: 'tremblement', gauche: 'stable', droite: 'tremblant', min: 0, max: 2.5, pas: 0.01 },
  { cle: 'volume', gauche: 'très doux', droite: 'plein', min: 0, max: 1, pas: 0.01 },
  { cle: 'presence', gauche: 'discret', droite: 'qui porte', min: 0, max: 1, pas: 0.01 },
]

const enDecibels = (niveau: number) => `${niveau >= 0.999 ? '0' : `−${Math.round((1 - niveau) * ETENDUE_VOLUME_DB)}`} dB`

/** `seulement` : les curseurs qui ont un sens pour ce son — un son importé n'a que son volume. */
export function BarreReglages({ onEcouter, seulement }: { onEcouter: () => void; seulement?: (keyof Reglages)[] }) {
  const reglages = useStudioStore((s) => s.son.reglages)
  const regler = useStudioStore((s) => s.reglerEnsemble)
  // Un seul point de retour par geste : sans ça, tirer un curseur en laisserait un par pixel.
  const enCoursRef = useRef<keyof Reglages | null>(null)

  return (
    <div className={styles.barre}>
      {CURSEURS.filter(({ cle }) => !seulement || seulement.includes(cle)).map(({ cle, gauche, droite, min, max, pas }) => (
        <label key={cle} className={styles.reglage}>
          {gauche}
          <input
            type="range"
            min={min}
            max={max}
            step={pas}
            value={reglages[cle]}
            onChange={(e) => {
              const premier = enCoursRef.current !== cle
              enCoursRef.current = cle
              regler(cle, +e.target.value, premier)
            }}
            onPointerUp={() => {
              enCoursRef.current = null
              onEcouter()
            }}
            onBlur={() => (enCoursRef.current = null)}
          />
          {droite}
          {cle === 'volume' && <b className={styles.valeur}>{enDecibels(reglages.volume)}</b>}
        </label>
      ))}
    </div>
  )
}
