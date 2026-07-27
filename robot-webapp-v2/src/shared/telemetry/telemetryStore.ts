import { create } from 'zustand'

const NB_ECHANTILLONS_HISTORIQUE = 30

interface TelemetryState {
  /** Dernière valeur connue par identifiant (mêmes identifiants que `/api/organes`). */
  valeurs: Record<string, number>
  /** Historique glissant par identifiant (derniers `NB_ECHANTILLONS_HISTORIQUE` échantillons). */
  historique: Record<string, number[]>
  /** Intègre un lot de valeurs reçues (fusion + poussée dans l'historique de chaque identifiant). */
  ingerer: (valeurs: Record<string, number>) => void
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

  ingerer(nouvellesValeurs) {
    const { valeurs, historique } = get()
    const prochainHistorique = { ...historique }
    for (const [id, valeur] of Object.entries(nouvellesValeurs)) {
      const serie = [...(prochainHistorique[id] ?? []), valeur]
      if (serie.length > NB_ECHANTILLONS_HISTORIQUE) serie.shift()
      prochainHistorique[id] = serie
    }
    set({ valeurs: { ...valeurs, ...nouvellesValeurs }, historique: prochainHistorique })
  },
}))
