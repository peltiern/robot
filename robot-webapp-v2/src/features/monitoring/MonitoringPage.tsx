import { useCallback, useEffect, useState } from 'react'
import { useTelemetryStore } from '../../shared/telemetry/telemetryStore'
import styles from './MonitoringPage.module.css'

/**
 * Page Monitoring : jauges + tendance pour chaque mesure de chaque organe {@code CAPTEUR}.
 *
 * Comme la page Contrôle, tout vient de la découverte de capacités du robot (`GET /api/organes`) :
 * la page ne code en dur ni les organes capteurs, ni leurs mesures — elle boucle sur ce qui est
 * reçu et affiche un widget par mesure. Aujourd'hui, seul l'organe « matériel » (OSHI : CPU,
 * mémoire, température, disque) est exposé côté backend, mais un futur capteur (courant, tension
 * batterie…) apparaîtrait ici sans aucune modification du front.
 * <p>
 * Valeur courante et historique viennent du {@code telemetryStore} partagé (alimenté en continu
 * par {@code TelemetryProvider}, monté au niveau du Layout) plutôt que d'un état local : sinon,
 * changer de page démonterait ce composant et l'historique accumulé serait perdu à chaque retour.
 */
interface Mesure {
  id: string
  libelle: string
  unite: string
  min: number
  max: number
  valeur: number | null
}

interface OrganeCapteur {
  id: string
  libelle: string
  type: string
  mesures: Mesure[]
}

type LoadState = 'loading' | 'ready' | 'error'

/** Borne une valeur dans [min, max]. */
const clamp = (v: number, min: number, max: number) => Math.max(min, Math.min(max, v))

export function MonitoringPage() {
  const [organes, setOrganes] = useState<OrganeCapteur[]>([])
  const [loadState, setLoadState] = useState<LoadState>('loading')

  const valeurs = useTelemetryStore((s) => s.valeurs)
  const historique = useTelemetryStore((s) => s.historique)

  const charger = useCallback(() => {
    setLoadState('loading')
    fetch('/api/organes')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<OrganeCapteur[]>
      })
      .then((organesRecus) => {
        const capteurs = organesRecus.filter((o) => o.type === 'CAPTEUR' && o.mesures.length > 0)
        // Amorce le store UNIQUEMENT pour les mesures sans historique : si la télémétrie live
        // tourne déjà depuis un moment (TelemetryProvider est monté dès le lancement de l'appli,
        // pas seulement sur cette page), on ne veut pas écraser l'historique déjà accumulé par
        // ce seul instantané GET.
        const { historique: historiqueActuel, ingerer } = useTelemetryStore.getState()
        const aAmorcer: Record<string, number> = {}
        for (const organe of capteurs) {
          for (const mesure of organe.mesures) {
            if (mesure.valeur != null && !historiqueActuel[mesure.id]?.length) {
              aAmorcer[mesure.id] = mesure.valeur
            }
          }
        }
        if (Object.keys(aAmorcer).length > 0) ingerer(aAmorcer)
        setOrganes(capteurs)
        setLoadState('ready')
      })
      .catch(() => setLoadState('error'))
  }, [])

  useEffect(() => {
    charger()
  }, [charger])

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h2 className={styles.title}>Monitoring</h2>
      </div>

      {loadState === 'loading' && <p className={styles.info}>Récupération des capteurs du robot…</p>}
      {loadState === 'error' && (
        <p className={styles.warning}>
          Impossible de récupérer les capteurs du robot. Le robot est-il démarré ?{' '}
          <button className={styles.retry} onClick={charger}>Réessayer</button>
        </p>
      )}
      {loadState === 'ready' && organes.length === 0 && (
        <p className={styles.info}>Aucun capteur exposé par le robot.</p>
      )}

      {loadState === 'ready' && (
        <div className={styles.sections}>
          {organes.map((organe) => (
            <section key={organe.id} className={styles.section}>
              <h3 className={styles.sectionTitle}>{organe.libelle}</h3>
              <div className={styles.grid}>
                {organe.mesures.map((mesure) => (
                  <CarteMesure
                    key={mesure.id}
                    mesure={mesure}
                    valeur={valeurs[mesure.id] ?? mesure.valeur}
                    historique={historique[mesure.id] ?? []}
                  />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  )
}

interface CarteMesureProps {
  mesure: Mesure
  valeur: number | null
  historique: number[]
}

function CarteMesure({ mesure, valeur, historique }: CarteMesureProps) {
  return (
    <div className={styles.carte}>
      <span className={styles.carteLabel}>{mesure.libelle}</span>
      <Jauge valeur={valeur} min={mesure.min} max={mesure.max} unite={mesure.unite} />
      <Tendance historique={historique} min={mesure.min} max={mesure.max} />
    </div>
  )
}

/** Seuils de couleur (fraction de l'échelle min-max), communs à toutes les jauges. */
function couleurJauge(fraction: number): string {
  if (fraction >= 0.9) return 'var(--danger)'
  if (fraction >= 0.7) return '#d29922'
  return 'var(--success)'
}

interface JaugeProps {
  valeur: number | null
  min: number
  max: number
  unite: string
}

/** Jauge circulaire (anneau de progression), avec valeur numérique au centre. */
function Jauge({ valeur, min, max, unite }: JaugeProps) {
  const rayon = 42
  const circonference = 2 * Math.PI * rayon
  const disponible = valeur != null
  const fraction = disponible ? clamp((valeur - min) / (max - min), 0, 1) : 0
  const decalage = circonference * (1 - fraction)

  return (
    <div className={styles.jaugeConteneur}>
      <svg viewBox="0 0 100 100" className={styles.jaugeSvg}>
        <circle cx="50" cy="50" r={rayon} className={styles.jaugeFond} />
        {disponible && (
          <circle
            cx="50"
            cy="50"
            r={rayon}
            stroke={couleurJauge(fraction)}
            strokeWidth="8"
            strokeLinecap="round"
            fill="none"
            strokeDasharray={circonference}
            strokeDashoffset={decalage}
            transform="rotate(-90 50 50)"
          />
        )}
      </svg>
      <div className={styles.jaugeValeur}>
        {disponible ? (
          <>
            <span className={styles.jaugeNombre}>{Math.round(valeur)}</span>
            <span className={styles.jaugeUnite}>{unite}</span>
          </>
        ) : (
          <span className={styles.jaugeIndisponible}>N/D</span>
        )}
      </div>
    </div>
  )
}

interface TendanceProps {
  historique: number[]
  min: number
  max: number
}

/** Mini graphique de tendance (dernières minutes) sous la jauge. */
function Tendance({ historique, min, max }: TendanceProps) {
  if (historique.length < 2) {
    return <div className={styles.tendance} />
  }
  const largeur = 100
  const hauteur = 24
  const echelle = (v: number) => hauteur - clamp((v - min) / (max - min), 0, 1) * hauteur
  const pas = largeur / (historique.length - 1)
  const points = historique.map((v, i) => `${(i * pas).toFixed(1)},${echelle(v).toFixed(1)}`).join(' ')

  return (
    <svg viewBox={`0 0 ${largeur} ${hauteur}`} className={styles.tendance} preserveAspectRatio="none">
      <polyline points={points} className={styles.tendanceLigne} />
    </svg>
  )
}
