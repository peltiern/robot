import { urlVignette } from '../../shared/api/personneApi'
import { usePersonnesStore } from '../../shared/personnes/personnesStore'
import styles from './hud.module.css'

interface Props {
  /** Absent quand le robot ne savait pas à qui il parlait. */
  idPersonne?: string
  /** Prénom tel que le robot l'a envoyé, qui sert de repli si le répertoire n'a rien. */
  prenom?: string
}

/**
 * Le portrait de celui qui a prononcé une bulle.
 *
 * Trois états, et les trois arrivent : le portrait quand le robot a reconnu quelqu'un dont il
 * garde une image, son initiale quand il le connaît sans image (les gens appris avant qu'on ne
 * garde des portraits), et un point d'interrogation quand il ne savait pas à qui il parlait. Ce
 * dernier n'est pas un défaut d'affichage : il dit que le robot a entendu sans voir, et c'est
 * exactement ce qu'on veut lire.
 */
export function PortraitBulle({ idPersonne, prenom }: Props) {
  const personne = usePersonnesStore(
    (s) => (idPersonne ? (s.liste?.find((p) => p.id === idPersonne) ?? null) : null),
  )

  if (personne?.aUneVignette) {
    return (
      <img
        className={styles.portrait}
        src={urlVignette(personne)}
        alt={`Visage de ${personne.prenom}`}
        title={personne.prenom}
        // Une image illisible laisserait un cadre brisé au milieu du fil : on l'efface, la
        // place reste tenue et la mise en page ne bouge pas.
        onError={(e) => (e.currentTarget.style.visibility = 'hidden')}
      />
    )
  }

  const nom = personne?.prenom ?? prenom
  if (nom) {
    return (
      <span className={`${styles.portrait} ${styles.portraitInitiale}`} title={nom}>
        {nom.trim().charAt(0).toUpperCase() || '?'}
      </span>
    )
  }

  return (
    <span
      className={`${styles.portrait} ${styles.portraitInconnu}`}
      title="Le robot ne savait pas à qui il parlait"
    >
      ?
    </span>
  )
}
