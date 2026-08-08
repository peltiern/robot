import { useCallback, useEffect } from 'react'
import { useTopic } from '../websocket/useTopic'
import { useWebSocketStore } from '../websocket/websocketStore'
import { useSanteStore, type SanteOrgane } from './santeStore'

/**
 * Composant sans rendu : suit l'état vital des organes sur `/events/sante-organes`.
 *
 * Monté au niveau du Layout, comme les autres flux : l'état vaut pour toute l'application, et
 * changer d'onglet ne doit pas couper la seule chose qui dise qu'un organe est mort.
 */
export function SanteProvider() {
  const appliquer = useSanteStore((s) => s.appliquer)
  const oublier = useSanteStore((s) => s.oublier)
  const connecte = useWebSocketStore((s) => s.connected)

  useTopic(
    '/events/sante-organes',
    useCallback(
      (msg) => {
        let evenement: { organes?: SanteOrgane[] }
        try {
          evenement = JSON.parse(msg.body)
        } catch {
          return
        }
        if (!Array.isArray(evenement?.organes)) return
        appliquer(evenement.organes)
      },
      [appliquer],
    ),
  )

  useEffect(() => {
    if (!connecte) oublier()
  }, [connecte, oublier])

  return null
}
