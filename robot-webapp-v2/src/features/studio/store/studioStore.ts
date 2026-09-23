import { create } from 'zustand'
import {
  avancementDuPoint,
  baseHz,
  borner,
  morceau as nouveauMorceau,
  REGLAGES_PAR_DEFAUT,
  valeurA,
  VERSION_SON,
  VOLUME_MAX,
  volumes,
  type Morceau,
  type Reglages,
  type Son,
  type Timbre,
} from '../synthese/types'

/**
 * Ce que le Studio a en main : un son, ce qui y est choisi, et de quoi revenir en arrière.
 *
 * Toute retouche passe par {@link modifier}, qui remplace le son en entier. C'est ce qui rend
 * l'annulation simple — une pile d'états, pas une pile d'opérations inverses — et ce qui garantit
 * que la frise redessine ce qu'on vient de changer.
 */

export type Mode = 'main' | 'crayon'

export const sonVide = (): Son => ({
  nom: 'nouveau son',
  version: VERSION_SON,
  reglages: REGLAGES_PAR_DEFAUT(),
  morceaux: [],
})

interface EtatStudio {
  son: Son
  selId: number | null
  mode: Mode
  timbreCrayon: Timbre
  historique: Son[]
  refaits: Son[]

  choisir: (id: number | null) => void
  changerMode: (mode: Mode) => void
  changerTimbreCrayon: (timbre: Timbre) => void

  /** Retouche le son. `memoriser` pose un point de retour avant la modification. */
  modifier: (retouche: (son: Son) => void, memoriser?: boolean) => void
  remplacer: (son: Son, memoriser?: boolean) => void
  renommer: (nom: string) => void
  reglerEnsemble: (cle: keyof Reglages, valeur: number, memoriser?: boolean) => void

  annuler: () => void
  refaire: () => void

  morceauChoisi: () => Morceau | null
  ajouterMorceau: (m: Morceau) => void
  supprimerChoisi: () => void
  dupliquerChoisi: () => void
  changerNbPoints: (n: number) => void
  reglerPoint: (i: number, hauteur: number) => void
  reglerVolume: (i: number, volume: number) => void
}

const copie = (son: Son): Son => structuredClone(son)

/** Change le nombre de points sans changer la forme : hauteurs et volumes sont relus sur l'ancien tracé. */
function reechantillonner(m: Morceau, n: number) {
  const note = m.timbre === 'note'
  const relire = (points: number[]) =>
    Array.from({ length: n }, (_, i) => valeurA(m.timbre, points, note ? (i + 0.5) / n : i / (n - 1)))
  const anciensVolumes = [...volumes(m)]
  m.courbe = relire([...m.courbe])
  m.volumes = relire(anciensVolumes)
}

export const useStudioStore = create<EtatStudio>((set, get) => ({
  son: sonVide(),
  selId: null,
  mode: 'main',
  timbreCrayon: 'voix',
  historique: [],
  refaits: [],

  choisir: (selId) => set({ selId }),
  changerMode: (mode) => set({ mode }),
  changerTimbreCrayon: (timbreCrayon) => set({ timbreCrayon }),

  modifier(retouche, memoriser = true) {
    const { son, historique } = get()
    const suivant = copie(son)
    retouche(suivant)
    set({
      son: suivant,
      historique: memoriser ? [...historique.slice(-150), son] : historique,
      refaits: memoriser ? [] : get().refaits,
    })
  },

  remplacer(son, memoriser = true) {
    const precedent = get().son
    set({
      son,
      selId: null,
      historique: memoriser ? [...get().historique.slice(-150), precedent] : [],
      refaits: [],
    })
  },

  renommer(nom) {
    // Sans point de retour : on ne veut pas une entrée d'annulation par lettre tapée.
    get().modifier((son) => {
      son.nom = nom
    }, false)
  },

  reglerEnsemble(cle, valeur, memoriser = true) {
    get().modifier((son) => {
      son.reglages[cle] = valeur
    }, memoriser)
  },

  annuler() {
    const { historique, son, refaits } = get()
    if (!historique.length) return
    const precedent = historique[historique.length - 1]
    set({ son: precedent, historique: historique.slice(0, -1), refaits: [...refaits, son] })
  },

  refaire() {
    const { refaits, son, historique } = get()
    if (!refaits.length) return
    const suivant = refaits[refaits.length - 1]
    set({ son: suivant, refaits: refaits.slice(0, -1), historique: [...historique, son] })
  },

  morceauChoisi() {
    const { son, selId } = get()
    return son.morceaux.find((m) => m.id === selId) ?? null
  },

  ajouterMorceau(m) {
    get().modifier((son) => {
      son.morceaux.push(m)
    })
    set({ selId: m.id })
  },

  supprimerChoisi() {
    const id = get().selId
    if (id == null) return
    get().modifier((son) => {
      son.morceaux = son.morceaux.filter((m) => m.id !== id)
    })
    set({ selId: null })
  },

  dupliquerChoisi() {
    const choisi = get().morceauChoisi()
    if (!choisi) return
    const copieMorceau = nouveauMorceau({
      ...structuredClone(choisi),
      id: undefined as unknown as number,
      debut: choisi.debut + choisi.duree + 0.04,
    })
    get().ajouterMorceau(copieMorceau)
  },

  changerNbPoints(n) {
    const id = get().selId
    get().modifier((son) => {
      const m = son.morceaux.find((x) => x.id === id)
      if (m) reechantillonner(m, borner(n, 2, 9))
    })
  },

  reglerPoint(i, hauteur) {
    const id = get().selId
    get().modifier((son) => {
      const m = son.morceaux.find((x) => x.id === id)
      if (m) m.courbe[i] = borner(hauteur, 0.3, 3.5)
    }, false)
  },

  reglerVolume(i, volume) {
    const id = get().selId
    get().modifier((son) => {
      const m = son.morceaux.find((x) => x.id === id)
      if (m) volumes(m)[i] = borner(volume, 0, VOLUME_MAX)
    }, false)
  },
}))

/** Un morceau neuf, posé à cet instant et à cette hauteur. */
export function morceauA(timbre: Timbre, debut: number, hauteur: number, R: Reglages): Morceau {
  const rapport = borner(hauteur / (baseHz(timbre) * R.hauteur), 0.3, 3.5)
  return nouveauMorceau({
    timbre,
    debut: Math.max(0, debut),
    duree: 0.25,
    courbe: [rapport, rapport * 1.15],
    v1: 'o',
    v2: 'i',
  })
}

export { avancementDuPoint }
