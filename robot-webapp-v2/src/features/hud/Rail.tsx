import { NavLink } from 'react-router'
import { Icone, type NomIcone } from '../../shared/components/Icone'
import { useHudStore, type Volets } from '../../shared/hud/hudStore'
import { useAudioStore } from '../../shared/audio/audioStore'
import styles from './hud.module.css'

const VOLETS: { cle: keyof Volets; icone: NomIcone; libelle: string }[] = [
  { cle: 'calques', icone: 'oeil', libelle: 'CALQUES' },
  { cle: 'posture', icone: 'posture', libelle: 'POSTURE' },
  { cle: 'dialogue', icone: 'bulle', libelle: 'DIALOG.' },
  { cle: 'vitaux', icone: 'jauge', libelle: 'VITAUX' },
]

/**
 * Rail d'outils. Les deux premiers boutons sont les seuls vrais onglets de
 * l'application ; tout le reste montre et cache des volets sans rien démonter —
 * les abonnements WebSocket ne sont jamais coupés par un geste d'affichage.
 *
 * Les bascules de volets ne concernent que le pilotage : dans l'atelier, la
 * scène est prise en entier par l'éditeur.
 */
export function Rail({ pilotage }: { pilotage: boolean }) {
  const volets = useHudStore()
  const basculerVolet = useHudStore((s) => s.basculerVolet)
  const reglages = useHudStore((s) => s.reglages)
  const ouvrirReglages = useHudStore((s) => s.ouvrirReglages)
  const muet = useAudioStore((s) => s.muet)
  const basculerSon = useAudioStore((s) => s.basculerSon)

  return (
    <nav className={styles.rail}>
      <NavLink
        to="/pilotage"
        className={({ isActive }) => `${styles.outil} ${isActive ? styles.outilActif : ''}`}
      >
        <Icone nom="camera" />
        <span>PILOTAGE</span>
      </NavLink>
      <NavLink
        to="/atelier"
        className={({ isActive }) => `${styles.outil} ${isActive ? styles.outilActif : ''}`}
      >
        <Icone nom="anim" />
        <span>ATELIER</span>
      </NavLink>

      <hr className={styles.separateur} />

      {pilotage &&
        VOLETS.map(({ cle, icone, libelle }) => (
          <button
            key={cle}
            className={`${styles.outil} ${volets[cle] ? styles.outilActif : ''}`}
            aria-pressed={volets[cle]}
            onClick={() => basculerVolet(cle)}
          >
            <Icone nom={icone} />
            <span>{libelle}</span>
          </button>
        ))}

      <button
        className={`${styles.outil} ${muet ? '' : styles.outilActif}`}
        aria-pressed={!muet}
        onClick={basculerSon}
        title={muet ? 'Écouter la voix du robot' : 'Couper le son'}
      >
        <Icone nom={muet ? 'sonCoupe' : 'son'} />
        <span>SON</span>
      </button>

      <div className={styles.pousse} />

      <button
        className={`${styles.outil} ${reglages ? styles.outilActif : ''}`}
        aria-pressed={reglages}
        onClick={() => ouvrirReglages(!reglages)}
      >
        <Icone nom="reglage" />
        <span>RÉGLAGES</span>
      </button>
    </nav>
  )
}
