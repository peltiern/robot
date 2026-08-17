/**
 * Mise en mots des dates et des durées de la page Personnes.
 *
 * Le robot raisonne en secondes d'absence ; une timeline se lit en français.
 * « revenu après 2 heures » se comprend d'un coup d'œil, « 7412 s » non.
 */

const FORMAT_LONG = new Intl.DateTimeFormat('fr-FR', {
  dateStyle: 'full',
  timeStyle: 'short',
})

const FORMAT_COURT = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})

/** Date complète, pour la fiche. */
export function dateLongue(iso: string): string {
  return FORMAT_LONG.format(new Date(iso))
}

/** Date resserrée, pour une ligne de timeline. */
export function dateCourte(iso: string): string {
  return FORMAT_COURT.format(new Date(iso))
}

/**
 * Temps écoulé depuis une date, en une poignée de mots.
 * Rend `null` si la date manque — le robot ne l'a jamais rencontrée.
 */
export function ilYA(iso: string | null): string | null {
  if (!iso) return null
  return `il y a ${duree((Date.now() - new Date(iso).getTime()) / 1000)}`
}

/**
 * Durée d'absence telle qu'elle se raconte.
 * `-1` marque une première rencontre : il n'y a pas d'avant.
 */
export function absence(secondes: number): string | null {
  if (secondes < 0) return null
  return `absent ${duree(secondes)}`
}

/** Une durée en secondes, arrondie à l'unité qui parle. */
function duree(secondes: number): string {
  const s = Math.max(0, Math.round(secondes))
  if (s < 60) return `${s} s`
  const minutes = Math.round(s / 60)
  if (minutes < 60) return `${minutes} min`
  const heures = Math.round(minutes / 60)
  if (heures < 24) return `${heures} h`
  const jours = Math.round(heures / 24)
  return jours <= 1 ? '1 jour' : `${jours} jours`
}
