import { useCallback, useEffect, useRef } from 'react'
import { useTopic } from './useTopic'

/**
 * S'abonne à un topic STOMP et rend l'évènement déjà décodé.
 *
 * Le décodage était recopié dans chaque flux : même `try`/`catch` autour de `JSON.parse`, même
 * garde sur le champ attendu, même abandon silencieux. Une trame tronquée ou un champ manquant ne
 * doit jamais faire tomber le HUD — le message suivant arrive de toute façon dans la fraction de
 * seconde.
 *
 * @param garde ce qui rend un évènement exploitable ; sans elle, tout ce qui se décode passe
 */
export function useEvenement<T>(
  topic: string,
  action: (evenement: T) => void,
  garde?: (evenement: T) => boolean,
) {
  // `useTopic` capture son handler par référence : une fonction recréée à chaque rendu ne
  // provoque donc aucun ré-abonnement.
  useTopic(topic, (msg) => {
    let evenement: T
    try {
      evenement = JSON.parse(msg.body)
    } catch {
      return
    }
    if (!evenement) return
    if (garde && !garde(evenement)) return
    action(evenement)
  })
}

/**
 * Accumule ce qui arrive et ne le livre qu'une fois par image écran.
 *
 * Le WebSocket est fiable et ordonné : après un à-coup réseau, tout le retard est livré d'un coup.
 * Un rendu par message enchaîne alors autant de défilements ou de mises à jour de courbe. Rien
 * n'est perdu, tout est simplement ingéré en une seule fois.
 *
 * @param maximum profondeur au-delà de laquelle les plus anciens sont écartés — un onglet en
 *                arrière-plan suspend les `requestAnimationFrame`, et la file grossirait sans fin
 */
export function useLotParFrame<T>(ingerer: (lot: T[]) => void, maximum: number) {
  const enAttente = useRef<T[]>([])
  const rafEnCours = useRef<number | null>(null)

  // `ingerer` vient d'un store zustand : son identité est stable, en dépendre ne recrée rien.
  const vider = useCallback(() => {
    rafEnCours.current = null
    const lot = enAttente.current
    enAttente.current = []
    if (lot.length > 0) ingerer(lot)
  }, [ingerer])

  const empiler = useCallback(
    (element: T) => {
      enAttente.current.push(element)
      if (enAttente.current.length > maximum) {
        enAttente.current = enAttente.current.slice(-maximum)
      }
      if (rafEnCours.current === null) {
        rafEnCours.current = requestAnimationFrame(vider)
      }
    },
    [maximum, vider],
  )

  /** Jette ce qui attend sans l'ingérer : ce qu'il faut faire quand la liaison tombe. */
  const jeter = useCallback(() => {
    if (rafEnCours.current !== null) {
      cancelAnimationFrame(rafEnCours.current)
      rafEnCours.current = null
    }
    enAttente.current = []
  }, [])

  useEffect(() => {
    return () => {
      if (rafEnCours.current !== null) cancelAnimationFrame(rafEnCours.current)
    }
  }, [])

  return { empiler, jeter }
}
