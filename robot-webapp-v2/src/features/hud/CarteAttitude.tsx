import { useState, type ReactNode } from 'react'
import styles from './hud.module.css'

/**
 * Attitude du robot dessinée en silhouettes : de dos pour le roulis, de profil pour le tangage,
 * de dessus pour le cap.
 *
 * Les silhouettes plutôt qu'un horizon artificiel : celui-ci se lit à l'envers pour qui n'est pas
 * pilote (c'est le ciel qui penche, pas l'engin), alors qu'un robot qui penche se comprend d'un
 * coup d'œil.
 *
 * Les sens de rotation supposent la carte montée à plat, axe X vers l'avant ; ils sont à vérifier
 * sur le robot, la centrale n'y ayant pas encore été fixée.
 */
export function CarteAttitude({
  roulis,
  tangage,
  cap,
}: {
  roulis: number | null
  tangage: number | null
  cap: number | null
}) {
  // Le cap passe de +180 à −180 d'un tour à l'autre : sans déroulage, la transition CSS ferait
  // faire au robot un tour complet dans le mauvais sens à chaque passage de la couture.
  const capDeroule = useAngleDeroule(cap)

  return (
    <div className={styles.attitude}>
      <Vue libelle="Roulis" valeur={roulis}>
        <Sol />
        <g className={styles.attitudeCorps} style={{ transform: `rotate(${roulis ?? 0}deg)` }}>
          <RobotDeDos />
        </g>
      </Vue>

      <Vue libelle="Tangage" valeur={tangage}>
        <Sol />
        {/* Négatif : en SVG un angle positif tourne dans le sens horaire, donc pique du nez. */}
        <g className={styles.attitudeCorps} style={{ transform: `rotate(${-(tangage ?? 0)}deg)` }}>
          <RobotDeProfil />
        </g>
      </Vue>

      <Vue libelle="Cap" valeur={cap}>
        <Cadran />
        {/* Négatif : le lacet croît dans le sens trigonométrique vu de dessus. */}
        <g className={styles.attitudeCap} style={{ transform: `rotate(${-capDeroule}deg)` }}>
          <RobotDeDessus />
        </g>
      </Vue>
    </div>
  )
}

function Vue({
  libelle,
  valeur,
  children,
}: {
  libelle: string
  valeur: number | null
  children: ReactNode
}) {
  const disponible = valeur != null
  return (
    <div className={styles.attitudeVue}>
      <span className={styles.mesureNom}>{libelle}</span>
      <svg viewBox="0 0 100 100" className={disponible ? undefined : styles.attitudeIndispo}>
        {children}
      </svg>
      {disponible ? (
        <span className={`${styles.attitudeValeur} data`}>{angleSigne(valeur)}</span>
      ) : (
        <span className={styles.anneauIndispo}>N/D</span>
      )}
    </div>
  )
}

/** Signe toujours écrit : « +3° » et « −3° » doivent se distinguer sans regarder le dessin. */
function angleSigne(degres: number): string {
  const arrondi = Math.round(degres)
  if (arrondi === 0) return '0°'
  return `${arrondi > 0 ? '+' : '−'}${Math.abs(arrondi)}°`
}

/**
 * Angle sans saut : chaque nouvelle valeur est rapprochée de la précédente par le plus court
 * chemin, quitte à dépasser ±180.
 */
function useAngleDeroule(angle: number | null): number {
  const [suivi, setSuivi] = useState({ brut: angle, deroule: angle ?? 0 })
  if (angle != null && angle !== suivi.brut) {
    const ecart = suivi.brut == null ? 0 : ((((angle - suivi.deroule) % 360) + 540) % 360) - 180
    const deroule = suivi.brut == null ? angle : suivi.deroule + ecart
    setSuivi({ brut: angle, deroule })
    return deroule
  }
  return suivi.deroule
}

/** Sol fixe et verticale de référence : c'est contre eux que se lit l'inclinaison. */
function Sol() {
  return (
    <>
      <line x1="50" y1="8" x2="50" y2="82" className={styles.attitudeRepere} />
      <line x1="6" y1="82" x2="94" y2="82" className={styles.attitudeSol} />
    </>
  )
}

function Cadran() {
  const graduations = Array.from({ length: 12 }, (_, i) => i * 30)
  return (
    <>
      <circle cx="50" cy="50" r="44" className={styles.attitudeCadran} />
      {graduations.map((angle) => (
        <line
          key={angle}
          x1="50"
          y1={angle === 0 ? 2 : 6}
          x2="50"
          y2="12"
          transform={`rotate(${angle} 50 50)`}
          className={angle === 0 ? styles.attitudeZero : styles.attitudeGraduation}
        />
      ))}
    </>
  )
}

// Silhouettes : chenilles, caisse, cou, tête à deux yeux. Le pivot des vues de dos et de profil
// est le point de contact au sol (50, 82), réglé en CSS : le robot bascule sur ses chenilles et
// non autour de son centre, ce qui est ce qu'il fait réellement.

function RobotDeDos() {
  return (
    <>
      <rect x="22" y="64" width="15" height="18" rx="3" className={styles.attitudeChenille} />
      <rect x="63" y="64" width="15" height="18" rx="3" className={styles.attitudeChenille} />
      <rect x="29" y="40" width="42" height="27" rx="3" className={styles.attitudeCaisse} />
      <rect x="47" y="31" width="6" height="10" className={styles.attitudeCaisse} />
      <rect x="31" y="18" width="16" height="13" rx="5" className={styles.attitudeCaisse} />
      <rect x="53" y="18" width="16" height="13" rx="5" className={styles.attitudeCaisse} />
    </>
  )
}

function RobotDeProfil() {
  return (
    <>
      <rect x="16" y="66" width="68" height="16" rx="8" className={styles.attitudeChenille} />
      <rect x="28" y="40" width="44" height="27" rx="3" className={styles.attitudeCaisse} />
      <rect x="45" y="31" width="6" height="10" className={styles.attitudeCaisse} />
      {/* L'œil déborde vers la droite : c'est l'avant du robot. */}
      <rect x="42" y="18" width="24" height="13" rx="5" className={styles.attitudeCaisse} />
      <circle cx="63" cy="24.5" r="3" className={styles.attitudeOeil} />
    </>
  )
}

function RobotDeDessus() {
  return (
    <>
      <rect x="27" y="26" width="11" height="48" rx="4" className={styles.attitudeChenille} />
      <rect x="62" y="26" width="11" height="48" rx="4" className={styles.attitudeChenille} />
      <rect x="37" y="32" width="26" height="36" rx="3" className={styles.attitudeCaisse} />
      {/* Flèche de l'avant : sans elle, vu de dessus, l'avant et l'arrière se confondent. */}
      <path d="M50 14 L58 26 L42 26 Z" className={styles.attitudeAvant} />
    </>
  )
}
