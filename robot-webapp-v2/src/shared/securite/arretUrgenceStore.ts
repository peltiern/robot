import { create } from 'zustand'
import { useWebSocketStore } from '../websocket/websocketStore'

/**
 * Arrêt d'urgence des moteurs.
 *
 * Le robot est seul maître de cet état : le front ne le décide jamais de lui-même, il envoie une
 * demande et attend le `/events/arret-urgence` en retour. Ça garantit que la tablette montre ce
 * que le robot fait, et non ce qu'on lui a demandé de faire — la nuance compte quand le geste
 * consiste justement à tout figer.
 *
 * L'état de départ vient de `GET /api/arret-urgence` (rechargé à chaque connexion) : une tablette
 * qui arrive après coup doit savoir que le robot est déjà arrêté, sinon elle l'affiche comme
 * normal alors qu'il refuse tout mouvement.
 */
interface ArretUrgenceState {
  actif: boolean
  /** Qui a déclenché (« manette », « interface »…), tel que rapporté par le robot. */
  origine: string | null
  /** Recharge l'état auprès du robot. */
  charger: () => void
  /** Applique l'état reçu du robot. */
  appliquer: (actif: boolean, origine: string | null) => void
  /** Demande le déclenchement ou le réarmement. */
  demander: (actif: boolean) => void
}

export const useArretUrgenceStore = create<ArretUrgenceState>((set) => ({
  actif: false,
  origine: null,

  charger() {
    fetch('/api/arret-urgence')
      .then((r) => (r.ok ? (r.json() as Promise<{ actif: boolean }>) : Promise.reject()))
      .then((etat) => set({ actif: etat.actif }))
      // Échec silencieux : l'évènement suivant remettra l'état d'aplomb, et afficher une erreur
      // de plus sur un robot déjà injoignable n'aide personne.
      .catch(() => undefined)
  },

  appliquer(actif, origine) {
    set({ actif, origine })
  },

  demander(actif) {
    useWebSocketStore.getState().sendRobotEvent({
      eventType: 'arret-urgence',
      actif,
      origine: 'interface',
    })
  },
}))
