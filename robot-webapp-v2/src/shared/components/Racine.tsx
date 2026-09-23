import { useEffect } from 'react'
import { Outlet } from 'react-router'
import { useWebSocketStore } from '../stores/websocketStore'
import { FluxRobot } from '../flux/FluxRobot'
import styles from './Layout.module.css'

/**
 * Ce que toutes les vues ont en commun : le fond, et la connexion au robot.
 *
 * La connexion vit ici et non dans les coques, pour qu'elle survive au passage du pilotage à
 * l'atelier — une coque par vue la couperait et la rouvrirait à chaque aller-retour. Elle se
 * contente de réessayer quand le robot est éteint ; c'est aussi le signal dont les éditeurs se
 * servent pour dire « robot injoignable » et pour lui envoyer ce qu'ils ont gardé.
 *
 * Les flux légers — télémétrie, dialogue, arrêt d'urgence — restent ici pour la même raison : le
 * panneau Dialogue promet qu'un aller-retour par l'atelier ne fait pas perdre un échange, et un
 * abonnement démonté perdrait ce qui arrive pendant l'absence. Ce qui coûte vraiment, c'est la
 * <b>vidéo</b> : elle est montée par la seule {@link CoqueRobot}, et l'atelier ne la paie plus.
 */
export function Racine() {
  const connect = useWebSocketStore((s) => s.connect)
  const disconnect = useWebSocketStore((s) => s.disconnect)

  useEffect(() => {
    connect()
    return () => disconnect()
  }, [connect, disconnect])

  return (
    <div className={styles.app}>
      <FluxRobot />
      <Outlet />
    </div>
  )
}
