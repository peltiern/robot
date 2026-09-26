import { useEffect, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { voixApi, type EtatVoix, type ReglagesVoix, type Voix } from '../../shared/api/voixApi'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import styles from './VoixPage.module.css'

/**
 * La voix du robot : on la règle, on l'écoute <b>sur le robot</b>, et on n'adopte que ce qui plaît.
 *
 * Contrairement au Studio, rien ne s'entend dans le navigateur : c'est Piper, sur le robot, qui
 * parle, et son haut-parleur fait partie de la voix. D'où une page qui ne sert à rien robot éteint,
 * et qui le dit.
 */

interface Curseur {
  cle: keyof ReglagesVoix
  gauche: string
  droite: string
  /** Le curseur va de 0 à 1 ; ces deux fonctions font le lien avec l'unité du robot. */
  versCurseur: (valeur: number) => number
  depuisCurseur: (position: number) => number
  lire: (valeur: number) => string
}

const lineaire = (min: number, max: number, pas: number) => ({
  versCurseur: (v: number) => (v - min) / (max - min),
  depuisCurseur: (p: number) => Math.round((min + p * (max - min)) / pas) * pas,
})

const virgule = (v: number, decimales: number) => v.toFixed(decimales).replace('.', ',').replace('-', '−')

// Les aigus se règlent à l'oreille, donc en rapport : entre 20 kHz et 800 Hz, un curseur linéaire
// tasserait dans son dernier dixième tout ce qui s'entend.
const AIGUS_MAX = 20000
const AIGUS_MIN = 800

const CURSEURS: Curseur[] = [
  { cle: 'hauteur', gauche: 'grave', droite: 'aiguë', ...lineaire(-12, 12, 0.5), lire: (v) => `${virgule(v, 1)} demi-ton${Math.abs(v) > 1 ? 's' : ''}` },
  { cle: 'debit', gauche: 'lente', droite: 'rapide', ...lineaire(0.5, 2, 0.05), lire: (v) => `× ${virgule(v, 2)}` },
  { cle: 'passeHaut', gauche: 'graves gardés', droite: 'graves coupés', ...lineaire(0, 1500, 10), lire: (v) => (v < 1 ? 'rien' : `sous ${Math.round(v)} Hz`) },
  {
    cle: 'passeBas',
    gauche: 'aigus gardés',
    droite: 'aigus coupés',
    versCurseur: (v) => Math.log(AIGUS_MAX / v) / Math.log(AIGUS_MAX / AIGUS_MIN),
    depuisCurseur: (p) => Math.round((AIGUS_MAX * Math.pow(AIGUS_MIN / AIGUS_MAX, p)) / 50) * 50,
    lire: (v) => (v >= AIGUS_MAX ? 'rien' : `au-dessus de ${Math.round(v)} Hz`),
  },
  { cle: 'grain', gauche: 'propre', droite: 'grésillante', ...lineaire(0, 40, 1), lire: (v) => (v < 0.1 ? 'aucun' : `${Math.round(v)} dB`) },
  { cle: 'machine', gauche: 'humaine', droite: 'machine', ...lineaire(0, 1, 0.01), lire: (v) => `${Math.round(v * 100)} %` },
  { cle: 'metal', gauche: 'libre', droite: 'en boîte', ...lineaire(0, 1, 0.01), lire: (v) => `${Math.round(v * 100)} %` },
  { cle: 'modulation', gauche: 'fluide', droite: 'hachée', ...lineaire(0, 200, 1), lire: (v) => (v < 0.5 ? 'aucune' : `${Math.round(v)} Hz`) },
]

const PHRASE = 'Bonjour ! Je suis Wall-E. Tu veux jouer avec moi ?'

const memesReglages = (a: ReglagesVoix, b: ReglagesVoix) => CURSEURS.every(({ cle }) => Math.abs(a[cle] - b[cle]) < 1e-6)

/** Le modèle nommé : `null` désigne celui de robot.properties, et c'est le même que son nom écrit. */
const cleModele = (voix: Voix, parDefaut: string) => `${voix.modele ?? parDefaut}|${voix.locuteur ?? ''}`

const memes = (a: Voix, b: Voix, parDefaut: string) =>
  cleModele(a, parDefaut) === cleModele(b, parDefaut) && memesReglages(a.reglages, b.reglages)

export function VoixPage() {
  const robotJoignable = useWebSocketStore((s) => s.connected)
  const [etat, setEtat] = useState<EtatVoix | null>(null)
  const [voix, setVoix] = useState<Voix | null>(null)
  const reglages = voix?.reglages ?? null
  const [phrase, setPhrase] = useState(PHRASE)
  const [message, setMessage] = useState('')

  // Relue au retour du robot : c'est lui qui garde la voix adoptée.
  useEffect(() => {
    if (!robotJoignable) return
    voixApi
      .etat()
      .then((lu) => {
        setEtat(lu)
        setVoix((courante) => courante ?? lu.adoptee)
      })
      .catch((e) => setMessage(`Voix illisible : ${e instanceof Error ? e.message : String(e)}`))
  }, [robotJoignable])

  async function ecouter() {
    if (!voix || !phrase.trim()) return
    try {
      await voixApi.essayer(phrase, voix)
      setMessage('Le robot parle avec cette voix…')
    } catch (e) {
      setMessage(`Essai impossible : ${e instanceof Error ? e.message : String(e)}`)
    }
  }

  async function adopter() {
    if (!voix || !etat) return
    try {
      const gardee = await voixApi.adopter(voix)
      setEtat({ ...etat, adoptee: gardee })
      setVoix(gardee)
      setMessage('Voix adoptée : le robot parle ainsi dès sa prochaine phrase')
    } catch (e) {
      setMessage(`Voix non adoptée : ${e instanceof Error ? e.message : String(e)}`)
    }
  }

  const modifiee = !!etat && !!voix && !memes(voix, etat.adoptee, etat.modeleParDefaut)
  const origine: Voix | null = etat && { modele: null, locuteur: null, reglages: etat.origine }

  return (
    <div className={styles.page}>
      <header className={styles.barre}>
        <span className={styles.marque}>Voix du robot</span>
        <div className={styles.espace} />
        <button className={styles.bouton} disabled={!modifiee} onClick={() => etat && setVoix(etat.adoptee)} title="Revenir à la voix avec laquelle le robot parle">
          Revenir à la voix adoptée
        </button>
        <button
          className={styles.bouton}
          disabled={!etat || !voix || !origine || memes(voix, origine, etat.modeleParDefaut)}
          onClick={() => origine && setVoix(origine)}
          title="Le modèle par défaut et la coloration Wall-E validée le 16 août 2026 — à écouter, puis à adopter si on y revient"
        >
          Voix d’origine
        </button>
        <button className={`${styles.bouton} ${modifiee ? styles.boutonActif : ''}`} disabled={!modifiee} onClick={() => void adopter()}>
          Adopter cette voix
        </button>
      </header>

      <div className={styles.centre}>
        {!robotJoignable ? (
          <p className={styles.avis}>Robot injoignable : c’est lui qui parle, la voix ne se règle qu’avec lui.</p>
        ) : !voix || !reglages || !etat ? (
          <p className={styles.avis}>Lecture de la voix…</p>
        ) : (
          <>
            {etat && !etat.reglable && (
              <p className={styles.avis}>Le robot parle avec Google : ces réglages ne valent que pour la voix Piper.</p>
            )}

            {/* La voix de base d'abord : c'est elle qui fait le timbre, les curseurs ne font que le
                colorer. */}
            <label className={styles.modele}>
              Voix de base
              <select
                value={cleModele(voix, etat.modeleParDefaut)}
                onChange={(e) => {
                  const choisi = etat.modeles.find((m) => `${m.fichier}|${m.locuteur ?? ''}` === e.target.value)
                  if (!choisi) return
                  setVoix({ ...voix, modele: choisi.fichier, locuteur: choisi.locuteur })
                  setMessage('Nouvelle voix de base : le premier essai la charge sur le robot, une à deux secondes')
                }}
              >
                {etat.modeles.map((m) => (
                  <option key={`${m.fichier}|${m.locuteur ?? ''}`} value={`${m.fichier}|${m.locuteur ?? ''}`}>
                    {m.nom}
                    {m.fichier === etat.modeleParDefaut ? ' (par défaut)' : ''}
                  </option>
                ))}
              </select>
            </label>

            <div className={styles.essai}>
              <input
                className={styles.phrase}
                value={phrase}
                onChange={(e) => setPhrase(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && void ecouter()}
                aria-label="Phrase d'essai"
              />
              <button className={styles.jouer} onClick={() => void ecouter()} disabled={!phrase.trim()} title="Faire dire la phrase au robot, avec ces réglages (Entrée) — sans effet s’il parle déjà">
                <Icone nom="reprise" taille={20} />
              </button>
            </div>

            <div className={styles.curseurs}>
              {CURSEURS.map(({ cle, gauche, droite, versCurseur, depuisCurseur, lire }) => (
                <label key={cle} className={styles.reglage}>
                  <span className={styles.bout}>{gauche}</span>
                  <input
                    type="range"
                    min={0}
                    max={1}
                    step={0.001}
                    value={versCurseur(reglages[cle])}
                    onChange={(e) => setVoix({ ...voix, reglages: { ...reglages, [cle]: depuisCurseur(+e.target.value) } })}
                  />
                  <span className={styles.bout}>{droite}</span>
                  <b className={styles.valeur}>{lire(reglages[cle])}</b>
                </label>
              ))}
            </div>
          </>
        )}
      </div>

      <footer className={styles.pied}>
        <span className={robotJoignable ? styles.pointOk : styles.pointKo} />
        <span>{robotJoignable ? 'Robot connecté' : 'Robot injoignable'}</span>
        {message && (
          <span className={styles.message} title={message}>
            {message}
          </span>
        )}
      </footer>
    </div>
  )
}
