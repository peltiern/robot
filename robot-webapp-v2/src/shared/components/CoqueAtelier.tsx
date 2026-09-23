import { Outlet } from 'react-router'
import { Rail } from '../../features/hud/Rail'
import { TiroirReglages } from '../../features/hud/TiroirReglages'
import styles from './Layout.module.css'

/**
 * La coque des établis : l'atelier d'animation, le studio son, le répertoire.
 *
 * On y travaille <b>avec ou sans robot</b>, sur un écran entier, à la souris. D'où ce qu'elle n'a
 * pas : ni flux vidéo, ni télémétrie, ni barre d'état, ni colonne de volets. Ce qui a besoin du
 * robot — jouer sur la tête, envoyer ce qui attend — le demande au moment où on le demande, et
 * s'en passe le reste du temps.
 *
 * Le fond et la connexion viennent de {@link Racine}, en amont : passer du pilotage à l'atelier ne
 * rouvre donc rien.
 */
export function CoqueAtelier() {
  return (
    <div className={styles.corps}>
      <Rail coque="atelier" />
      <div className={styles.vue}>
        <Outlet />
        <TiroirReglages />
      </div>
    </div>
  )
}
