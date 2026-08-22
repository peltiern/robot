import { useEffect, useRef } from 'react'
import { usePersonnesStore } from '../../shared/personnes/personnesStore'
import type { ChatMessage } from '../../shared/conversation/conversationStore'

/**
 * Tient le répertoire à jour de ce que le fil demande.
 *
 * Le HUD n'ouvre jamais la page des personnes : sans cela le store resterait vide et toutes les
 * bulles porteraient une initiale. Et la liste ne suffit pas à être chargée une fois pour toutes —
 * le robot fait connaissance en cours de route, et celle qu'il vient d'apprendre parle dans la
 * minute qui suit.
 *
 * Rien n'est demandé tant qu'aucune bulle ne nomme quelqu'un d'introuvable. La première version
 * réclamait la liste dès le montage : le HUD ouvert robot éteint — le cas de tous les démarrages —
 * partait aussitôt sur un appel voué au refus de connexion. Or il n'y a alors pas la moindre bulle
 * à illustrer, donc rien à aller chercher.
 *
 * Un identifiant n'est cherché qu'une fois, sans quoi une personne effacée depuis ferait boucler
 * la tablette sur le robot. Mais il n'est tenu pour cherché que si le robot a répondu : autrement
 * une coupure au mauvais moment condamnerait ces portraits jusqu'au rechargement de la page.
 */
export function useRepertoirePourLeFil(messages: ChatMessage[]) {
  const liste = usePersonnesStore((s) => s.liste)
  const charger = usePersonnesStore((s) => s.charger)
  const dejaCherches = useRef(new Set<string>())
  const enCours = useRef(false)

  useEffect(() => {
    if (enCours.current) return

    const aResoudre = messages
      .map((message) => message.idPersonne)
      .filter((id): id is string => !!id && !dejaCherches.current.has(id))
      .filter((id) => !liste?.some((personne) => personne.id === id))
    if (aResoudre.length === 0) return

    enCours.current = true
    void charger().finally(() => {
      enCours.current = false
      if (usePersonnesStore.getState().erreur === null) {
        aResoudre.forEach((id) => dejaCherches.current.add(id))
      }
    })
  }, [messages, liste, charger])
}
