import { Icone } from '../../shared/components/Icone'
import { useTelemetryStore } from '../../shared/telemetry/telemetryStore'
import { useOrganesStore, capteurs, type Mesure } from '../../shared/organes/organesStore'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { couleurSeuil, fractionMesure } from '../../shared/hud/seuils'
import { PastillesSante } from './PastillesSante'
import styles from './hud.module.css'

/**
 * Jauges des capteurs du robot, précédées des pastilles d'état des organes.
 *
 * Rien n'est codé en dur : la liste vient de la découverte de capacités
 * (`GET /api/organes`), la valeur et l'historique du store de télémétrie. Un
 * capteur ajouté côté robot apparaît ici sans une ligne de front à écrire.
 *
 * Les pastilles d'abord, les jauges ensuite : « est-ce que ça vit ? » précède
 * « combien ça consomme ? », et c'est la question qu'on se pose devant un robot
 * qui ne répond plus.
 */
export function PanneauVitaux() {
  const connecte = useWebSocketStore((s) => s.connected)
  const organes = useOrganesStore((s) => s.organes)
  const etat = useOrganesStore((s) => s.etat)
  const charger = useOrganesStore((s) => s.charger)
  const valeurs = useTelemetryStore((s) => s.valeurs)
  const historique = useTelemetryStore((s) => s.historique)

  const mesures = capteurs(organes).flatMap((organe) => organe.mesures)
  // L'uptime n'est pas déclaré comme mesure par le robot, justement pour qu'il n'atterrisse pas
  // dans cette grille : une valeur qui ne fait que croître n'a pas d'échelle, donc pas d'anneau.
  // Il arrive avec la télémétrie et se lit en toutes lettres, sous les jauges.
  const uptime = valeurs['uptimeSecondes']

  return (
    <section className={styles.bloc}>
      <header className={styles.blocEntete}>
        <Icone nom="jauge" taille={18} />
        <span className="eyebrow">Vitaux</span>
      </header>

      {connecte && <PastillesSante />}

      {!connecte && <p className={styles.messageBloc}>En attente du robot.</p>}

      {connecte && etat === 'chargement' && (
        <p className={styles.messageBloc}>Lecture des capteurs…</p>
      )}

      {connecte && etat === 'erreur' && (
        <p className={styles.messageBloc}>
          Capteurs illisibles.
          <br />
          <button className={styles.relancer} onClick={charger}>
            RÉESSAYER
          </button>
        </p>
      )}

      {connecte && etat === 'pret' && mesures.length === 0 && (
        <p className={styles.messageBloc}>Aucun capteur exposé par le robot.</p>
      )}

      {/* `connecte` conditionne aussi la grille : hors ligne, les anneaux
          garderaient la dernière valeur reçue, qui ne veut plus rien dire. */}
      {connecte && etat === 'pret' && mesures.length > 0 && (
        <div className={styles.grille}>
          {mesures.map((mesure) => (
            <CarteMesure
              key={mesure.id}
              mesure={mesure}
              valeur={valeurs[mesure.id] ?? mesure.valeur}
              historique={historique[mesure.id] ?? []}
            />
          ))}
        </div>
      )}

      {connecte && uptime != null && (
        <p className={styles.uptime}>
          Debout depuis <span className="data">{dureeLisible(uptime)}</span>
        </p>
      )}
    </section>
  )
}

/**
 * Durée en clair, à deux unités au plus : « 3 j 4 h », « 5 h 12 min », « 12 min ».
 *
 * Deux unités suffisent parce qu'on ne lit pas cette ligne pour chronométrer, mais pour répondre
 * à une seule question — le Jetson a-t-il redémarré dans mon dos ? Les secondes n'y ajoutent
 * rien, et elles feraient changer la ligne à chaque relevé.
 */
function dureeLisible(secondes: number): string {
  const minutes = Math.floor(secondes / 60)
  const heures = Math.floor(minutes / 60)
  const jours = Math.floor(heures / 24)
  if (jours > 0) return `${jours} j ${heures % 24} h`
  if (heures > 0) return `${heures} h ${minutes % 60} min`
  return `${minutes} min`
}

function CarteMesure({
  mesure,
  valeur,
  historique,
}: {
  mesure: Mesure
  valeur: number | null
  historique: number[]
}) {
  return (
    <div className={styles.mesure}>
      <span className={styles.mesureNom}>{mesure.libelle}</span>
      <Anneau
        valeur={valeur}
        min={mesure.min}
        max={mesure.max}
        unite={mesure.unite}
        hautEstBon={mesure.hautEstBon}
      />
      <Tendance historique={historique} />
    </div>
  )
}

/**
 * Jauge circulaire — géométrie inchangée depuis la page Monitoring : rayon 42
 * dans une boîte de 100, trait de 8, bouts ronds, départ à midi.
 */
function Anneau({
  valeur,
  min,
  max,
  unite,
  hautEstBon,
}: {
  valeur: number | null
  min: number
  max: number
  unite: string
  hautEstBon?: boolean
}) {
  const rayon = 42
  const circonference = 2 * Math.PI * rayon
  const position = fractionMesure(valeur, min, max)
  const disponible = valeur != null && position != null
  const fraction = position ?? 0

  return (
    <div className={styles.anneau}>
      <svg viewBox="0 0 100 100">
        <circle cx="50" cy="50" r={rayon} className={styles.anneauFond} />
        {disponible && (
          <circle
            className={styles.anneauArc}
            cx="50"
            cy="50"
            r={rayon}
            fill="none"
            stroke={couleurSeuil(fraction, hautEstBon)}
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circonference}
            strokeDashoffset={circonference * (1 - fraction)}
            transform="rotate(-90 50 50)"
          />
        )}
      </svg>
      <div className={styles.anneauCentre}>
        {disponible ? (
          <>
            <span className={`${styles.anneauNombre} data`}>{Math.round(valeur)}</span>
            <span className={styles.anneauUnite}>{unite}</span>
          </>
        ) : (
          <span className={styles.anneauIndispo}>N/D</span>
        )}
      </div>
    </div>
  )
}

/**
 * Mini-courbe de tendance, à échelle adaptative (min/max de l'historique) et non
 * sur l'échelle absolue de la mesure : sur 0-100 %, une variation de trois points
 * dessinait une ligne parfaitement plate. La courbe montre la forme, l'anneau
 * porte la valeur absolue.
 */
function Tendance({ historique }: { historique: number[] }) {
  if (historique.length < 2) return <div className={styles.tendance} />

  let bas = Math.min(...historique)
  let haut = Math.max(...historique)
  // Marge : évite d'écraser la courbe contre les bords. Étendue nulle (mesure
  // parfaitement stable) : une marge arbitraire, qui centre la ligne plate.
  const etendue = haut - bas
  const marge = etendue > 0 ? etendue * 0.25 : 1
  bas -= marge
  haut += marge

  const hauteur = 22
  const y = (v: number) => (hauteur - 3 - ((v - bas) / (haut - bas)) * (hauteur - 6)).toFixed(1)
  const pas = 100 / (historique.length - 1)
  const points = historique.map((v, i) => `${(i * pas).toFixed(1)},${y(v)}`).join(' ')

  return (
    <svg viewBox={`0 0 100 ${hauteur}`} className={styles.tendance} preserveAspectRatio="none">
      <polygon className={styles.tendanceAire} points={`0,${hauteur} ${points} 100,${hauteur}`} />
      <polyline className={styles.tendanceLigne} points={points} />
    </svg>
  )
}
