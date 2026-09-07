/** Réglages du suivi de visage, tels que le robot les applique à l'instant. */
export type ReglagesRegard = {
  actif: boolean
  zoneMorteDegres: number
  champHorizontalDegres: number
  centreXRelatif: number
  centreYRelatif: number
  deportDegres: number
}

// Chemin relatif : c'est le proxy Vite qui pointe sur le robot (cf. animationApi).
const URL_REGARD = '/api/regard'

/**
 * Les réglages sont relus, jamais figés : ces clés sont rechargées à chaud sur le robot, et un
 * repère qui montrerait le réglage d'il y a une heure serait pire que pas de repère du tout.
 */
export const regardApi = {
  reglages: (): Promise<ReglagesRegard> =>
    fetch(URL_REGARD).then(r => {
      if (!r.ok) throw new Error(r.statusText)
      return r.json()
    }),
}

/**
 * Demi-côté de la zone morte, en pixels de l'image analysée.
 *
 * Le robot ne l'envoie pas : c'est `Regard` qui recalcule sa focale image par image, depuis la
 * largeur que lui donne la vision. On refait ici le même calcul, avec la même largeur — sans quoi
 * le repère dessiné et la décision du robot ne parleraient pas de la même chose.
 *
 * **Un demi-côté, et non un rayon.** `Regard` teste les deux axes séparément (`|écart panoramique|`
 * puis `|écart inclinaison|`), donc la région où il ne commande rien est un carré, pas un disque.
 * Un cercle laisserait croire qu'un visage posé dans un coin est corrigé alors qu'il ne l'est pas.
 */
export function demiCoteZoneMorte(reglages: ReglagesRegard, largeurImage: number): number {
  const focale = largeurImage / 2 / Math.tan((reglages.champHorizontalDegres * Math.PI) / 360)
  return focale * Math.tan((reglages.zoneMorteDegres * Math.PI) / 180)
}

/**
 * Où le panoramique vise réellement, en fraction de la largeur.
 *
 * Ce n'est pas l'axe optique : la webcam n'étant que dans un des deux yeux, `Regard` vise quelques
 * degrés à côté pour que le robot présente sa figure. Le repère doit se déplacer avec, sinon il
 * désigne un centre que plus personne ne vise.
 */
export function centreViseXRelatif(reglages: ReglagesRegard, largeurImage: number): number {
  const focale = largeurImage / 2 / Math.tan((reglages.champHorizontalDegres * Math.PI) / 360)
  return reglages.centreXRelatif + (focale * Math.tan((reglages.deportDegres * Math.PI) / 180)) / largeurImage
}
