import { useEffect } from 'react'
import { Outlet } from 'react-router'
import { useWebSocketStore } from '../stores/websocketStore'
import { VideoProvider } from '../flux/VideoProvider'
import { useOrganesStore } from '../stores/organesStore'
import { useArretUrgenceStore } from '../stores/arretUrgenceStore'
import { BarreEtat } from '../../features/hud/BarreEtat'
import { Rail } from '../../features/hud/Rail'
import { ColonneDroite } from '../../features/hud/ColonneDroite'
import { TiroirReglages } from '../../features/hud/TiroirReglages'
import styles from './Layout.module.css'

/**
 * La coque du pilotage : ce qui ne sert qu'à conduire le robot — la vidéo, la barre d'état, la
 * colonne des volets.
 *
 * La vidéo surtout : elle décode une image après l'autre et sature son tampon dès qu'on cesse de
 * la consommer. L'atelier et le studio, qui s'ouvrent robot éteint, ne la paient plus. Montrer et
 * cacher un volet ne démonte toujours rien tant qu'on reste en pilotage.
 */
export function CoqueRobot() {
  const connecte = useWebSocketStore((s) => s.connected)
  const chargerOrganes = useOrganesStore((s) => s.charger)
  const chargerArretUrgence = useArretUrgenceStore((s) => s.charger)

  // Découverte de capacités relancée à chaque (re)connexion : inutile de faire cliquer
  // l'utilisateur alors que l'appli sait que le robot vient de répondre. L'arrêt d'urgence est relu
  // au même moment : il a pu être déclenché depuis la manette avant qu'on ouvre l'application, ou
  // pendant une coupure de liaison.
  useEffect(() => {
    if (!connecte) return
    chargerOrganes()
    chargerArretUrgence()
  }, [connecte, chargerOrganes, chargerArretUrgence])

  return (
    <>
      <VideoProvider />

      <BarreEtat />

      <div className={styles.corps}>
        <Rail coque="robot" />
        <div className={styles.vue}>
          <Outlet />
          {/* Le tiroir vit ici, et non dans la vue : ses réglages (palette, son) valent pour toute
              l'application. */}
          <TiroirReglages />
        </div>
        <ColonneDroite />
      </div>
    </>
  )
}
