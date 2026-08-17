import { urlVignette, type Personne } from '../../shared/api/personneApi'
import styles from './personnes.module.css'

interface Props {
  personne: Personne
  grande?: boolean
  /** Rend le portrait cliquable, pour l'agrandir. Sans effet faute d'image. */
  surClic?: () => void
}

/**
 * Le portrait de quelqu'un, ou son initiale à défaut.
 *
 * Il manque dans deux cas bien réels : les gens connus avant que le robot ne
 * garde des portraits, et ceux dont le visage touchait le bord de l'image au
 * moment de l'enrôlement. L'initiale vaut mieux qu'un rond vide — elle distingue
 * les fiches d'un coup d'œil, ce qu'une silhouette générique ne ferait pas.
 */
export function Vignette({ personne, grande, surClic }: Props) {
  const classe = grande ? styles.vignetteGrande : styles.vignette

  if (!personne.aUneVignette) {
    return (
      <span className={classe} aria-hidden>
        {personne.prenom.trim().charAt(0).toUpperCase() || '?'}
      </span>
    )
  }

  const image = (
    <img
      className={classe}
      src={urlVignette(personne)}
      alt={`Visage de ${personne.prenom}`}
      // Une image absente ou illisible ne doit pas laisser un cadre brisé au
      // milieu de la liste : on efface, l'initiale n'apparaîtra pas mais la
      // mise en page tient.
      onError={(e) => (e.currentTarget.style.visibility = 'hidden')}
    />
  )

  if (!surClic) return image

  return (
    <button
      className={styles.portraitCliquable}
      onClick={surClic}
      title={`Agrandir le portrait de ${personne.prenom}`}
    >
      {image}
    </button>
  )
}
