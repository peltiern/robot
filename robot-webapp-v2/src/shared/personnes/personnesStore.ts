import { create } from 'zustand'
import { personneApi, type Personne } from '../api/personneApi'

/**
 * Le répertoire du robot, côté interface.
 *
 * Dans un store et non dans la page, comme le reste de l'application : la liste
 * et la fiche ouverte se répondent — renommer quelqu'un doit se voir dans la
 * liste, le supprimer doit refermer sa fiche — et deux composants qui se
 * synchronisent par des rappels finissent toujours par se désaccorder.
 *
 * Rien ici ne vient du WebSocket : ces données ne changent qu'à la suite d'une
 * rencontre, et une liste qui se réordonne toute seule pendant qu'on clique
 * dessus est une plaie. Le rechargement est demandé, jamais subi.
 */
interface PersonnesState {
  /** `null` tant que rien n'a été lu : à distinguer d'une liste vide. */
  liste: Personne[] | null
  /** Fiche ouverte, complète (timeline et conversation comprises). */
  fiche: Personne | null
  /** Identifiant sélectionné, posé avant même que la fiche n'arrive. */
  selection: string | null
  erreur: string | null

  charger: () => Promise<void>
  choisir: (id: string | null) => Promise<void>
  renommer: (id: string, prenom: string) => Promise<void>
  supprimer: (id: string) => Promise<void>
  oublierLaConversation: (id: string) => Promise<void>
  creer: (prenom: string, photos: File[]) => Promise<boolean>
  ajouterDesVisages: (id: string, photos: File[]) => Promise<boolean>
}

const message = (e: unknown) => (e instanceof Error ? e.message : 'robot injoignable')

export const usePersonnesStore = create<PersonnesState>((set, get) => ({
  liste: null,
  fiche: null,
  selection: null,
  erreur: null,

  async charger() {
    try {
      const liste = await personneApi.toutes()
      // La personne ouverte a pu disparaître entre-temps (suppression depuis une
      // autre tablette, base réamorcée) : on ne garde une sélection que si elle
      // désigne encore quelqu'un.
      const selection = get().selection
      const survit = selection !== null && liste.some((p) => p.id === selection)
      set({
        liste,
        erreur: null,
        selection: survit ? selection : null,
        fiche: survit ? get().fiche : null,
      })

      // La fiche ouverte est relue elle aussi. Sans cela, rafraîchir mettait à jour
      // la liste et laissait la timeline et la conversation dans l'état où elles
      // étaient — or c'est justement ce qu'on regarde, et le bouton semblait donc
      // n'avoir aucun effet. La fiche n'est pas vidée avant : la remplacer une fois
      // arrivée évite de faire clignoter « Lecture de la fiche… » à chaque clic.
      if (survit && selection !== null) {
        const fiche = await personneApi.fiche(selection)
        if (get().selection === selection) set({ fiche })
      }
    } catch (e) {
      set({ erreur: message(e) })
    }
  },

  async choisir(id) {
    set({ selection: id, fiche: null, erreur: null })
    if (id === null) return
    try {
      const fiche = await personneApi.fiche(id)
      // Un clic plus rapide que le réseau : si la sélection a changé pendant
      // l'aller-retour, cette fiche n'est plus celle qu'on regarde.
      if (get().selection === id) set({ fiche })
    } catch (e) {
      set({ erreur: message(e) })
    }
  },

  async renommer(id, prenom) {
    try {
      const fiche = await personneApi.renommer(id, prenom)
      set({ fiche, erreur: null })
      await get().charger()
    } catch (e) {
      set({ erreur: message(e) })
    }
  },

  async supprimer(id) {
    try {
      await personneApi.supprimer(id)
      set({ selection: null, fiche: null, erreur: null })
      await get().charger()
    } catch (e) {
      set({ erreur: message(e) })
    }
  },

  async oublierLaConversation(id) {
    try {
      await personneApi.oublierLaConversation(id)
      await get().choisir(id)
    } catch (e) {
      set({ erreur: message(e) })
    }
  },

  /**
   * Crée quelqu'un depuis des photos, et ouvre sa fiche dans la foulée : on vient
   * de l'ajouter, c'est elle qu'on veut regarder.
   *
   * @returns vrai si le robot a accepté — le formulaire ne se referme que dans ce
   *          cas, sinon on perdrait la saisie en même temps qu'on affiche l'erreur
   */
  async creer(prenom, photos) {
    try {
      const creee = await personneApi.creer(prenom, photos)
      set({ selection: creee.id, fiche: creee, erreur: null })
      await get().charger()
      return true
    } catch (e) {
      set({ erreur: message(e) })
      return false
    }
  },

  async ajouterDesVisages(id, photos) {
    try {
      const fiche = await personneApi.ajouterDesVisages(id, photos)
      set({ fiche, erreur: null })
      await get().charger()
      return true
    } catch (e) {
      set({ erreur: message(e) })
      return false
    }
  },
}))
