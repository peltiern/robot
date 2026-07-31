import { create } from 'zustand'

/**
 * Palettes disponibles. Ajouter une entrée ici (et le bloc `[data-palette="…"]`
 * correspondant dans `palettes.css`) suffit à la proposer dans le tiroir Réglages.
 */
export const PALETTES = [
  { id: 'walle', libelle: 'Wall·E' },
  { id: 'eve', libelle: 'EVE' },
  { id: 'nuit', libelle: 'Nuit' },
] as const

export type IdPalette = (typeof PALETTES)[number]['id']

const PALETTE_DEFAUT: IdPalette = 'walle'
const CLE_STOCKAGE = 'robot.palette'

const estConnue = (id: string | null): id is IdPalette =>
  PALETTES.some((p) => p.id === id)

/** Pose la palette sur `<html data-palette="…">`, ce que lit `palettes.css`. */
function appliquer(id: IdPalette) {
  document.documentElement.dataset.palette = id
}

function paletteRetenue(): IdPalette {
  try {
    const retenue = localStorage.getItem(CLE_STOCKAGE)
    if (estConnue(retenue)) return retenue
  } catch {
    // Navigation privée / stockage refusé : on se rabat sur le défaut.
  }
  return PALETTE_DEFAUT
}

interface ThemeState {
  palette: IdPalette
  choisirPalette: (id: IdPalette) => void
}

export const useThemeStore = create<ThemeState>((set) => ({
  palette: paletteRetenue(),

  choisirPalette(id) {
    appliquer(id)
    try {
      localStorage.setItem(CLE_STOCKAGE, id)
    } catch {
      // Le choix vaudra pour la session en cours, sans être retenu.
    }
    set({ palette: id })
  },
}))

// Application immédiate au chargement du module, avant le premier rendu : sinon
// la page s'afficherait un instant dans la palette par défaut avant de basculer.
appliquer(useThemeStore.getState().palette)
