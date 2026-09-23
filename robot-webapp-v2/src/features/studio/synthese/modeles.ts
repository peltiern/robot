import { borner, morceau, type Morceau, type Timbre, type Voyelle } from './types'

/**
 * De quoi partir : des modèles tout faits, et un compositeur d'humeurs.
 *
 * Les humeurs ne tirent pas au hasard n'importe quoi : chaque émotion suit des règles de prosodie
 * que l'oreille reconnaît — hauteur moyenne, étendue, débit, pente des syllabes, forme de la fin,
 * timbre. Ce qui en sort est un point de départ modifiable, pas un son figé.
 */

type SuiteVoix = [number, number[], Voyelle, Voyelle, number, [number, number]?][]
type SuiteBips = [Timbre, number, number[], number][]

export const MODELES_VOIX: { nom: string; suite: SuiteVoix }[] = [
  { nom: 'Curieux', suite: [[0.38, [1, 1.03, 1.65], 'o', 'i', 0, [6, 0.02]]] },
  {
    nom: 'Content',
    suite: [
      [0.11, [1.2, 1.5], 'a', 'i', 0.05],
      [0.11, [1.3, 1.7], 'a', 'i', 0.05],
      [0.22, [1.4, 1.95, 1.7], 'o', 'i', 0, [7, 0.02]],
    ],
  },
  { nom: 'Triste', suite: [[0.8, [1.3, 1.22, 0.72], 'u', 'o', 0, [4, 0.035]]] },
  {
    nom: 'Surpris',
    suite: [
      [0.07, [1, 1], 'o', 'o', 0.03],
      [0.32, [1.1, 2.5], 'o', 'i', 0, [8, 0.02]],
    ],
  },
  {
    nom: 'Ohé !',
    suite: [
      [0.24, [1.1, 1.12], 'o', 'o', 0.03],
      [0.4, [1.55, 1.5, 1.35], 'e', 'e', 0, [6, 0.03]],
    ],
  },
  { nom: 'Inquiet', suite: [[0.55, [1, 1.12, 0.95, 1.06], 'u', 'e', 0, [13, 0.06]]] },
  {
    nom: 'Rire',
    suite: Array.from({ length: 5 }, (_, k) => [0.07, [1.7 - k * 0.05, 1.3 - k * 0.05], 'i', 'a', 0.045] as SuiteVoix[number]),
  },
  {
    nom: 'Non non',
    suite: [
      [0.17, [1.25, 0.9], 'o', 'u', 0.07],
      [0.2, [1.2, 0.85], 'o', 'u', 0],
    ],
  },
]

export const MODELES_BIPS: { nom: string; suite?: SuiteBips; tirage?: () => SuiteBips }[] = [
  {
    nom: 'Sifflement',
    suite: [
      ['sweep', 0.25, [1, 2.2], 0.05],
      ['sweep', 0.35, [1.4, 0.8], 0],
    ],
  },
  { nom: 'Trille', suite: [['trill', 0.45, [1.2, 1.3], 0]] },
  { nom: 'Arpège', suite: [['note', 0.36, [1, 1.26, 1.5, 2, 1.5, 2.5], 0]] },
  { nom: 'Pfft', suite: [['blat', 0.45, [1, 0.8], 0]] },
  { nom: 'Alarme', suite: Array.from({ length: 4 }, () => ['sweep', 0.12, [1, 1.6], 0.03] as SuiteBips[number]) },
  { nom: 'Scan', suite: [['warble', 0.8, [0.8, 1.6, 0.8], 0]] },
  {
    nom: 'Bip-bip',
    suite: [
      ['note', 0.07, [1.5, 1.5], 0.06],
      ['note', 0.07, [1.5, 1.5], 0],
    ],
  },
  {
    nom: 'Bavardage',
    tirage: () =>
      Array.from(
        { length: 6 + Math.floor(Math.random() * 5) },
        () => ['note', 0.05 + Math.random() * 0.06, [0.8 + Math.random() * 1.4, 0.8 + Math.random() * 1.4], 0.02] as SuiteBips[number],
      ),
  },
]

export type CleHumeur = 'joie' | 'tristesse' | 'colere' | 'peur' | 'surprise' | 'curiosite' | 'tendresse' | 'fatigue'

interface Humeur {
  libelle: string
  emoji: string
  base: number
  etendue: number
  debit: number
  pente: number
  contour: (avancement: number, rang: number) => number
  fin: number[]
  voyelles: Voyelle[]
  vib: [number, number]
  voix: number
  clic: number
  souffle: number
  finPlusLongue?: number
}

