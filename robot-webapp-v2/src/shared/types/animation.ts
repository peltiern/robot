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
  valeur: number           // degrés d'ORGANE. Pour les yeux, 0 = dessus de la coque de niveau, et ce
                           // n'est pas la position du servo : la tringlerie amplifie de 1,25 à 3,70
  vitesse?: number         // absent = celle de la piste
  acceleration?: number
}

export interface Piste {
  axe: Axe
  vitesseParDefaut: number
  accelerationParDefaut: number
  imagesCles: ImageCle[]
}

/**
 * Un son du Studio lancé à un instant de l'animation, désigné par son nom dans la bibliothèque —
 * pas embarqué : retoucher le son dans le Studio doit profiter à toutes les animations qui s'en
 * servent.
 */
export interface SonDeclenche {
  instant: number          // ms depuis le début de l'animation
  son: string              // nom du son dans la bibliothèque du Studio
}

export interface Animation {
  nom: string
  dureeTotale: number      // ms ; indépendante de la dernière image-clé, une animation peut
                           // finir sur une pause
  pistes: Piste[]
  sons: SonDeclenche[]     // la piste Son ; envoyée telle quelle, vide comprise — vide veut
                           // dire « plus de son », et le robot efface
  version: number          // unité des valeurs ; cf. VERSION_ANIMATION
}

// Le robot refuse une animation sans version, et il a raison : avant la 1, les pistes des yeux
// portaient des unités de position moteur. La 2 (2026-09-08) change le repère des yeux — zéro à
// la coque de niveau, positif = bord extérieur vers le haut.
// DOIT SUIVRE Animation.VERSION_COURANTE côté Java. Restée à 1 quand le robot est passé à la 2,
// elle faisait enregistrer des animations que le robot ignorait au redémarrage suivant, sans un mot.
export const VERSION_ANIMATION = 2

/** Ce que le robot répond quand il lance une animation. */
export interface AnimationEnCours {
  nom: string
  avertissements: string[]
  /** Ce que les moteurs attendent la bande-son avant de bouger, en ms : la tête de lecture aussi. */
  attenteDuSon?: number
}

/**
 * Un axe animable, tel que le robot le décrit — butées et vitesses de travail comprises.
 *
 * Rien de tout ça n'est codé en dur côté éditeur : les butées viennent de robot.properties, et
 * chaque fois qu'elles ont été recopiées ici, elles ont fini par être fausses. Une timeline qui
 * laisse dessiner ce que la mécanique refuse est pire qu'inutile.
 */
export interface AxeAnimable {
  id: Axe
  libelle: string
  positionMin: number
  positionMax: number
  vitesseParDefaut: number
  accelerationParDefaut: number
}
