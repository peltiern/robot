import { useState } from 'react'
import { composer, HUMEURS, type CleHumeur, type Longueur, type Style } from '../synthese/modeles'
import { useStudioStore } from '../store/studioStore'
import { VERSION_SON, type Son } from '../synthese/types'
import styles from './BarreHumeurs.module.css'

/**
 * Improviser plutôt que partir d'une page blanche.
 *
 * Chaque humeur suit des règles de prosodie que l'oreille reconnaît, et ce qui en sort est un
 * brouillon modifiable : la frise le reçoit comme si on l'avait dessiné.
 */
export function BarreHumeurs({ onCompose }: { onCompose: (son: Son) => void }) {
  const son = useStudioStore((s) => s.son)
  const [humeur, setHumeur] = useState<CleHumeur | null>(null)
  const [intensite, setIntensite] = useState(0.7)
  const [longueur, setLongueur] = useState<Longueur>('moyen')
  const [style, setStyle] = useState<Style>('voix')

  function improviser(cle: CleHumeur) {
    setHumeur(cle)
    const { morceaux, voix } = composer(cle, intensite, longueur, style)
    const nom =
      son.nom === 'nouveau son' || son.nom.startsWith('humeur ') ? `humeur ${HUMEURS[cle].libelle.toLowerCase()}` : son.nom
    onCompose({ nom, version: VERSION_SON, reglages: { ...son.reglages, voix }, morceaux })
  }

  return (
    <div className={styles.barre}>
      <span className={styles.titre}>Humeur</span>
      <div className={styles.puces}>
        {(Object.keys(HUMEURS) as CleHumeur[]).map((cle) => (
          <button key={cle} className={styles.puce} aria-pressed={humeur === cle} onClick={() => improviser(cle)}>
            <span className={styles.emoji}>{HUMEURS[cle].emoji}</span>
            {HUMEURS[cle].libelle}
          </button>
        ))}
      </div>

      <label className={styles.curseur}>
        un peu
        <input type="range" min={0} max={1} step={0.05} value={intensite} onChange={(e) => setIntensite(+e.target.value)} />
        très
      </label>

      <div className={styles.segmente}>
        {(['court', 'moyen', 'long'] as Longueur[]).map((l) => (
          <button key={l} aria-pressed={longueur === l} onClick={() => setLongueur(l)}>
            {l}
          </button>
        ))}
      </div>

      <div className={styles.segmente}>
        {(['voix', 'bips'] as Style[]).map((s) => (
          <button key={s} aria-pressed={style === s} onClick={() => setStyle(s)}>
            {s}
          </button>
        ))}
      </div>

      <button
        className={styles.encore}
        title="Retirer au sort la même humeur"
        disabled={!humeur}
        onClick={() => humeur && improviser(humeur)}
      >
        🎲 Encore
      </button>
    </div>
  )
}
