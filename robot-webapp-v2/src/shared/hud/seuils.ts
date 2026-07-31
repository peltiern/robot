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

/** Vert jusqu'à 70 % de l'échelle, ambre jusqu'à 90 %, rouge au-delà. */
export function couleurSeuil(fraction: number | null): string {
  if (fraction == null) return 'var(--texte)'
  if (fraction >= 0.9) return 'var(--alarme)'
  if (fraction >= 0.7) return 'var(--veille)'
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
export function couleurAlerte(fraction: number | null): string | null {
  if (fraction == null || fraction < 0.7) return null
  return couleurSeuil(fraction)
}
