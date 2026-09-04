// Le contrat JSON du robot, à la lettre.
//
// Ces noms ne sont pas un choix de style : ce sont ceux des records Java et des fichiers
// enregistrés dans $ROBOT_HOME/animations. En changer un ici rend illisibles les animations
// déjà écrites — le backend relirait le champ manquant à null, sans une erreur.
//
// Le modèle interne de l'éditeur (EditorTrack, EditorKeyframe) garde ses propres noms ; la
// frontière entre les deux est convert.ts, et elle est volontairement au même endroit que la
// conversion, pour qu'on voie d'un coup d'oeil ce qui part sur le fil.

export type Axe =
  | 'OEIL_GAUCHE'
  | 'OEIL_DROIT'
  | 'COU_GAUCHE_DROITE'
  | 'COU_HAUT_BAS'
  | 'COU_MONTER_DESCENDRE'

export interface ImageCle {
  instant: number          // ms depuis le début de l'animation
  valeur: number           // degrés d'ORGANE (0 = posture de travail) — pour les yeux ce n'est
                           // pas la position du servo : la tringlerie amplifie de 1,25 à 3,70
  vitesse?: number         // absent = celle de la piste
  acceleration?: number
}

export interface Piste {
  axe: Axe
  vitesseParDefaut: number
  accelerationParDefaut: number
  imagesCles: ImageCle[]
}

export interface Animation {
  nom: string
  dureeTotale: number      // ms ; indépendante de la dernière image-clé, une animation peut
                           // finir sur une pause
  pistes: Piste[]
  sons: unknown[]          // emplacement réservé ; l'éditeur ne les connaît pas encore et les
                           // renvoie tels quels — le backend les conserve de toute façon
  version: number          // unité des valeurs ; cf. VERSION_ANIMATION
}

// Le robot refuse une animation sans version, et il a raison : avant la 1, les pistes des yeux
// portaient des unités de position moteur. La tringlerie n'étant pas linéaire, +14 unités valent
// 19,4° d'œil — une animation de l'ancien monde rejouée telle quelle décalerait le geste d'un
// tiers sans que rien ne le dise.
export const VERSION_ANIMATION = 1

/** Ce que le robot répond quand il lance une animation. */
export interface AnimationEnCours {
  nom: string
  avertissements: string[]
}

/**
 * Un axe animable, tel que le robot le décrit — butées et vitesses de travail comprises.
 *
 * Rien de tout ça n'est codé en dur côté éditeur : les butées viennent de robot.properties, et
 * celles qui étaient écrites ici étaient fausses (l'inclinaison annoncée −25→50 quand le servo
 * ne fait que −8→7). Une timeline qui laisse dessiner ce que la mécanique refuse est pire
 * qu'inutile.
 */
export interface AxeAnimable {
  id: Axe
  libelle: string
  positionMin: number
  positionMax: number
  vitesseParDefaut: number
  accelerationParDefaut: number
}
