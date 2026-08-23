import { Icone } from '../../shared/components/Icone'
import { useHudStore } from '../../shared/stores/hudStore'
import { useThemeStore, PALETTES } from '../../shared/stores/themeStore'
import { useAudioStore } from '../../shared/stores/audioStore'
import { useOrganesStore } from '../../shared/stores/organesStore'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import styles from './hud.module.css'

/**
 * Tiroir des réglages : ce qui se règle depuis la tablette, c'est-à-dire ce qui
 * vit dans le navigateur (apparence, son) et la liaison au robot.
 *
 * Les réglages du robot lui-même — moteur de reconnaissance vocale, voix,
 * qualité vidéo — ne sont volontairement pas là : ils se règlent aujourd'hui
 * dans `robot.properties`, et le backend n'expose aucune API pour les changer à
 * chaud. Ils viendront ici le jour où cette API existera.
 */
export function TiroirReglages() {
  const ouvert = useHudStore((s) => s.reglages)
  const ouvrirReglages = useHudStore((s) => s.ouvrirReglages)
  const palette = useThemeStore((s) => s.palette)
  const choisirPalette = useThemeStore((s) => s.choisirPalette)
  const muet = useAudioStore((s) => s.muet)
  const basculerSon = useAudioStore((s) => s.basculerSon)
  const connecte = useWebSocketStore((s) => s.connected)
  const charger = useOrganesStore((s) => s.charger)

  return (
    <aside
      className={`${styles.tiroir} ${ouvert ? styles.tiroirOuvert : ''} verre`}
      aria-hidden={!ouvert}
    >
      <div className={styles.tiroirTitre}>
        <span className="eyebrow">Réglages</span>
        <button className={styles.fermer} onClick={() => ouvrirReglages(false)} aria-label="Fermer">
          <Icone nom="croix" taille={20} />
        </button>
      </div>

      <div className={styles.reglage}>
        <span className={styles.reglageLibelle}>Palette</span>
        <div className={styles.segments}>
          {PALETTES.map((p) => (
            <button
              key={p.id}
              className={palette === p.id ? styles.segmentActif : ''}
              aria-pressed={palette === p.id}
              onClick={() => choisirPalette(p.id)}
            >
              {p.libelle.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      <div className={styles.reglage}>
        <span className={styles.reglageLibelle}>Voix du robot</span>
        <div className={styles.segments}>
          <button
            className={muet ? styles.segmentActif : ''}
            aria-pressed={muet}
            onClick={() => muet || basculerSon()}
          >
            MUET
          </button>
          <button
            className={muet ? '' : styles.segmentActif}
            aria-pressed={!muet}
            onClick={() => muet && basculerSon()}
          >
            AUDIBLE
          </button>
        </div>
      </div>

      <div className={styles.reglage}>
        <span className={styles.reglageLibelle}>Liaison</span>
        <input className={`${styles.champ} data`} value={window.location.host} readOnly />
        <button className={styles.relancer} onClick={charger} disabled={!connecte}>
          RELIRE LA CONFIGURATION DU ROBOT
        </button>
      </div>

      <p className={styles.note}>
        Reconnaissance vocale, voix de synthèse et qualité vidéo se règlent côté robot
        (<span className="data">robot.properties</span>) : le backend ne sait pas encore les
        changer à chaud.
      </p>
    </aside>
  )
}
