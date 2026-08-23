import { create } from 'zustand'
import type { DetectedBox } from '../types/events'

/** Trame prête à l'affichage : l'image est déjà un Blob URL, plus une chaîne base64. */
export interface TrameAffichee {
  url: string
  faces: DetectedBox[]
  objects: DetectedBox[]
}

interface VideoState {
  /** Dernière trame affichable, ou `null` tant qu'aucune image n'est arrivée. */
  trame: TrameAffichee | null
  /** Images par seconde réellement affichées (et non reçues). */
  fps: number
  /**
   * Taille de l'image source, en pixels — connue seulement une fois une image
   * chargée dans le DOM. Sert à ramener les détections (exprimées en pixels
   * source) en fractions, pour la scène comme pour le regard des yeux du logo.
   */
  naturel: { w: number; h: number } | null
  poserTrame: (trame: TrameAffichee) => void
  majFps: (fps: number) => void
  poserNaturel: (naturel: { w: number; h: number }) => void
  oublier: () => void
}

/**
 * Dernière image du robot, partagée : la scène l'affiche, la barre d'état en
 * tire le débit et le nombre de détections. Alimenté par `VideoProvider`, monté
 * au niveau du Layout, donc la réception ne s'interrompt pas quand on passe à
 * l'atelier.
 */
export const useVideoStore = create<VideoState>((set, get) => ({
  trame: null,
  fps: 0,
  naturel: null,

  poserTrame(trame) {
    set({ trame })
  },

  majFps(fps) {
    set({ fps })
  },

  poserNaturel(naturel) {
    // La taille source ne change qu'au (re)démarrage du flux : on ne remplace
    // l'état que si elle diffère vraiment, sinon chaque image chargée (une par
    // trame) relancerait un rendu de toute l'application.
    const actuel = get().naturel
    if (actuel?.w === naturel.w && actuel?.h === naturel.h) return
    set({ naturel })
  },

  oublier() {
    set({ trame: null, fps: 0, naturel: null })
  },
}))
