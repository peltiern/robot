import { useEffect, useRef, useState } from 'react'
import { Icone } from '../../../shared/components/Icone'
import type { Son } from '../synthese/types'
import { bibliotheque, type Liste } from '../utils/bibliotheque'
import { AideGestes } from './AideGestes'
import styles from './Bibliotheque.module.css'

/**
 * Les sons rangés : ceux du robot, et ceux qui attendent de lui être envoyés.
 *
 * La pastille orange dit qu'un son n'est encore que dans le navigateur ; elle disparaît d'elle-même
 * au retour du robot, quand la file part. Chaque ligne se supprime et se joue sans être ouverte —
 * sur le robot quand il répond, dans le navigateur sinon.
 */
export function Bibliotheque({
  nomCourant,
  rafraichir,
  robotJoignable,
  onNouveau,
  onImporter,
  onCharger,
  onJouer,
  onMessage,
}: {
  nomCourant: string
  /** Change de valeur pour relire la bibliothèque : après un enregistrement, ou au retour du robot. */
  rafraichir: number
  robotJoignable: boolean
  onNouveau: () => void
  onImporter: (fichiers: File[]) => void
  onCharger: (son: Son) => void
  /** `surLeRobot` : le robot l'a et répond ; sinon le son se joue dans le navigateur. */
  onJouer: (nom: string, surLeRobot: boolean) => void
  onMessage: (texte: string) => void
}) {
  const [liste, setListe] = useState<Liste>({ robot: null, enAttente: [] })
  const choixRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    let vivant = true
    void bibliotheque.lister().then((l) => vivant && setListe(l))
    return () => {
      vivant = false
    }
  }, [rafraichir])

  // Un son en attente d'envoi porte le même nom que celui du robot : il ne paraît qu'une fois.
  const noms = [...new Set([...(liste.robot ?? []), ...liste.enAttente])].sort((a, b) =>
    a.toLowerCase().localeCompare(b.toLowerCase(), 'fr'),
  )

  async function ouvrir(nom: string) {
    try {
      onCharger(await bibliotheque.charger(nom))
    } catch (e) {
      onMessage(`« ${nom} » illisible : ${e instanceof Error ? e.message : String(e)}`)
    }
  }

  async function supprimer(nom: string) {
    try {
      await bibliotheque.supprimer(nom)
      onMessage(`« ${nom} » supprimé`)
    } catch (e) {
      onMessage(`« ${nom} » non supprimé : ${e instanceof Error ? e.message : String(e)}`)
    }
    setListe(await bibliotheque.lister())
  }

  return (
    <aside className={styles.colonne}>
      {/* Le « + » dans l'en-tête de la liste, comme dans la bibliothèque de l'Atelier. */}
      <div className={styles.entete}>
        <h2>Mes sons</h2>
        <button className={styles.action} title="Nouveau son" onClick={onNouveau}>
          <Icone nom="plus" taille={16} />
        </button>
        <button className={styles.action} title="Importer des fichiers son (WAV, MP3, OGG…)" onClick={() => choixRef.current?.click()}>
          <Icone nom="importer" taille={16} />
        </button>
        <input
          ref={choixRef}
          type="file"
          accept="audio/*,.wav,.mp3,.ogg"
          multiple
          hidden
          onChange={(e) => {
            const fichiers = [...(e.target.files ?? [])]
            // Vidé aussitôt : sans ça, choisir deux fois le même fichier ne déclencherait rien.
            e.target.value = ''
            if (fichiers.length) onImporter(fichiers)
          }}
        />
      </div>
      {noms.length === 0 ? (
        <p className={styles.vide}>
          {liste.robot === null
            ? 'Robot injoignable. Les sons enregistrés ici l’attendront.'
            : 'Rien d’enregistré pour l’instant.'}
        </p>
      ) : (
        <ul className={styles.liste}>
          {noms.map((nom) => {
            const enAttente = liste.enAttente.includes(nom)
            // Un son qui n'est que dans le navigateur, le robot ne peut pas le jouer.
            const surLeRobot = robotJoignable && !enAttente
            return (
              <li key={nom} className={`${styles.ligne} ${nom === nomCourant ? styles.courant : ''}`} onClick={() => void ouvrir(nom)} title={nom}>
                <span className={styles.nom}>{nom}</span>
                {enAttente && <span className={styles.attente} title="Dans le navigateur : partira au robot à sa reconnexion" />}
                <div className={styles.actions}>
                  <button
                    className={styles.action}
                    title={surLeRobot ? 'Jouer sur le robot' : 'Jouer dans le navigateur'}
                    onClick={(e) => {
                      e.stopPropagation()
                      onJouer(nom, surLeRobot)
                    }}
                  >
                    <Icone nom="reprise" taille={13} />
                  </button>
                  <button
                    className={`${styles.action} ${styles.supprimer}`}
                    title={`Supprimer « ${nom} »`}
                    onClick={(e) => {
                      e.stopPropagation()
                      void supprimer(nom)
                    }}
                  >
                    <Icone nom="corbeille" taille={13} />
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <AideGestes />
    </aside>
  )
}
