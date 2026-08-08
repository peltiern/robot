import { create } from 'zustand'

/**
 * État vital d'un organe. Trois valeurs et non deux : un organe éteint volontairement
 * (vision désactivée, manette non branchée) n'est pas en panne, et l'afficher en rouge
 * reviendrait à apprendre à l'utilisateur à ignorer les pastilles.
 */
export type EtatSante = 'VIVANT' | 'MUET' | 'ETEINT'

/**
 * Agit sur le monde, ou l'observe ? Déclarée par l'organe côté robot, jamais déduite ici : la
 * manette est un capteur — elle observe l'opérateur — bien qu'elle soit surveillée par le
 * watchdog, étant la seule à pouvoir envoyer le STOPPER qui arrête les chenilles.
 */
export type NatureOrgane = 'ACTIONNEUR' | 'CAPTEUR'

export interface SanteOrgane {
  id: string
  libelle: string
  nature: NatureOrgane
  etat: EtatSante
  /** Âge du dernier battement, ou `null` si l'organe n'a jamais donné signe de vie. */
  ageMillis: number | null
  /** Vrai si le watchdog peut couper les moteurs à cause de cet organe. */
  surveille: boolean
}

interface SanteState {
  organes: SanteOrgane[]
  appliquer: (organes: SanteOrgane[]) => void
  oublier: () => void
}

/**
 * Santé des organes du robot, rafraîchie en continu par `SanteProvider` depuis
 * `/events/sante-organes` — le même relevé que celui sur lequel le watchdog décide de
 * couper les moteurs. Amorcé par la découverte de capacités (`GET /api/organes`) pour ne pas
 * rester vide la première seconde.
 */
export const useSanteStore = create<SanteState>((set) => ({
  organes: [],

  appliquer(organes) {
    set({ organes })
  },

  // Hors ligne, on n'affiche rien : une pastille verte figée sur la dernière trame reçue
  // dirait que tout va bien d'un robot dont on n'a plus de nouvelles.
  oublier() {
    set({ organes: [] })
  },
}))
