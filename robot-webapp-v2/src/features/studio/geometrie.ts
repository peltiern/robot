import { avancementDuPoint, baseHz, borner, type Morceau, type Reglages, valeurA, volumes } from './synthese/types'

/**
 * Où se place un son sur la frise. Le temps va de gauche à droite, la hauteur de bas en haut.
 *
 * L'axe des hauteurs est <b>logarithmique</b> et en hertz, pas en rapport : l'oreille entend des
 * rapports, et deux timbres au même rapport ne sonnent pas à la même hauteur — la base d'une voix
 * est à 480 Hz, celle d'un bip à 900. En hertz, ce qu'on voit plus haut s'entend plus aigu, quel
 * que soit le timbre.
 */

export const FMIN = 110
export const FMAX = 3400

/** Gouttière de gauche (les repères aigu/grave), marge de droite, hauteur de la règle. */
export const MARGE_G = 52
export const MARGE_D = 16
export const REGLE = 24

/** Distance à laquelle la pastille de volume flotte au-dessus du bord du ruban. */
export const ECART_PASTILLE = 14

export interface Cadre {
  w: number
  h: number
  /** Durée visible, en secondes. */
  fenetre: number
}

export const xDe = (t: number, c: Cadre) => MARGE_G + (t / c.fenetre) * (c.w - MARGE_G - MARGE_D)
export const tDe = (x: number, c: Cadre) => ((x - MARGE_G) / (c.w - MARGE_G - MARGE_D)) * c.fenetre
export const yDe = (f: number, c: Cadre) =>
  REGLE + 12 + (1 - Math.log(f / FMIN) / Math.log(FMAX / FMIN)) * (c.h - REGLE - 24)
export const fDe = (y: number, c: Cadre) =>
  FMIN * Math.pow(FMAX / FMIN, 1 - (y - REGLE - 12) / (c.h - REGLE - 24))

/** Fin du dernier morceau, en temps entendu (débit appliqué). */
export const finDe = (morceaux: Morceau[], R: Reglages) =>
  Math.max(0, ...morceaux.map((m) => (m.debut + m.duree) / R.debit))

/**
 * Fenêtre visible : au moins 2 s, arrondie à la demi-seconde pour que la règle tombe juste, et
 * bornée à 60 s. La borne haute n'est pas un choix d'affichage : la règle se dessine graduation
 * par graduation, et un morceau traîné très loin ferait boucler ce tracé sans fin visible.
 */
export const fenetreDe = (morceaux: Morceau[], R: Reglages) =>
  Math.min(60, Math.max(2, Math.ceil((finDe(morceaux, R) * 1.15 + 0.25) * 2) / 2))

/** La ligne du ruban : la hauteur entendue, roulement mis à part. */
export const frequenceCentre = (m: Morceau, u: number, R: Reglages) =>
  borner(baseHz(m.timbre) * R.hauteur * valeurA(m.timbre, m.courbe, u), FMIN * 1.02, FMAX * 0.98)

export const volumeA = (m: Morceau, u: number) => valeurA(m.timbre, volumes(m), u)

/**
 * Amplitude du roulement, en hertz : trille, scanner, tremblement de la voix.
 *
 * La frise le dessine <b>en bande</b> et non oscillation par oscillation : l'oreille entend une
 * note qui roule, pas quatre-vingts sauts, et le peigne rendait la frise illisible dès qu'il y
 * avait deux trilles.
 */
export function ampRoulement(m: Morceau, R: Reglages): number {
  const H = R.hauteur
  if (m.timbre === 'voix') return 480 * H * m.vib[1] * R.tremblement
  if (m.timbre === 'trill') return 900 * H * 0.22 * Math.max(R.tremblement, 0.2)
  if (m.timbre === 'warble') return 900 * H * 0.12 * Math.max(R.tremblement, 0.2)
  return 0
}

/** Demi-épaisseur du ruban à volume plein. Le grognement est plus gras : il sonne plus large. */
export const epaisseurMax = (m: Morceau, R: Reglages) =>
  (m.timbre === 'blat' ? 13 : 9) * (R.volume * 0.6 + 0.4)

/** Attaque et extinction du morceau, entre 0 et 1 — les deux bouts effilés du ruban. */
export function enveloppe(m: Morceau, u: number, R: Reglages): number {
  const d = m.duree / R.debit
  const t = u * d
  const attaque = Math.min(m.timbre === 'voix' ? 0.018 : 0.01, d * 0.3)
  const relache = Math.min(0.03, d * 0.4)
  if (t < attaque) return t / attaque
  if (t > d - relache) return Math.max(0, (d - t) / relache)
  return 1
}

export const yPastille = (m: Morceau, i: number, yPoint: number, R: Reglages) =>
  yPoint - ECART_PASTILLE - epaisseurMax(m, R) * volumes(m)[i]

export const xDuPoint = (m: Morceau, i: number, c: Cadre, R: Reglages) =>
  xDe((m.debut + avancementDuPoint(m.timbre, m.courbe.length, i) * m.duree) / R.debit, c)

export const yDuPoint = (m: Morceau, i: number, c: Cadre, R: Reglages) =>
  yDe(baseHz(m.timbre) * R.hauteur * m.courbe[i], c)

export interface PointRuban {
  x: number
  y: number
  /** Demi-épaisseur au point courant. */
  th: number
  /** Bords haut et bas de la bande de roulement, 0 s'il n'y en a pas. */
  yb: number
  yh: number
  /** Bords du ruban, portés perpendiculairement au trait. */
  ux: number
  uy: number
  lx: number
  ly: number
}

/**
 * Le tracé d'un morceau, prêt à dessiner.
 *
 * L'épaisseur se porte <b>perpendiculairement</b> au trait : portée à la verticale, elle fondait
 * dans les montées raides et gonflait sur les paliers, ce qui n'avait aucun rapport avec le son.
 */
export function traceRuban(m: Morceau, c: Cadre, R: Reglages): PointRuban[] {
  const debut = m.debut / R.debit
  const duree = m.duree / R.debit
  const N = borner(Math.ceil(duree * 300), 30, 400)
  const K = epaisseurMax(m, R)
  const A = ampRoulement(m, R)
  const points: PointRuban[] = []

  for (let k = 0; k <= N; k++) {
    const u = k / N
    const f = frequenceCentre(m, u, R)
    points.push({
      x: xDe(debut + u * duree, c),
      y: yDe(f, c),
      th: 1.2 + K * volumeA(m, u) * enveloppe(m, u, R),
      yb: A ? yDe(Math.min(FMAX, f + A), c) : 0,
      yh: A ? yDe(Math.max(FMIN, f - A), c) : 0,
      ux: 0,
      uy: 0,
      lx: 0,
      ly: 0,
    })
  }

  points.forEach((p, k) => {
    const avant = points[Math.max(0, k - 1)]
    const apres = points[Math.min(N, k + 1)]
    const dx = apres.x - avant.x
    const dy = apres.y - avant.y
    const l = Math.hypot(dx, dy) || 1
    const nx = -dy / l
    const ny = dx / l
    p.ux = p.x - nx * p.th
    p.uy = p.y - ny * p.th
    p.lx = p.x + nx * p.th
    p.ly = p.y + ny * p.th
  })

  return points
}

/** Le morceau qui sonne à cet instant, s'il y en a un — le dernier posé gagne. */
export function morceauA(morceaux: Morceau[], t: number, R: Reglages): Morceau | null {
  for (let i = morceaux.length - 1; i >= 0; i--) {
    const m = morceaux[i]
    if (t >= m.debut / R.debit && t <= (m.debut + m.duree) / R.debit) return m
  }
  return null
}
