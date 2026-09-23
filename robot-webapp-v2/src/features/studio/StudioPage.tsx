import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { sonApi } from '../../shared/api/sonApi'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import { BarreHumeurs } from './components/BarreHumeurs'
import { Bibliotheque } from './components/Bibliotheque'
import { BarreReglages } from './components/BarreReglages'
import { Frise } from './components/Frise'
import { Onde } from './components/Onde'
import { PanneauMorceau } from './components/PanneauMorceau'
import { couleurTimbre } from './dessin'
import { useLecture } from './lecture'
import { rendre } from './synthese/moteur'
import { REGLAGES_PAR_DEFAUT, TIMBRES, VERSION_SON, type Morceau, type Son, type Timbre } from './synthese/types'
import { enKo, enWav } from './synthese/wav'
import { sonVide, useStudioStore } from './store/studioStore'
import { bibliotheque, exporterWav } from './utils/bibliotheque'
import styles from './StudioPage.module.css'

/**
 * Le Studio son : on y dessine un son, on l'écoute, on le retouche.
 *
 * Tout se fait dans le navigateur, <b>robot éteint comme allumé</b> : c'est ici que le son existe,
 * et le robot n'en recevra qu'un fichier. La bibliothèque et l'envoi au robot viennent ensuite.
 */
