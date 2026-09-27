import { create } from 'zustand'
import type { EmotionConnue } from '../types/emotions'

/*
 * Les émotions du robot, telles qu'il les a données la dernière fois.
 *
 * Le robot seul définit la liste. Mais l'Atelier doit la proposer robot éteint : le navigateur en
 * garde donc une copie, relue au démarrage et remplacée à chaque réponse du robot. Seul un
 * navigateur qui n'a jamais vu le robot n'a rien à proposer.
 */

const CLE = 'robot.emotions'

function lireCache(): EmotionConnue[] {
  try {
    return JSON.parse(localStorage.getItem(CLE) ?? '[]') as EmotionConnue[]
  } catch {
    return []
  }
}

interface EtatEmotions {
  emotions: EmotionConnue[]
  /** Redemande la liste au robot ; sans réponse, la copie du navigateur reste. */
  rafraichir: () => Promise<void>
}

export const useEmotions = create<EtatEmotions>((set) => ({
  emotions: lireCache(),
  async rafraichir() {
    try {
      const reponse = await fetch('/api/emotions')
      if (!reponse.ok) return
      const emotions = (await reponse.json()) as EmotionConnue[]
      set({ emotions })
      try {
        localStorage.setItem(CLE, JSON.stringify(emotions))
      } catch {
        // Stockage refusé : la liste vaut pour cette séance seulement.
      }
    } catch {
      // Robot injoignable : on garde la copie.
    }
  },
}))

/** L'émotion d'une clé, si le robot l'a fait connaître. */
export const emotionDe = (emotions: EmotionConnue[], cle: string | null | undefined) =>
  cle ? emotions.find((e) => e.cle === cle) : undefined