export const HUMEURS: Record<CleHumeur, Humeur> = {
  joie: { libelle: 'Joie', emoji: '😄', base: 1.35, etendue: 0.3, debit: 1.25, pente: 0.18, contour: (_p, k) => 0.12 * Math.sin(k * 1.9), fin: [1.05, 1.35, 1.2], voyelles: ['a', 'i', 'e'], vib: [7, 0.015], voix: 1, clic: 0.5, souffle: 0.1 },
  tristesse: { libelle: 'Tristesse', emoji: '😢', base: 0.82, etendue: 0.08, debit: 0.6, pente: -0.12, contour: (p) => -0.2 * p, fin: [1, 0.88, 0.68], voyelles: ['u', 'o'], vib: [4, 0.035], voix: 0.8, clic: 0.1, souffle: 0.05 },
  colere: { libelle: 'Colère', emoji: '😠', base: 1, etendue: 0.2, debit: 1.35, pente: -0.25, contour: (_p, k) => (k % 2 ? 0.12 : -0.05), fin: [1.25, 0.75], voyelles: ['a', 'e'], vib: [22, 0.02], voix: 1, clic: 0.8, souffle: 0.3 },
  peur: { libelle: 'Peur', emoji: '😨', base: 1.5, etendue: 0.3, debit: 1.45, pente: 0.05, contour: () => 0, fin: [1, 1.1, 1.3], voyelles: ['i', 'y', 'e'], vib: [14, 0.06], voix: 0.9, clic: 0.2, souffle: 0.4 },
  surprise: { libelle: 'Surprise', emoji: '😮', base: 1.05, etendue: 0.15, debit: 1.1, pente: 0.1, contour: (p) => 0.1 * p, fin: [0.9, 1.9, 1.8], voyelles: ['o', 'a'], vib: [6, 0.02], voix: 1, clic: 0.3, souffle: 0.1 },
  curiosite: { libelle: 'Curiosité', emoji: '🤔', base: 1.08, etendue: 0.2, debit: 0.95, pente: 0.08, contour: (p) => 0.05 * p, fin: [1, 1.05, 1.6], voyelles: ['o', 'u', 'i'], vib: [6, 0.02], voix: 1, clic: 0.3, souffle: 0.1 },
  tendresse: { libelle: 'Tendresse', emoji: '🥰', base: 1.15, etendue: 0.12, debit: 0.75, pente: 0.04, contour: (p) => 0.08 * Math.sin(p * Math.PI), fin: [1.1, 1.2, 1], voyelles: ['u', 'o', 'y'], vib: [5, 0.02], voix: 0.3, clic: 0.05, souffle: 0 },
  fatigue: { libelle: 'Fatigue', emoji: '😴', base: 0.72, etendue: 0.05, debit: 0.55, pente: -0.05, contour: (p) => -0.1 * p, fin: [0.95, 0.85, 0.6], voyelles: ['e', 'o'], vib: [3, 0.01], voix: 0.9, clic: 0.1, souffle: 0.05, finPlusLongue: 3 },
}

/** Timbres de bips privilégiés par humeur, quand on compose en électronique. */
const BIPS_PAR_HUMEUR: Record<CleHumeur, Partial<Record<Timbre, number>>> = {
  joie: { note: 0.45, sweep: 0.25, trill: 0.3 },
  tristesse: { sweep: 0.8, warble: 0.2 },
  colere: { blat: 0.45, note: 0.3, warble: 0.25 },
  peur: { trill: 0.5, warble: 0.3, note: 0.2 },
  surprise: { sweep: 0.5, note: 0.5 },
  curiosite: { sweep: 0.5, note: 0.3, trill: 0.2 },
  tendresse: { sweep: 0.6, note: 0.4 },
  fatigue: { sweep: 0.6, blat: 0.4 },
}

export type Longueur = 'court' | 'moyen' | 'long'
const LONGUEURS: Record<Longueur, [number, number]> = { court: [2, 3], moyen: [5, 7], long: [9, 12] }

export type Style = 'voix' | 'bips'

const entre = (a: number, b: number) => a + Math.floor(Math.random() * (b - a + 1))
const auHasard = <T,>(valeurs: T[]): T => valeurs[Math.floor(Math.random() * valeurs.length)]

function tirageMouille(poids: Partial<Record<Timbre, number>>): Timbre {
  const total = Object.values(poids).reduce((a, b) => a + (b ?? 0), 0)
  let x = Math.random() * total
  for (const [timbre, p] of Object.entries(poids)) {
    if ((x -= p ?? 0) <= 0) return timbre as Timbre
  }
  return Object.keys(poids)[0] as Timbre
}

/**
 * Les modèles et les humeurs ont été écrits du temps où chaque morceau s'éteignait sur son dernier
 * cinquième. Sans compensation, ils finiraient net et sonneraient autrement ; on baisse donc le
 * volume de leur dernier point juste assez pour que le morceau garde la même énergie qu'avant.
 */
