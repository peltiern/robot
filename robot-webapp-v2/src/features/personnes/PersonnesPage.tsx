import { useEffect, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { usePersonnesStore } from '../../shared/personnes/personnesStore'
import { FichePersonne } from './FichePersonne'
import { ChoixDePhotos } from './ChoixDePhotos'
import { Vignette } from './Vignette'
import { ilYA } from './dates'
import styles from './personnes.module.css'

/**
 * Le répertoire du robot : qui il connaît, depuis quand, et ce qu'ils se sont dit.
 *
 * Deux colonnes plutôt qu'une liste puis une page : sur la tablette, revenir en
 * arrière pour changer de personne est un geste de trop quand on compare deux
 * fiches ou qu'on fait le ménage dans une base pleine de doublons.
 */
export function PersonnesPage() {
  const liste = usePersonnesStore((s) => s.liste)
  const erreur = usePersonnesStore((s) => s.erreur)
  const selection = usePersonnesStore((s) => s.selection)
  const charger = usePersonnesStore((s) => s.charger)
  const choisir = usePersonnesStore((s) => s.choisir)
  const creer = usePersonnesStore((s) => s.creer)

  const [ajoutEnCours, setAjoutEnCours] = useState(false)

  useEffect(() => {
    void charger()
  }, [charger])

  return (
    <section className={styles.page}>
      <aside className={styles.colonne}>
        <header className={styles.entete}>
          <h1 className={styles.titre}>RÉPERTOIRE</h1>
          <button
            className={styles.rafraichir}
            onClick={() => setAjoutEnCours(true)}
            title="Ajouter quelqu'un depuis des photos"
          >
            <Icone nom="plus" />
          </button>
          <button className={styles.rafraichir} onClick={() => void charger()} title="Relire la liste">
            <Icone nom="recharger" />
          </button>
        </header>

        {erreur && <p className={styles.erreur}>{erreur}</p>}

        {liste === null && !erreur && <p className={styles.attente}>Lecture de la mémoire…</p>}

        {liste?.length === 0 && !ajoutEnCours && (
          <p className={styles.attente}>
            Le robot ne connaît encore personne. Présente-toi à lui : il demandera ton prénom et
            apprendra ton visage. Ou ajoute quelqu'un depuis des photos.
          </p>
        )}

        <ul className={styles.liste}>
          {liste?.map((personne) => (
            <li key={personne.id}>
              <button
                className={`${styles.carte} ${selection === personne.id ? styles.carteActive : ''}`}
                onClick={() => {
                  setAjoutEnCours(false)
                  void choisir(personne.id)
                }}
              >
                <Vignette personne={personne} />
                <span className={styles.identite}>
                  <span className={styles.prenom}>{personne.prenom}</span>
                  <span className={styles.detail}>
                    {ilYA(personne.derniereRencontre) ?? 'jamais rencontrée'}
                  </span>
                </span>
                {personne.nombreDeVisages === 0 && (
                  <span
                    className={styles.alerte}
                    title="Aucune empreinte : le robot ne la reconnaîtra pas"
                  >
                    <Icone nom="alerte" />
                  </span>
                )}
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <div className={styles.volet}>
        {ajoutEnCours ? (
          <section className={styles.fiche}>
            <h2 className={styles.titreAjout}>Quelqu'un de nouveau</h2>
            <p className={styles.resume}>
              Le robot cherchera un visage sur chaque photo, apprendra à le reconnaître et en
              gardera un portrait.
            </p>
            {erreur && <p className={styles.erreur}>{erreur}</p>}
            <ChoixDePhotos
              action="Faire connaissance"
              avecPrenom
              surEnvoi={async (prenom, photos) => {
                const accepte = await creer(prenom, photos)
                // Le formulaire ne se referme qu'en cas de succès : sur un refus,
                // le message s'affiche au-dessus des photos déjà choisies.
                if (accepte) setAjoutEnCours(false)
                return accepte
              }}
              surAnnulation={() => setAjoutEnCours(false)}
            />
          </section>
        ) : selection ? (
          /* `key` sur la sélection : changer de personne remonte la fiche, et
             remet donc à zéro ce qu'elle a de local — le prénom en cours d'édition
             et surtout le bouton de suppression armé. Sans cela, armer la
             suppression puis cliquer sur quelqu'un d'autre laisse le bouton prêt à
             partir sur la mauvaise personne, au clic suivant. */
          <FichePersonne key={selection} />
        ) : (
          <p className={styles.vide}>
            <Icone nom="visage" />
            Choisis quelqu'un dans la liste.
          </p>
        )}
      </div>
    </section>
  )
}
