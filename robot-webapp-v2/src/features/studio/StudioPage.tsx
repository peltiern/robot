import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { sonApi } from '../../shared/api/sonApi'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import { BarreHumeurs } from './components/BarreHumeurs'
import { BarreModeles } from './components/BarreModeles'
import { Bibliotheque } from './components/Bibliotheque'
import { BarreReglages } from './components/BarreReglages'
import { Frise } from './components/Frise'
import { Onde } from './components/Onde'
import { BarreMorceau } from './components/BarreMorceau'
import { couleurTimbre } from './dessin'
import { useLecture } from './lecture'
import { rendre, rendreSon } from './synthese/moteur'
import { aDuSon, REGLAGES_PAR_DEFAUT, TIMBRES, VERSION_SON, type Morceau, type Son, type Timbre } from './synthese/types'
import { enKo, enWav } from './synthese/wav'
import { sonVide, useStudioStore } from './store/studioStore'
import { bibliotheque, exporterWav } from './utils/bibliotheque'
import { importerFichier } from './utils/importer'
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
  const rogner = useStudioStore((s) => s.rogner)
  // Recalculé à chaque retouche : le bouton ne s'allume que s'il y a vraiment du silence à retirer.
  const silenceAvant = useStudioStore((s) => s.silenceAvant())

  const robotJoignable = useWebSocketStore((s) => s.connected)
  const lecture = useLecture()
  const [message, setMessage] = useState('')
  // Le son tel qu'il a été enregistré ou ouvert pour la dernière fois : la disquette s'allume tant
  // qu'il reste des retouches à perdre, comme dans l'Atelier.
  const [enregistre, setEnregistre] = useState<string | null>(null)
  // Mémorisé : la recette d'un son importé porte tout son enregistrement, des centaines de Ko à
  // resérialiser à chaque image de la lecture sinon.
  const modifie = useMemo(() => aDuSon(son) && JSON.stringify(son) !== enregistre, [son, enregistre])
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
      if (!aDuSon(son)) {
        setRendu({ tampon: null, pour: son })
        return
      }
      void rendreSon(son).then((rendu) => setRendu({ tampon: rendu, pour: son }))
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
    if (!aDuSon(son)) return
    const frais = await rendreSon(son)
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
    void rendreSon(aJouer).then((frais) => {
      setRendu({ tampon: frais, pour: aJouer })
      lecture.jouer(frais)
    })
  }

  // Le WAV n'est refait que quand le tampon change : sans ce filet, il l'était à chaque image de
  // la lecture, soit une centaine de milliers d'octets réencodés soixante fois par seconde.
  async function enregistrer() {
    const resultat = await bibliotheque.enregistrer(son)
    setEnregistre(JSON.stringify(son))
    setMessage(
      resultat.ou === 'robot'
        ? `« ${son.nom} » enregistré sur le robot`
        : `« ${son.nom} » gardé dans le navigateur : il partira au robot à sa reconnexion (${resultat.raison})`,
    )
    setRafraichir((n) => n + 1)
  }

  /**
   * Un son de la bibliothèque, sans l'ouvrir : sur le robot quand il l'a et qu'il répond — son
   * haut-parleur ne sonne pas comme un poste —, dans le navigateur sinon.
   */
  async function jouerRange(nom: string, surLeRobot: boolean) {
    try {
      if (surLeRobot) {
        await sonApi.jouer(nom)
        setMessage(`« ${nom} » joué sur le robot`)
        return
      }
      const range = await bibliotheque.charger(nom)
      lecture.jouer(await rendreSon(range))
    } catch (e) {
      setMessage(`« ${nom} » n'a pas été joué : ${e instanceof Error ? e.message : String(e)}`)
    }
  }

  /**
   * Les fichiers choisis, convertis, rangés comme n'importe quel son, et le dernier ouvert. Un nom
   * déjà pris reçoit un numéro : importer ne doit jamais écraser un son en silence.
   */
  async function importer(fichiers: File[]) {
    const liste = await bibliotheque.lister()
    const pris = new Set([...(liste.robot ?? []), ...liste.enAttente])
    let dernier: Son | null = null
    const refuses: string[] = []
    for (const fichier of fichiers) {
      try {
        const importe = await importerFichier(fichier, pris)
        pris.add(importe.nom)
        await bibliotheque.enregistrer(importe)
        dernier = importe
      } catch (e) {
        refuses.push(`${fichier.name} (${e instanceof Error ? e.message : String(e)})`)
      }
    }
    setRafraichir((n) => n + 1)
    if (dernier) {
      remplacer(dernier)
      setEnregistre(JSON.stringify(dernier))
      ecouterCeSon(dernier)
    }
    const importes = fichiers.length - refuses.length
    setMessage(
      [
        importes && `${importes} son${importes > 1 ? 's' : ''} importé${importes > 1 ? 's' : ''}`,
        refuses.length && `illisible${refuses.length > 1 ? 's' : ''} : ${refuses.join(', ')}`,
      ]
        .filter(Boolean)
        .join(' · '),
    )
  }

  const octets = useMemo(() => (tampon ? enWav(tampon) : null), [tampon])
  const fichier = son.fichier

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
        {/* À côté du nom, puisque c'est sous ce nom qu'il part. Allumée tant qu'il reste des
            retouches à perdre. */}
        <button
          className={`${styles.icone} ${modifie ? styles.iconeActive : ''}`}
          onClick={() => void enregistrer()}
          disabled={!aDuSon(son)}
          title={modifie ? 'Enregistrer — retouches non enregistrées' : 'Enregistrer'}
        >
          <Icone nom="disquette" taille={20} />
        </button>
        <div className={styles.espace} />
        <button className={styles.bouton} onClick={() => void exporterWav(son)} disabled={!aDuSon(son)}>
          Exporter le WAV
        </button>

        <button className={styles.jouer} onClick={() => void jouerTout()} title="Écouter (espace)">
          <Icone nom={lecture.enCours ? 'stop' : 'reprise'} taille={20} />
        </button>
      </header>


      <div className={styles.corps}>
        <Bibliotheque
          nomCourant={son.nom}
          rafraichir={rafraichir}
          robotJoignable={robotJoignable}
          onNouveau={() => remplacer(sonVide())}
          onImporter={(fichiers) => void importer(fichiers)}
          onCharger={(ouvert) => {
            remplacer(ouvert)
            setEnregistre(JSON.stringify(ouvert))
            ecouterCeSon(ouvert)
          }}
          onJouer={(nom, surLeRobot) => void jouerRange(nom, surLeRobot)}
          onMessage={setMessage}
        />

        <section className={styles.centre}>
          {/* Improviser et partir d'un modèle, au-dessus de la frise : c'est ici qu'on crée. */}
          <BarreHumeurs
            onCompose={(compose) => {
              remplacer(compose)
              ecouterCeSon(compose)
            }}
          />
          <BarreModeles onCharger={charger} />

          {fichier ? (
            <>
              {/* Un son importé n'a pas de morceaux à modeler : on l'écoute, on le renomme, on règle
                  son volume. Annuler reste, pour le volume. */}
              <div className={styles.outils}>
                <span className={styles.origine}>
                  {fichier.origine ? (
                    <>
                      Importé de <b>{fichier.origine}</b> — silences du début et de la fin retirés
                    </>
                  ) : (
                    'Déposé sur le robot sans recette : ouvert tel qu’il le joue'
                  )}
                </span>
                <span className={styles.separateur} />
                <button className={styles.outil} disabled={!historique.length} title="Annuler (Ctrl+Z)" onClick={annuler}>
                  <Icone nom="annuler" taille={20} />
                </button>
                <button className={styles.outil} disabled={!refaits.length} title="Refaire (Ctrl+Maj+Z)" onClick={refaire}>
                  <Icone nom="retablir" taille={20} />
                </button>
              </div>
              <BarreReglages onEcouter={() => void jouerTout()} seulement={['volume']} />
              <Onde
                rendu={tampon}
                perime={perime}
                tete={lecture.tete}
                fenetre={tampon ? Math.max(1, tampon.duration * 1.05) : 2}
                grande
              />
            </>
          ) : (
            <>
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

                <span className={styles.separateur} />
                <button
                  className={styles.outil}
                  disabled={silenceAvant < 0.001}
                  title={
                    silenceAvant < 0.001
                      ? 'Rien à rogner : le son commence au premier morceau'
                      : `Rogner : retirer ${silenceAvant.toFixed(2).replace('.', ',')} s de silence avant le premier son`
                  }
                  onClick={rogner}
                >
                  <Icone nom="ciseaux" taille={20} />
                </button>

              </div>

              <BarreReglages onEcouter={() => void jouerTout()} />

              <Frise tete={lecture.tete} onEcouter={(m) => void jouerMorceau(m)} />

              <BarreMorceau onEcouter={(m) => void jouerMorceau(m)} />

              <Onde rendu={tampon} perime={perime} tete={lecture.tete} />
            </>
          )}

          {/* Sous l'onde et non dans la barre d'état : c'est le son en cours qu'elle décrit. */}
          <p className={styles.infos}>
            {tampon && octets ? (
              <>
                Le robot recevra <b>{son.nom}.wav</b> · <b>{tampon.duration.toFixed(2).replace('.', ',')} s</b> · 44,1 kHz mono ·{' '}
                {enKo(octets)} Ko
              </>
            ) : (
              'Aucun son.'
            )}
            {!fichier && (
              <span className={styles.compte}>
                {son.morceaux.length} morceau{son.morceaux.length > 1 ? 'x' : ''}
              </span>
            )}
          </p>
        </section>
      </div>

      {/* La barre d'état court sous tout le Studio, bibliothèque comprise : elle dit ce qui vaut
          pour tout l'écran — le robot est-il là, et qu'a-t-il répondu —, comme celle de l'Atelier. */}
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