function volumeFinalEquivalent(duree: number, nbPoints: number): number {
  const energie = (profil: (u: number) => number) => {
    let somme = 0
    for (let i = 0; i < 400; i++) {
      const v = profil((i + 0.5) / 400)
      somme += v * v
    }
    return somme
  }
  const attaque = Math.min(0.018, duree * 0.3) / duree
  const relache = Math.min(0.03, duree * 0.4) / duree
  const dernierPalier = (nbPoints - 2) / (nbPoints - 1)

  const avant = energie((u) => (u < attaque ? u / attaque : u < 0.8 ? 1 : Math.pow(1e-4, (u - 0.8) / 0.2)))
  const apres = (V: number) =>
    energie((u) => {
      const env = u < attaque ? u / attaque : u > 1 - relache ? Math.pow(1e-4, (u - (1 - relache)) / relache) : 1
      const volume = u <= dernierPalier ? 1 : Math.pow(V, (u - dernierPalier) / (1 - dernierPalier))
      return env * volume
    })

  let bas = 1e-3
  let haut = 1
  for (let k = 0; k < 40; k++) {
    const milieu = Math.sqrt(bas * haut)
    if (apres(milieu) > avant) haut = milieu
    else bas = milieu
  }
  return Math.sqrt(bas * haut)
}

/** Les bips s'éteignaient déjà en 30 ms : il n'y a que les voix à compenser. */
function compenserFondu(m: Morceau): Morceau {
  if (m.timbre !== 'voix') return m
  m.volumes[m.courbe.length - 1] = volumeFinalEquivalent(m.duree, m.courbe.length)
  return m
}

export function depuisSuiteVoix(suite: SuiteVoix): Morceau[] {
  let t = 0
  return suite.map(([duree, courbe, v1, v2, silence, vib]) => {
    const m = compenserFondu(morceau({ debut: t, duree, courbe: [...courbe], v1, v2, vib: vib ? [...vib] : [5, 0.012] }))
    t += duree + (silence || 0)
    return m
  })
}

export function depuisSuiteBips(suite: SuiteBips): Morceau[] {
  let t = 0
  return suite.map(([timbre, duree, courbe, silence]) => {
    const m = morceau({ timbre, debut: t, duree, courbe: [...courbe] })
    t += duree + silence
    return m
  })
}

/**
 * Compose un babillage d'humeur : les morceaux, et la dose de voix qui va avec.
 *
 * L'intensité ne fait pas que monter le ton : elle dose tout à la fois — écart des hauteurs, débit,
 * tremblement, attaques —, ce qui donne « un peu triste » plutôt que « triste à moitié fort ».
 */
export function composer(cle: CleHumeur, intensite: number, longueur: Longueur, style: Style): { morceaux: Morceau[]; voix: number } {
  const h = HUMEURS[cle]
  const dose = 0.35 + 0.65 * intensite
  const nb = entre(...LONGUEURS[longueur])
  const vitesse = 1 + (h.debit - 1) * dose
  const vib: [number, number] = [h.vib[0], h.vib[1] * dose]
  const morceaux: Morceau[] = []
  let t = 0
  let motRestant = entre(1, 3)

  for (let k = 0; k < nb; k++) {
    const avancement = nb > 1 ? k / (nb - 1) : 1
    const dernier = k === nb - 1
    const hauteur = 1 + (h.base - 1) * dose + h.contour(avancement, k) * dose + (Math.random() - 0.5) * h.etendue * dose
    let courbe = dernier
      ? h.fin.map((e) => hauteur * (1 + (e - 1) * dose))
      : [hauteur, hauteur * (1 + h.pente * dose * (0.5 + Math.random()))]
    const v1 = auHasard(h.voyelles)
    const v2 = Math.random() < 0.3 ? auHasard(h.voyelles) : v1
    let duree = (0.09 + Math.random() * 0.07) * (dernier ? h.finPlusLongue ?? 1.9 : 1)
    let attaque: Morceau['attaque'] = Math.random() < h.clic * dose ? 'click' : Math.random() < h.souffle * dose ? 'hiss' : ''
    let silence = 0.015
    if (dernier) silence = 0
    else if (--motRestant <= 0) {
      motRestant = entre(1, 3)
      silence = 0.07 + Math.random() * 0.05
    }

    let timbre: Timbre = 'voix'
    if (style === 'bips') {
      timbre = dernier ? (cle === 'peur' ? 'trill' : cle === 'colere' ? 'blat' : 'sweep') : tirageMouille(BIPS_PAR_HUMEUR[cle])
      duree *= 0.8
      silence *= 0.6
      attaque = ''
      if (timbre === 'note' && !dernier) {
        courbe = Array.from({ length: entre(3, 5) }, (_, i) =>
          borner(hauteur * (1 + (Math.random() - 0.5) * 0.7 * dose + (h.pente * dose * i) / 3), 0.3, 3.5),
        )
        duree = (duree / 0.8) * 1.3
      }
    }

    if (attaque === 'click') t += 0.012
    else if (attaque === 'hiss') t += 0.035

    const d = duree / vitesse
    morceaux.push(compenserFondu(morceau({ timbre, debut: t, duree: d, courbe, v1, v2, vib: [...vib], attaque })))
    t += d + silence / vitesse
  }

  return { morceaux, voix: Math.min(1, 0.9 * h.voix) }
}
