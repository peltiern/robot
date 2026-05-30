/**
 * Gestion des couleurs de tracks.
 * Les couleurs sont assignées automatiquement depuis une palette de couleurs distinctes
 * et persistées en localStorage pour être stables entre les sessions.
 */

// Palette de couleurs bien distinctes visuellement
const PALETTE = [
  '#4fc3f7', // bleu ciel
  '#81c784', // vert
  '#ffb74d', // orange
  '#f06292', // rose
  '#ce93d8', // violet
  '#4db6ac', // teal
  '#ff8a65', // corail
  '#90a4ae', // gris-bleu
  '#e57373', // rouge doux
  '#fff176', // jaune
  '#a5d6a7', // vert pâle
  '#ef9a9a', // saumon
]

const STORAGE_KEY = 'robot:track-colors'

function load(): Record<string, string> {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}')
  } catch {
    return {}
  }
}

function save(colors: Record<string, string>): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(colors))
}

/**
 * Retourne un mapping id → couleur pour la liste de track IDs fournie.
 * - Les tracks déjà connus conservent leur couleur sauvegardée.
 * - Les nouveaux tracks reçoivent la prochaine couleur disponible de la palette.
 * - Le résultat est sauvegardé en localStorage.
 */
export function assignTrackColors(trackIds: string[]): Record<string, string> {
  const saved = load()
  const result: Record<string, string> = { ...saved }
  const usedColors = new Set(Object.values(result))

  // Index de départ = nombre de couleurs déjà attribuées (déterministe)
  let paletteIdx = Object.keys(result).length

  for (const id of trackIds) {
    if (result[id]) continue  // couleur déjà connue

    // Chercher la prochaine couleur non encore utilisée
    let assigned = PALETTE[paletteIdx % PALETTE.length]
    for (let i = 0; i < PALETTE.length; i++) {
      const candidate = PALETTE[(paletteIdx + i) % PALETTE.length]
      if (!usedColors.has(candidate)) {
        assigned = candidate
        break
      }
    }

    result[id] = assigned
    usedColors.add(assigned)
    paletteIdx++
  }

  save(result)
  return result
}

/** Réinitialise toutes les couleurs (utile pour les tests). */
export function resetTrackColors(): void {
  localStorage.removeItem(STORAGE_KEY)
}
