import { useEffect, useState } from 'react'
import { Icone } from '../../../shared/components/Icone'
import { depuisSuiteBips, depuisSuiteVoix, MODELES_BIPS, MODELES_VOIX } from '../synthese/modeles'
import type { Morceau, Son } from '../synthese/types'
import { bibliotheque, type Liste } from '../utils/bibliotheque'
import styles from './Bibliotheque.module.css'

/**
 * Les sons du robot, ceux qui attendent de lui être envoyés, et les modèles pour démarrer.
 *
 * La pastille orange dit qu'un son n'est encore que dans le navigateur. Elle disparaît d'elle-même
 * au retour du robot, quand la file part.
 */
export function Bibliotheque({
  nomCourant,
  rafraichir,
  onCharger,
  onChargerModele,
  onMessage,
}: {
  nomCourant: string
  /** Change de valeur pour relire la bibliothèque : après un enregistrement, ou au retour du robot. */
  rafraichir: number
  onCharger: (son: Son) => void
  onChargerModele: (nom: string, morceaux: Morceau[]) => void
  onMessage: (texte: string) => void
}) {
  const [liste, setListe] = useState<Liste>({ robot: null, enAttente: [] })

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
      <h2>Mes sons</h2>
      {noms.length === 0 ? (
        <p className={styles.vide}>
          {liste.robot === null
            ? 'Robot injoignable. Les sons enregistrés ici l’attendront.'
            : 'Rien d’enregistré pour l’instant.'}
        </p>
      ) : (
        <ul className={styles.liste}>
          {noms.map((nom) => (
            <li key={nom} className={nom === nomCourant ? styles.courant : undefined}>
              <button className={styles.entree} onClick={() => void ouvrir(nom)}>
                {liste.enAttente.includes(nom) && (
                  <span className={styles.attente} title="Dans le navigateur : partira au robot à sa reconnexion" />
                )}
                {nom}
              </button>
              <button className={styles.supprimer} title={`Supprimer « ${nom} »`} onClick={() => void supprimer(nom)}>
                <Icone nom="corbeille" taille={16} />
              </button>
            </li>
          ))}
        </ul>
      )}

      <h2>Modèles · paroles</h2>
      <ul className={styles.liste}>
        {MODELES_VOIX.map((modele) => (
          <li key={modele.nom}>
            <button className={styles.entree} onClick={() => onChargerModele(modele.nom.toLowerCase(), depuisSuiteVoix(modele.suite))}>
              {modele.nom}
            </button>
          </li>
        ))}
      </ul>

      <h2>Modèles · bips</h2>
      <ul className={styles.liste}>
        {MODELES_BIPS.map((modele) => (
          <li key={modele.nom}>
            <button
              className={styles.entree}
              onClick={() => onChargerModele(modele.nom.toLowerCase(), depuisSuiteBips(modele.suite ?? modele.tirage!()))}
            >
              {modele.nom}
            </button>
          </li>
        ))}
      </ul>
    </aside>
  )
}
