import { create } from 'zustand'

/** Profondeur de l'historique conservé par mesure, en nombre d'échantillons. */
export const NB_ECHANTILLONS_HISTORIQUE = 30

interface TelemetryState {
  /** Dernière valeur connue par identifiant (mêmes identifiants que `/api/organes`). */
  valeurs: Record<string, number>
  /** Historique glissant par identifiant (derniers `NB_ECHANTILLONS_HISTORIQUE` échantillons). */
  historique: Record<string, number[]>
  /**
   * Intègre un lot d'échantillons reçus, dans l'ordre d'arrivée, en une seule mise à jour.
   *
   * Le lot est la clé : le WebSocket est fiable et ordonné, donc après un à-coup réseau tout le
   * retard est livré d'un coup. Ingérer chaque message séparément déclenchait un rendu par
   * message, et la page Monitoring rejouait visiblement la rafale — jauges et courbes défilaient
   * en accéléré. Aucun échantillon n'est pour autant écarté : l'historique reste complet, il est
   * simplement publié en un seul rendu, la jauge affichant directement la valeur la plus récente.
   */
  ingerer: (echantillons: Record<string, number>[]) => void
  /** Efface tout : plus aucune valeur connue, comme au lancement de l'application. */
  oublier: () => void
}

/**
 * Store de télémétrie partagé, alimenté en continu par `TelemetryProvider` (monté une seule fois
 * au niveau du Layout, donc vivant pendant toute la session — comme la connexion WebSocket
 * elle-même). Sans ça, l'historique vivrait dans l'état local de la page Monitoring et serait
 * perdu à chaque navigation (démontage du composant) : ce n'est qu'un affichage éphémère (pas une
 * donnée métier à faire persister côté robot), mais il doit survivre à la navigation dans la SPA.
 */
export const useTelemetryStore = create<TelemetryState>((set, get) => ({
  valeurs: {},
  historique: {},

  ingerer(echantillons) {
    if (echantillons.length === 0) return

    const { valeurs, historique } = get()
    const prochainesValeurs = { ...valeurs }
    const prochainHistorique = { ...historique }
    // Une seule copie par série touchée, quel que soit le nombre d'échantillons du lot.
    const seriesTouchees = new Set<string>()

    for (const echantillon of echantillons) {
      for (const [id, valeur] of Object.entries(echantillon)) {
        if (!seriesTouchees.has(id)) {
          prochainHistorique[id] = [...(prochainHistorique[id] ?? [])]
          seriesTouchees.add(id)
        }
        prochainHistorique[id].push(valeur)
        prochainesValeurs[id] = valeur
      }
    }

    for (const id of seriesTouchees) {
      const serie = prochainHistorique[id]
      if (serie.length > NB_ECHANTILLONS_HISTORIQUE) {
        prochainHistorique[id] = serie.slice(-NB_ECHANTILLONS_HISTORIQUE)
      }
    }

    set({ valeurs: prochainesValeurs, historique: prochainHistorique })
  },

  oublier() {
    set({ valeurs: {}, historique: {} })
  },
}))
