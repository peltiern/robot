import { useRef, useState } from 'react'
import styles from './personnes.module.css'

interface Props {
  /** Libellé du bouton qui lance l'envoi. */
  action: string
  /** Champ prénom en plus des photos : à la création seulement. */
  avecPrenom?: boolean
  /** Rend vrai si le robot a accepté — c'est ce qui referme le formulaire. */
  surEnvoi: (prenom: string, photos: File[]) => Promise<boolean>
  surAnnulation: () => void
}

/**
 * Choisir des photos, et les envoyer au robot pour qu'il apprenne un visage.
 *
 * Plusieurs fichiers d'un coup, volontairement : une seule photo prise de trois
 * quarts ou à contre-jour, et la personne n'est plus jamais reconnue — c'est la
 * même raison qui fait relever cinq empreintes devant la caméra.
 */
export function ChoixDePhotos({ action, avecPrenom, surEnvoi, surAnnulation }: Props) {
  const [prenom, setPrenom] = useState('')
  const [photos, setPhotos] = useState<File[]>([])
  const [envoiEnCours, setEnvoiEnCours] = useState(false)
  const champ = useRef<HTMLInputElement>(null)

  const pretAEnvoyer = photos.length > 0 && (!avecPrenom || prenom.trim().length > 0)

  async function envoyer(e: React.FormEvent) {
    e.preventDefault()
    if (!pretAEnvoyer || envoiEnCours) return
    // La détection tourne sur le CPU du Jetson : quelques secondes pour cinq
    // photos. Sans ce verrou, un second clic lancerait un doublon.
    setEnvoiEnCours(true)
    const accepte = await surEnvoi(prenom.trim(), photos)
    setEnvoiEnCours(false)
    if (accepte) {
      setPrenom('')
      setPhotos([])
      if (champ.current) champ.current.value = ''
    }
  }

  return (
    <form className={styles.formulaire} onSubmit={(e) => void envoyer(e)}>
      {avecPrenom && (
        <input
          className={styles.champPrenom}
          value={prenom}
          onChange={(e) => setPrenom(e.target.value)}
          placeholder="Prénom"
          aria-label="Prénom"
        />
      )}

      <input
        ref={champ}
        type="file"
        accept="image/*"
        multiple
        aria-label="Photos"
        onChange={(e) => setPhotos(Array.from(e.target.files ?? []))}
      />

      <p className={styles.conseil}>
        {photos.length === 0
          ? 'Un seul visage par photo. Plusieurs photos valent mieux qu’une.'
          : `${photos.length} photo${photos.length > 1 ? 's' : ''} choisie${photos.length > 1 ? 's' : ''}.`}
      </p>

      <div className={styles.actions}>
        <button className={styles.action} type="submit" disabled={!pretAEnvoyer || envoiEnCours}>
          {envoiEnCours ? 'Le robot regarde…' : action}
        </button>
        <button className={styles.action} type="button" onClick={surAnnulation} disabled={envoiEnCours}>
          Annuler
        </button>
      </div>
    </form>
  )
}
