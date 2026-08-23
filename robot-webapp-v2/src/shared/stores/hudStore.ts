import { create } from 'zustand'

/**
 * État d'affichage du HUD : quels volets sont visibles par-dessus (ou à côté de)
 * la vidéo. Ce n'est pas de la navigation — rien n'est démonté, tout reste
 * abonné : le rail montre et cache, il ne change pas de page.
 *
 * Partagé dans un store plutôt que dans le Layout parce que le rail (à gauche)
 * et les volets (au centre et à droite) sont dans des sous-arbres différents.
 * Retenu d'une session à l'autre : sur la tablette, on rouvre l'appli dans la
 * configuration où on l'avait laissée.
 */
export interface Volets {
  /** Surcouches posées sur l'image : boîtes de détection et compteurs. */
  calques: boolean
  /** Position des articulations, en bas à gauche de la scène. */
  posture: boolean
  /** Fil de discussion, colonne de droite. */
  dialogue: boolean
  /** Jauges des capteurs, colonne de droite. */
  vitaux: boolean
}

const CLE_STOCKAGE = 'robot.volets'

const VOLETS_DEFAUT: Volets = {
  calques: true,
  posture: true,
  dialogue: true,
  vitaux: true,
}

function voletsRetenus(): Volets {
  try {
    const brut = localStorage.getItem(CLE_STOCKAGE)
    if (!brut) return VOLETS_DEFAUT
    const retenus = JSON.parse(brut) as Partial<Volets>
    // Fusion avec les défauts : un volet ajouté plus tard apparaît quand même.
    return { ...VOLETS_DEFAUT, ...retenus }
  } catch {
    return VOLETS_DEFAUT
  }
}

interface HudState extends Volets {
  /** Tiroir Réglages ouvert (jamais retenu : il se referme d'une session à l'autre). */
  reglages: boolean
  basculerVolet: (volet: keyof Volets) => void
  ouvrirReglages: (ouvert: boolean) => void
}

export const useHudStore = create<HudState>((set, get) => ({
  ...voletsRetenus(),
  reglages: false,

  basculerVolet(volet) {
    const etat = get()
    const suivants: Volets = {
      calques: etat.calques,
      posture: etat.posture,
      dialogue: etat.dialogue,
      vitaux: etat.vitaux,
      [volet]: !etat[volet],
    }
    try {
      localStorage.setItem(CLE_STOCKAGE, JSON.stringify(suivants))
    } catch {
      // Choix valable pour la session en cours, sans être retenu.
    }
    set(suivants)
  },

  ouvrirReglages(ouvert) {
    set({ reglages: ouvert })
  },
}))
