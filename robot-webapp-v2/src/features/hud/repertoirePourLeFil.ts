import { useEffect, useRef } from 'react'
import { usePersonnesStore } from '../../shared/personnes/personnesStore'
import type { ChatMessage } from '../../shared/conversation/conversationStore'

/**
 * Tient le répertoire à jour de ce que le fil demande.
 *
 * Le HUD n'ouvre jamais la page des personnes, et le robot fait connaissance en cours de route :
 * la liste ne peut donc être ni ignorée, ni chargée une fois pour toutes.
 *
 * Rien n'est demandé tant qu'aucune bulle ne nomme quelqu'un d'introuvable — le HUD ouvert robot
 * éteint, le cas de tous les démarrages, n'a aucune bulle à illustrer. Un identifiant n'est
 * cherché qu'une fois, sinon une personne effacée ferait boucler la tablette ; mais il n'est tenu
 * pour cherché que si le robot a répondu, sinon une coupure condamnerait ces portraits jusqu'au
 * rechargement de la page.
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
