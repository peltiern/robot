/**
 * Code couleur des mesures, commun à tout le HUD.
 *
 * Une même grandeur doit avoir la même couleur partout : la pastille de la barre
 * d'état et l'anneau du volet Vitaux parlent de la même chose, ils ne peuvent pas
 * se contredire. Les seuils sont exprimés en fraction de l'échelle déclarée par
 * le robot (`min`/`max` de la mesure), jamais en valeurs absolues codées ici.
 */

/** Position d'une valeur sur son échelle, dans [0, 1]. `null` si indisponible. */
export function fractionMesure(
  valeur: number | null | undefined,
  min: number,
  max: number,
): number | null {
  if (valeur == null || max <= min) return null
  return Math.max(0, Math.min(1, (valeur - min) / (max - min)))
}

/**
 * Vert jusqu'à 70 % de l'échelle, ambre jusqu'à 90 %, rouge au-delà.
 *
 * `hautEstBon` retourne l'échelle avant d'en tirer la couleur, sans toucher au remplissage :
 * l'anneau montre toujours la vraie fraction, seule la couleur change de sens. Sans ça, un disque
 * dont il ne reste que 3 Go sur 60 s'affichait d'un vert franc — l'anneau presque vide disait la
 * vérité, la couleur disait le contraire, et c'est la couleur qu'on lit de loin.
 */
export function couleurSeuil(fraction: number | null, hautEstBon = false): string {
  if (fraction == null) return 'var(--texte)'
  const gravite = hautEstBon ? 1 - fraction : fraction
  if (gravite >= 0.9) return 'var(--alarme)'
  if (gravite >= 0.7) return 'var(--veille)'
  return 'var(--ok)'
}

/**
 * Même code couleur, mais `null` tant que la mesure est au repos.
 *
 * Pour les pastilles de la barre d'état : deux boîtes vertes en permanence
 * finissent par ne plus rien vouloir dire. Elles restent neutres et ne
 * s'allument qu'à partir de la veille — là, la couleur est une information.
 * Les anneaux des Vitaux, eux, gardent leur vert : on les regarde pour lire une
 * valeur, pas pour être alerté.
 */
export function couleurAlerte(fraction: number | null, hautEstBon = false): string | null {
  if (fraction == null) return null
  const gravite = hautEstBon ? 1 - fraction : fraction
  if (gravite < 0.7) return null
  return couleurSeuil(fraction, hautEstBon)
}
