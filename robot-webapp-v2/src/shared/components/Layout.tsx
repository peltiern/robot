import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router'
import { useWebSocketStore } from '../websocket/websocketStore'
import { TelemetryProvider } from '../telemetry/TelemetryProvider'
import { ConversationProvider } from '../conversation/ConversationProvider'
import { AudioProvider } from '../audio/AudioProvider'
import { VideoProvider } from '../video/VideoProvider'
import { useOrganesStore } from '../organes/organesStore'
import { useArretUrgenceStore } from '../securite/arretUrgenceStore'
import { ArretUrgenceProvider } from '../securite/ArretUrgenceProvider'
import { SanteProvider } from '../sante/SanteProvider'
import { BarreEtat } from '../../features/hud/BarreEtat'
import { Rail } from '../../features/hud/Rail'
import { ColonneDroite } from '../../features/hud/ColonneDroite'
import { TiroirReglages } from '../../features/hud/TiroirReglages'
import styles from './Layout.module.css'

/**
 * Coque du HUD, vivante pendant toute la session : c'est ici que se branchent la
 * connexion WebSocket et tous les flux (télémétrie, conversation, audio, vidéo).
 * Les vues ne font que les afficher — changer d'onglet ne coupe donc jamais un
 * abonnement, et rien n'est perdu pendant qu'on est ailleurs.
 */
export function Layout() {
  const connect = useWebSocketStore((s) => s.connect)
  const disconnect = useWebSocketStore((s) => s.disconnect)
  const connecte = useWebSocketStore((s) => s.connected)
  const chargerOrganes = useOrganesStore((s) => s.charger)
  const chargerArretUrgence = useArretUrgenceStore((s) => s.charger)

  useEffect(() => {
    connect()
    return () => disconnect()
  }, [connect, disconnect])

  // Découverte de capacités relancée à chaque (re)connexion : inutile de faire
  // cliquer l'utilisateur alors que l'appli sait que le robot vient de répondre.
  // L'arrêt d'urgence est relu au même moment : il a pu être déclenché depuis la
  // manette avant qu'on ouvre l'application, ou pendant une coupure de liaison.
  useEffect(() => {
    if (!connecte) return
    chargerOrganes()
    chargerArretUrgence()
  }, [connecte, chargerOrganes, chargerArretUrgence])

  // L'atelier prend l'écran en entier : la colonne et les volets s'effacent.
  const pilotage = useLocation().pathname !== '/atelier'

  return (
    <div className={styles.app}>
      <TelemetryProvider />
      <ConversationProvider />
      <AudioProvider />
      <VideoProvider />
      <ArretUrgenceProvider />
      <SanteProvider />

      <BarreEtat />

      <div className={styles.corps}>
        <Rail pilotage={pilotage} />
        <div className={styles.vue}>
          <Outlet />
          {/* Le tiroir vit ici, et non dans la vue : ses réglages (palette, son)
              valent pour toute l'application, atelier compris. */}
          <TiroirReglages />
        </div>
        {pilotage && <ColonneDroite />}
      </div>
    </div>
  )
}
