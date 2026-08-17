import { useEffect } from 'react'
import { Icone } from '../../shared/components/Icone'
import { urlVignette, type Personne } from '../../shared/api/personneApi'
import styles from './personnes.module.css'

/**
 * Le portrait de quelqu'un, en grand, posé par-dessus la fiche.
 *
 * Une surcouche du DOM et non une vraie boîte de dialogue : le flux vidéo et le
 * WebSocket continuent de tourner dans la coque, et `dialog` modal comme
 * `confirm()` bloqueraient la page.
 *
 * Trois façons d'en sortir — la croix, le fond, la touche Échap — parce que ça
 * s'ouvre d'un clic distrait et que rester coincé dessus serait ridicule.
 */
export function PortraitAgrandi({ personne, surFermeture }: { personne: Personne; surFermeture: () => void }) {
  useEffect(() => {
    const auClavier = (e: KeyboardEvent) => {
      if (e.key === 'Escape') surFermeture()
    }
    document.addEventListener('keydown', auClavier)
    return () => document.removeEventListener('keydown', auClavier)
  }, [surFermeture])

  return (
    <div
      className={styles.voile}
      role="dialog"
      aria-modal="true"
      aria-label={`Portrait de ${personne.prenom}`}
      // Le clic ne ferme que s'il tombe sur le fond : sans cette garde, relâcher
      // la souris hors de l'image après avoir cliqué dessus refermerait aussi.
      onClick={(e) => {
        if (e.target === e.currentTarget) surFermeture()
      }}
    >
      <figure className={styles.portrait}>
        <img src={urlVignette(personne)} alt={`Visage de ${personne.prenom}`} />
        <figcaption>{personne.prenom}</figcaption>
      </figure>

      <button className={styles.fermer} onClick={surFermeture} aria-label="Fermer">
        <Icone nom="croix" />
      </button>
    </div>
  )
}
