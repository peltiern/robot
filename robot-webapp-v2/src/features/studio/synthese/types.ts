/**
 * Ce qu'est un son du Studio : des morceaux posés sur une frise, et des réglages d'ensemble.
 *
 * C'est aussi le format de la <b>recette</b> enregistrée sur le robot, à côté du WAV. Le robot ne
 * la relit pas — il ne joue que le WAV — mais c'est elle qui permet de rouvrir un son et de le
 * retoucher, donc les noms de champs sont un contrat.
 */

export type Timbre = 'voix' | 'sweep' | 'note' | 'trill' | 'warble' | 'blat'

export type Voyelle = 'a' | 'e' | 'i' | 'o' | 'u' | 'y'

/** Bruit posé juste avant un morceau : un clic sec, un souffle, ou rien. */
export type Attaque = '' | 'click' | 'hiss'

/**
 * Un geste sonore continu.
 *
 * @property debut   instant en secondes, <b>avant</b> application du débit
 * @property courbe  les hauteurs traversées, en rapport de la fréquence de base du timbre ; reliées
 *                   en glissando, ce qui fait tenir une intonation entière dans un seul trait
 * @property volumes un volume par point de la courbe, de 0 à {@link VOLUME_MAX} — c'est ce qui
 *                   donne son relief au morceau, et son épaisseur au ruban de la frise
 * @property vib     tremblement de la voix : vitesse en Hz, profondeur en rapport
 */
export interface Morceau {
  id: number
  timbre: Timbre
  debut: number
  duree: number
  courbe: number[]
  volumes: number[]
  v1: Voyelle
  v2: Voyelle
  vib: [number, number]
  attaque: Attaque
}

/**
 * Les réglages qui valent pour tout le son.
 *
 * @property hauteur     multiplie toutes les fréquences
 * @property debit       divise toutes les durées
 * @property voix        du bip pur à la voix : dose les formants
 * @property tremblement multiplie les tremblements et les roulements
 * @property volume      0 à 1, gradué à l'oreille — voir {@link gainDe}
 * @property presence    resserre l'écart entre fort et faible, puis rattrape : le son porte plus
 *                       sans monter en crête
 */
export interface Reglages {
  hauteur: number
  debit: number
  voix: number
  tremblement: number
  volume: number
  presence: number
}

/**
 * Un son importé d'un fichier, et non dessiné : il n'a pas de morceaux, seulement son enregistrement.
 *
 * L'original est gardé dans la recette, et non seulement sur le robot : le robot joue le son au
 * volume réglé, et c'est de l'original qu'on repart pour le régler à nouveau — repartir du son déjà
 * baissé perdrait un peu de qualité à chaque retouche.
 *
 * @property original le fichier converti au format du Studio (WAV 44,1 kHz mono 16 bits), en base64,
 *                    silences du début et de la fin déjà rognés
 * @property origine  le nom du fichier importé, pour s'en souvenir ; vide pour un son déposé à la
 *                    main sur le robot, rouvert depuis son WAV
 */
export interface SonFichier {
  original: string
  origine: string
}

export interface Son {
  nom: string
  version: number
  reglages: Reglages
  morceaux: Morceau[]
  /** Présent seulement pour un son importé ; `morceaux` est alors vide. */
  fichier?: SonFichier
}

/** Y a-t-il quelque chose à entendre : des morceaux dessinés, ou un fichier importé. */
export const aDuSon = (son: Son): boolean => son.morceaux.length > 0 || !!son.fichier

/**
 * Version du format de recette. La 1 est celle du premier Studio.
 * <p>
 * Le robot refuse d'enregistrer une recette qui ne dit pas sa version : sans elle, un son
 * d'aujourd'hui serait indistinguable d'un son d'un Studio à venir. C'est la leçon du format
 * d'animation, payée une fois.
 */
export const VERSION_SON = 1

/**
 * Un point ne monte pas au-dessus de « plein ».
 * <p>
 * Mesuré sur la maquette : pousser un point à 1,6 ne donnait que 3,5 dB sans présence et 0,2 dB
 * avec — on appuie alors contre le plafond, et la compression reprend ce qu'on ajoute. Pour faire
 * ressortir un point, on baisse les autres.
 */
export const VOLUME_MAX = 1

/** Étendue du curseur de volume, en décibels sous le plafond. */
export const ETENDUE_VOLUME_DB = 30

export const REGLAGES_PAR_DEFAUT = (): Reglages => ({
  hauteur: 1,
  debit: 1,
  voix: 0.9,
  tremblement: 1,
  // Au milieu, et non au plafond : sinon il ne reste rien à monter et tout à descendre.
  volume: 0.6,
  presence: 0.3,
})

export const TIMBRES: Record<Timbre, { nom: string; aide: string }> = {
  voix: { nom: 'Voix', aide: 'Une syllabe, comme une parole.' },
  sweep: { nom: 'Sifflet', aide: 'Un bip qui glisse d’une note à l’autre.' },
  note: { nom: 'Notes', aide: 'Un bip qui saute de note en note.' },
  trill: { nom: 'Trille', aide: 'Un roulement rapide, excité ou inquiet.' },
  warble: { nom: 'Scanner', aide: 'Une vibration électronique, de machine.' },
  blat: { nom: 'Grognement', aide: 'Un bourdon grave et grinçant.' },
}

/** Fréquence à laquelle correspond le rapport 1, selon le timbre. */
export const baseHz = (timbre: Timbre): number =>
  timbre === 'voix' ? 480 : timbre === 'blat' ? 252 : 900

/**
 * Le curseur de volume se lit à l'oreille et non en proportion : l'oreille entend des rapports, si
 * bien qu'un curseur linéaire tassait tout ce qui s'entend dans sa moitié basse. 0 vaut 30 dB sous
 * le plafond, 1 le plafond.
 */
export const gainDe = (niveau: number): number => Math.pow(10, ((niveau - 1) * ETENDUE_VOLUME_DB) / 20)

export const borner = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v))

/** Un volume par point, 1 par défaut ; recalé si la courbe a changé de longueur sans lui. */
export function volumes(morceau: Morceau): number[] {
  if (!morceau.volumes || morceau.volumes.length !== morceau.courbe.length) {
    morceau.volumes = morceau.courbe.map(() => 1)
  }
  return morceau.volumes
}

/**
 * La valeur d'une suite de points à l'avancement `u` (0 à 1).
 *
 * Les notes sautent d'un palier à l'autre, tout le reste glisse — c'est ce que fait le moteur, et
 * la frise doit dessiner la même chose.
 */
export function valeurA(timbre: Timbre, points: number[], u: number): number {
  const n = points.length
  if (timbre === 'note') return points[Math.min(n - 1, Math.floor(u * n))]
  const x = u * (n - 1)
  const i = Math.min(n - 2, Math.floor(x))
  return points[i] + (points[i + 1] - points[i]) * (x - i)
}

/** Avancement auquel se tient le point d'indice `i` : au milieu de son palier pour les notes. */
export const avancementDuPoint = (timbre: Timbre, nbPoints: number, i: number): number =>
  timbre === 'note' ? (i + 0.5) / nbPoints : i / (nbPoints - 1)

let dernierId = 0

export function morceau(patron: Partial<Morceau> = {}): Morceau {
  const m: Morceau = {
    id: ++dernierId,
    timbre: 'voix',
    debut: 0,
    duree: 0.25,
    courbe: [1, 1],
    volumes: [],
    v1: 'o',
    v2: 'o',
    vib: [6, 0.02],
    attaque: '',
    ...patron,
  }
  volumes(m)
  return m
}
