import { useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { usePersonnesStore } from '../../shared/stores/personnesStore'
import { ChoixDePhotos } from './ChoixDePhotos'
import { Vignette } from './Vignette'
import { PortraitAgrandi } from './PortraitAgrandi'
import { absence, dateCourte, dateLongue } from './dates'
import styles from './personnes.module.css'

/**
 * Tout ce que le robot garde d'une personne : son prénom, sa timeline
 * d'apparition, son fil de conversation — et de quoi en effacer une partie.
 *
 * Les deux gestes destructeurs s'arment en deux temps dans le bouton lui-même,
 * sans boîte de dialogue : `confirm()` bloque le navigateur et gèlerait le flux
 * vidéo qui continue de tourner dans la coque.
 *
 * Ce composant est monté avec une `key` portant l'identifiant de la personne :
 * changer de fiche le remonte, et rien de ce qu'il tient — prénom en cours
 * d'édition, bouton armé — ne peut passer d'une personne à l'autre.
 */
export function FichePersonne() {
  const personne = usePersonnesStore((s) => s.fiche)
  const erreur = usePersonnesStore((s) => s.erreur)
  const renommer = usePersonnesStore((s) => s.renommer)
  const supprimer = usePersonnesStore((s) => s.supprimer)
  const oublierLaConversation = usePersonnesStore((s) => s.oublierLaConversation)
  const ajouterDesVisages = usePersonnesStore((s) => s.ajouterDesVisages)

  const [prenomEnCours, setPrenomEnCours] = useState<string | null>(null)
  const [aConfirmer, setAConfirmer] = useState<'personne' | 'conversation' | null>(null)
  const [ajoutDePhotos, setAjoutDePhotos] = useState(false)
  const [portraitAgrandi, setPortraitAgrandi] = useState(false)

  if (erreur && !personne) return <p className={styles.erreur}>Fiche indisponible — {erreur}</p>
  if (!personne) return <p className={styles.attente}>Lecture de la fiche…</p>

  const rencontres = personne.rencontres ?? []
  const conversation = personne.conversation ?? []

  function validerLeRenommage() {
    const prenom = prenomEnCours?.trim()
    setPrenomEnCours(null)
    if (personne && prenom && prenom !== personne.prenom) void renommer(personne.id, prenom)
  }

  function agir(quoi: 'personne' | 'conversation') {
    if (aConfirmer !== quoi) {
      setAConfirmer(quoi)
      return
    }
    setAConfirmer(null)
    if (!personne) return
    if (quoi === 'personne') void supprimer(personne.id)
    else void oublierLaConversation(personne.id)
  }

  return (
    <article className={styles.fiche}>
      <header className={styles.ficheEntete}>
        <Vignette personne={personne} grande surClic={() => setPortraitAgrandi(true)} />
        {prenomEnCours === null ? (
          <button
            className={styles.prenomModifiable}
            onClick={() => setPrenomEnCours(personne.prenom)}
            title="Corriger le prénom"
          >
            <h2>{personne.prenom}</h2>
            <Icone nom="crayon" />
          </button>
        ) : (
          <form
            className={styles.renommage}
            onSubmit={(e) => {
              e.preventDefault()
              validerLeRenommage()
            }}
          >
            <input
              value={prenomEnCours}
              onChange={(e) => setPrenomEnCours(e.target.value)}
              onKeyDown={(e) => e.key === 'Escape' && setPrenomEnCours(null)}
              aria-label="Prénom"
            />
            <button type="submit">OK</button>
            <button type="button" onClick={() => setPrenomEnCours(null)}>
              Annuler
            </button>
          </form>
        )}

        <p className={styles.resume}>
          {personne.nombreDeVisages === 0
            ? 'aucune empreinte — le robot ne la reconnaîtra pas'
            : `${personne.nombreDeVisages} empreinte${personne.nombreDeVisages > 1 ? 's' : ''} de visage`}
          {' · '}
          {personne.nombreDeRencontres} rencontre{personne.nombreDeRencontres > 1 ? 's' : ''}
        </p>
        {erreur && <p className={styles.erreur}>{erreur}</p>}

        {ajoutDePhotos ? (
          <ChoixDePhotos
            action="Apprendre ces visages"
            surEnvoi={async (_, photos) => {
              const accepte = await ajouterDesVisages(personne.id, photos)
              if (accepte) setAjoutDePhotos(false)
              return accepte
            }}
            surAnnulation={() => setAjoutDePhotos(false)}
          />
        ) : (
          <button className={styles.lien} onClick={() => setAjoutDePhotos(true)}>
            {personne.nombreDeVisages === 0
              ? 'Apprendre son visage depuis des photos'
              : 'Ajouter des photos'}
          </button>
        )}
      </header>

      <div className={styles.colonnes}>
        <section className={styles.bloc}>
          <h3>APPARITIONS</h3>
          {rencontres.length === 0 ? (
            <p className={styles.attente}>Aucune rencontre enregistrée.</p>
          ) : (
            <ol className={styles.timeline}>
              {rencontres.map((rencontre, i) => (
                <li key={`${rencontre.instant}-${i}`} className={styles.jalon}>
                  <span
                    className={
                      rencontre.type === 'PREMIERE' ? styles.pucePremiere : styles.puceRetour
                    }
                    aria-hidden
                  />
                  <span className={styles.jalonTexte}>
                    <span className={styles.jalonDate} title={dateLongue(rencontre.instant)}>
                      {dateCourte(rencontre.instant)}
                    </span>
                    <span className={styles.jalonQuoi}>
                      {rencontre.type === 'PREMIERE'
                        ? 'première rencontre'
                        : (absence(rencontre.secondesDAbsence) ?? 'de retour')}
                    </span>
                  </span>
                </li>
              ))}
            </ol>
          )}
        </section>

        <section className={styles.bloc}>
          <h3>CONVERSATION</h3>
          {conversation.length === 0 ? (
            <p className={styles.attente}>Le robot n'a rien retenu de vos échanges.</p>
          ) : (
            <ul className={styles.fil}>
              {conversation.map((message, i) => (
                <li
                  key={i}
                  className={message.role === 'USER' ? styles.messagePersonne : styles.messageRobot}
                >
                  {message.texte}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <footer className={styles.actions}>
        <button
          className={aConfirmer === 'conversation' ? styles.actionArmee : styles.action}
          onClick={() => agir('conversation')}
          disabled={conversation.length === 0}
        >
          {aConfirmer === 'conversation' ? 'Confirmer l’oubli' : 'Oublier la conversation'}
        </button>
        <button
          className={aConfirmer === 'personne' ? styles.actionDangerArmee : styles.actionDanger}
          onClick={() => agir('personne')}
        >
          {aConfirmer === 'personne'
            ? `Supprimer ${personne.prenom} définitivement`
            : 'Supprimer cette personne'}
        </button>
      </footer>

      {portraitAgrandi && (
        <PortraitAgrandi personne={personne} surFermeture={() => setPortraitAgrandi(false)} />
      )}
    </article>
  )
}