export function StudioPage() {
  const son = useStudioStore((s) => s.son)
  const mode = useStudioStore((s) => s.mode)
  const timbreCrayon = useStudioStore((s) => s.timbreCrayon)
  const historique = useStudioStore((s) => s.historique)
  const refaits = useStudioStore((s) => s.refaits)
  const changerMode = useStudioStore((s) => s.changerMode)
  const changerTimbreCrayon = useStudioStore((s) => s.changerTimbreCrayon)
  const remplacer = useStudioStore((s) => s.remplacer)
  const renommer = useStudioStore((s) => s.renommer)
  const annuler = useStudioStore((s) => s.annuler)
  const refaire = useStudioStore((s) => s.refaire)
  const supprimerChoisi = useStudioStore((s) => s.supprimerChoisi)
  const choisir = useStudioStore((s) => s.choisir)

  const robotJoignable = useWebSocketStore((s) => s.connected)
  const lecture = useLecture()
  const [message, setMessage] = useState('')
  const [rafraichir, setRafraichir] = useState(0)
  // Le rendu retient POUR QUEL son il a été calculé : c'est ce qui dit, sans drapeau à tenir à
  // jour, que l'onde affichée est en retard sur la dernière retouche.
  const [rendu, setRendu] = useState<{ tampon: AudioBuffer | null; pour: Son } | null>(null)
  const tampon = rendu?.tampon ?? null
  const perime = rendu?.pour !== son

  // Le son se recalcule après chaque retouche, mais pas pendant : 90 ms d'attente suffisent à ne
  // rendre qu'une fois quand on tire un point sur la frise.
  useEffect(() => {
    const minuteur = setTimeout(() => {
      if (!son.morceaux.length) {
        setRendu({ tampon: null, pour: son })
        return
      }
      void rendre(son.morceaux, son.reglages).then((rendu) => setRendu({ tampon: rendu, pour: son }))
    }, 90)
    return () => clearTimeout(minuteur)
  }, [son])

  // Le robot revient : ce qui a été enregistré pendant son absence lui part d'un coup, puis la
  // bibliothèque est relue dans tous les cas — sans quoi elle resterait sur « robot injoignable »
  // quand il n'y avait rien en attente.
  useEffect(() => {
    if (!robotJoignable) return
    bibliotheque
      .envoyerEnAttente()
      .then(({ envoyes }) => {
        if (envoyes.length) setMessage(`${envoyes.length} son${envoyes.length > 1 ? 's' : ''} envoyé${envoyes.length > 1 ? 's' : ''} au robot`)
      })
      .catch(() => {
        /* robot pas encore prêt : la prochaine connexion réessaiera */
      })
      .finally(() => setRafraichir((n) => n + 1))
  }, [robotJoignable])

  const jouerTout = useCallback(async () => {
    if (lecture.enCours) {
      lecture.arreter()
      return
    }
    if (!son.morceaux.length) return
    const frais = await rendre(son.morceaux, son.reglages)
    setRendu({ tampon: frais, pour: son })
    lecture.jouer(frais)
  }, [lecture, son])

  /** Un morceau seul, joué à sa place dans le son : la tête de lecture tombe là où il commence. */
  const jouerMorceau = useCallback(
    async (m: Morceau) => {
      const seul = await rendre([{ ...m, debut: 0 }], son.reglages)
      lecture.jouer(seul, m.debut / son.reglages.debit)
    },
    [lecture, son.reglages],
  )

  // Le raccourci clavier passe par une référence : l'écouteur est posé une fois, et jouerTout
  // change à chaque retouche du son.
  const jouerToutRef = useRef(jouerTout)
  useEffect(() => {
    jouerToutRef.current = jouerTout
  }, [jouerTout])

  useEffect(() => {
    function auClavier(e: KeyboardEvent) {
      const cible = e.target as HTMLElement
      if (cible.tagName === 'INPUT' && (cible as HTMLInputElement).type !== 'range') return
      const touche = e.key.toLowerCase()
      if ((e.ctrlKey || e.metaKey) && touche === 'z') {
        e.preventDefault()
        if (e.shiftKey) refaire()
        else annuler()
      } else if ((e.ctrlKey || e.metaKey) && touche === 'y') {
        e.preventDefault()
        refaire()
      } else if (e.key === ' ') {
        e.preventDefault()
        void jouerToutRef.current()
      } else if (e.key === 'Delete' || e.key === 'Backspace') {
        e.preventDefault()
        supprimerChoisi()
      } else if (touche === 'c') changerMode('crayon')
      else if (touche === 'v') changerMode('main')
      else if (e.key === 'Escape') choisir(null)
    }
    window.addEventListener('keydown', auClavier)
    return () => window.removeEventListener('keydown', auClavier)
  }, [annuler, refaire, supprimerChoisi, changerMode, choisir])

  function charger(nom: string, morceaux: Morceau[]) {
    const nouveau: Son = { nom, version: VERSION_SON, reglages: REGLAGES_PAR_DEFAUT(), morceaux }
    remplacer(nouveau)
    ecouterCeSon(nouveau)
  }

  /** Rend un son entier puis le joue : ce que font le chargement d'un modèle et l'improvisation. */
  function ecouterCeSon(aJouer: Son) {
    void rendre(aJouer.morceaux, aJouer.reglages).then((frais) => {
      setRendu({ tampon: frais, pour: aJouer })
      lecture.jouer(frais)
    })
  }

  // Le WAV n'est refait que quand le tampon change : sans ce filet, il l'était à chaque image de
  // la lecture, soit une centaine de milliers d'octets réencodés soixante fois par seconde.
  async function enregistrer() {
    const resultat = await bibliotheque.enregistrer(son)
    setMessage(
      resultat.ou === 'robot'
        ? `« ${son.nom} » enregistré sur le robot`
        : `« ${son.nom} » gardé dans le navigateur : il partira au robot à sa reconnexion (${resultat.raison})`,
    )
    setRafraichir((n) => n + 1)
  }

  /** Écouter le son là où il sera joué : le haut-parleur du robot ne sonne pas comme un poste. */
  async function jouerSurLeRobot() {
    try {
      await sonApi.jouer(son.nom)
      setMessage(`« ${son.nom} » joué sur le robot`)
    } catch (e) {
      setMessage(`Le robot n'a pas joué « ${son.nom} » : ${e instanceof Error ? e.message : String(e)}. Enregistre-le d'abord.`)
    }
  }

  const octets = useMemo(() => (tampon ? enWav(tampon) : null), [tampon])

  return (
    <div className={styles.page}>
      <header className={styles.barre}>
        <span className={styles.marque}>Studio son</span>
        <input
          className={styles.nom}
          value={son.nom}
          aria-label="Nom du son"
          spellCheck={false}
          onChange={(e) => renommer(e.target.value)}
        />
        <button className={styles.bouton} onClick={() => void enregistrer()} disabled={!son.morceaux.length}>
          Enregistrer
        </button>
        <button className={styles.bouton} onClick={() => remplacer(sonVide())}>
          Nouveau
        </button>
        <div className={styles.espace} />
        <button className={styles.bouton} onClick={() => void exporterWav(son)} disabled={!son.morceaux.length}>
          Exporter le WAV
        </button>
        <button
          className={styles.bouton}
          onClick={() => void jouerSurLeRobot()}
          disabled={!robotJoignable || !son.morceaux.length}
          title={robotJoignable ? 'Écouter ce son sur le robot' : 'Robot injoignable'}
        >
          Jouer sur le robot
        </button>
        <button className={styles.jouer} onClick={() => void jouerTout()} title="Écouter (espace)">
          <Icone nom={lecture.enCours ? 'stop' : 'reprise'} taille={20} />
        </button>
      </header>

      <BarreHumeurs
        onCompose={(compose) => {
          remplacer(compose)
          ecouterCeSon(compose)
        }}
      />

      <div className={styles.corps}>
        <Bibliotheque
          nomCourant={son.nom}
          rafraichir={rafraichir}
          onCharger={(ouvert) => {
            remplacer(ouvert)
            ecouterCeSon(ouvert)
          }}
          onChargerModele={charger}
          onMessage={setMessage}
        />

        <section className={styles.centre}>
          <div className={styles.outils}>
            <button className={styles.outil} aria-pressed={mode === 'main'} title="Déplacer et modeler (V)" onClick={() => changerMode('main')}>
              <Icone nom="main" taille={20} />
            </button>
            <button
              className={styles.outil}
              aria-pressed={mode === 'crayon'}
              title="Dessiner un son à main levée (C)"
              onClick={() => changerMode('crayon')}
            >
              <Icone nom="crayon" taille={20} />
            </button>

            {mode === 'crayon' && (
              <div className={styles.crayonTimbres}>
                le crayon dessine :
                {(Object.keys(TIMBRES) as Timbre[]).map((timbre) => (
                  <button
                    key={timbre}
                    aria-pressed={timbre === timbreCrayon}
                    style={{ '--c': couleurTimbre(timbre) } as React.CSSProperties}
                    onClick={() => changerTimbreCrayon(timbre)}
                  >
                    {TIMBRES[timbre].nom}
                  </button>
                ))}
              </div>
            )}

            <span className={styles.separateur} />
            <button className={styles.outil} disabled={!historique.length} title="Annuler (Ctrl+Z)" onClick={annuler}>
              <Icone nom="annuler" taille={20} />
            </button>
            <button className={styles.outil} disabled={!refaits.length} title="Refaire (Ctrl+Maj+Z)" onClick={refaire}>
              <Icone nom="retablir" taille={20} />
            </button>

            <span className={styles.astuce}>
              {mode === 'crayon'
                ? 'Dessine de gauche à droite : plus haut = plus aigu'
                : 'Plus haut = plus aigu · de gauche à droite = le temps'}
            </span>
          </div>

          <BarreReglages onEcouter={() => void jouerTout()} />

          <Frise tete={lecture.tete} onEcouter={(m) => void jouerMorceau(m)} />

          <Onde rendu={tampon} perime={perime} tete={lecture.tete} />

          <p className={styles.infos}>
            {tampon && octets ? (
              <>
                Le robot recevra <b>{son.nom}.wav</b> · <b>{tampon.duration.toFixed(2).replace('.', ',')} s</b> · 44,1 kHz mono ·{' '}
                {enKo(octets)} Ko
                <span className={styles.compte}>
                  {son.morceaux.length} morceau{son.morceaux.length > 1 ? 'x' : ''}
                </span>
              </>
            ) : (
              'Aucun son.'
            )}
          </p>
          {message && <p className={styles.message}>{message}</p>}
        </section>

        <PanneauMorceau onEcouter={(m) => void jouerMorceau(m)} />
      </div>
    </div>
  )
}
