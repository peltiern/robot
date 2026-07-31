import { useCallback, useEffect, useMemo, useState, type CSSProperties } from 'react'
import { Icone, type NomIcone } from '../../shared/components/Icone'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useTelemetryStore } from '../../shared/telemetry/telemetryStore'
import { useVideoStore } from '../../shared/video/videoStore'
import { capteurs, useOrganesStore, type Mesure } from '../../shared/organes/organesStore'
import { evenementsRecentrage } from '../../shared/organes/commandes'
import { couleurAlerte, fractionMesure } from '../../shared/hud/seuils'
import styles from './hud.module.css'

/**
 * Mesures épinglées dans la barre, par identifiant de télémétrie. Une pastille
 * n'apparaît que si le robot publie effectivement la mesure : rien à changer
 * ici le jour où un capteur disparaît, et une ligne à ajouter le jour où une
 * tension de batterie apparaît.
 *
 * L'unité n'est qu'un secours : celle déclarée par le robot est préférée quand la
 * découverte de capacités a abouti.
 */
const EPINGLEES: { id: string; icone: NomIcone; unite: string }[] = [
  { id: 'temperatureCpu', icone: 'thermo', unite: '°C' },
  { id: 'cpuCharge', icone: 'jauge', unite: '%' },
]

/** Durée d'affichage de l'accusé de réception de l'arrêt. */
const ACCUSE_ARRET_MS = 1600

export function BarreEtat() {
  const connecte = useWebSocketStore((s) => s.connected)
  const envoyer = useWebSocketStore((s) => s.sendRobotEvent)
  const stopAnimation = useWebSocketStore((s) => s.stopAnimation)
  const valeurs = useTelemetryStore((s) => s.valeurs)
  const fps = useVideoStore((s) => s.fps)
  const organes = useOrganesStore((s) => s.organes)

  // Échelles déclarées par le robot, pour colorer les pastilles selon les mêmes
  // seuils que les anneaux du volet Vitaux.
  const echelles = useMemo(
    () =>
      Object.fromEntries(
        capteurs(organes)
          .flatMap((o) => o.mesures)
          .map((m) => [m.id, m]),
      ) as Record<string, Mesure>,
    [organes],
  )

  const [arretEnvoye, setArretEnvoye] = useState(false)

  useEffect(() => {
    if (!arretEnvoye) return
    const minuterie = window.setTimeout(() => setArretEnvoye(false), ACCUSE_ARRET_MS)
    return () => clearTimeout(minuterie)
  }, [arretEnvoye])

  /**
   * Arrêt : coupe l'animation en cours et ramène toutes les articulations à 0°.
   *
   * Ce n'est pas (encore) un arrêt d'urgence matériel : le robot n'expose rien
   * pour désengager les servos — son seul `StopEvent` éteint carrément
   * l'application, ce qui n'a rien à faire derrière un bouton de HUD. C'est donc
   * le maximum atteignable depuis le front : plus rien ne bouge, tout revient au
   * neutre.
   */
  const arreter = useCallback(() => {
    stopAnimation()
    for (const evenement of evenementsRecentrage(organes)) envoyer(evenement)
    setArretEnvoye(true)
  }, [stopAnimation, envoyer, organes])

  return (
    <header className={styles.bar}>
      <div className={styles.marque}>
        <YeuxEtat connecte={connecte} />
        <div className={`${styles.mot} titraille`}>
          WALL·E
          <small>{window.location.host}</small>
        </div>
      </div>

      <div className={styles.pastilles}>
        {EPINGLEES.filter((m) => valeurs[m.id] != null).map((epinglee) => {
          const echelle = echelles[epinglee.id]
          const alerte = echelle
            ? couleurAlerte(fractionMesure(valeurs[epinglee.id], echelle.min, echelle.max))
            : null
          return (
            <div
              key={epinglee.id}
              // `--teinte` habille le nombre, le contour et le fond — seulement à
              // partir de la veille. Au repos, ou sans échelle connue (robot pas
              // encore découvert, débit vidéo), la pastille reste neutre.
              className={`${styles.pastille} ${alerte ? styles.pastilleMesuree : ''}`}
              style={alerte ? ({ '--teinte': alerte } as CSSProperties) : undefined}
            >
              <Icone nom={epinglee.icone} taille={20} />
              <span className={`${styles.pastilleVal} data`}>
                {Math.round(valeurs[epinglee.id])}
              </span>
              <span className={styles.pastilleUnite}>{echelle?.unite ?? epinglee.unite}</span>
            </div>
          )
        })}
        <div className={styles.pastille}>
          <Icone nom="camera" taille={20} />
          <span className={`${styles.pastilleVal} data`}>{fps}</span>
          <span className={styles.pastilleUnite}>fps</span>
        </div>
      </div>

      <div className={styles.pousse} />

      <div className={`${styles.etat} ${connecte ? '' : styles.etatCoupe}`}>
        <i className={styles.led} />
        <span>{connecte ? 'EN LIGNE' : 'HORS LIGNE'}</span>
      </div>

      <button
        className={styles.stop}
        onClick={arreter}
        disabled={!connecte}
        title="Coupe l'animation en cours et recentre toutes les articulations à 0°"
      >
        <Icone nom="stop" taille={22} />
        <span>{arretEnvoye ? 'ARRÊTÉ' : 'STOP'}</span>
      </button>
    </header>
  )
}

/**
 * Les yeux du robot font office de voyant de connexion : iris cyan et clignement
 * quand la liaison est là, iris éteint sinon. Ils suivent du regard le premier
 * visage détecté — le robot regarde ce qu'il voit.
 */
function YeuxEtat({ connecte }: { connecte: boolean }) {
  const visage = useVideoStore((s) => s.trame?.faces[0])
  const naturel = useVideoStore((s) => s.naturel)

  // Décalage horizontal des iris, en unités du viewBox (±3 px de débattement).
  let dx = 0
  if (connecte && visage && naturel) {
    const centre = (visage.x + visage.width / 2) / naturel.w
    dx = Math.max(-1, Math.min(1, (centre - 0.5) * 2)) * 3
  }

  return (
    <svg
      className={`${styles.yeux} ${connecte ? '' : styles.horsLigne}`}
      viewBox="0 0 54 34"
      role="img"
      aria-label={connecte ? 'Robot connecté' : 'Robot déconnecté'}
    >
      <g className={styles.cligne}>
        <ellipse className={styles.globe} cx="15" cy="17" rx="13" ry="15" />
        <ellipse className={styles.globe} cx="39" cy="17" rx="13" ry="15" />
        <circle className={styles.iris} cx={15 + dx} cy="17" r="6" />
        <circle className={styles.iris} cx={39 + dx} cy="17" r="6" />
      </g>
    </svg>
  )
}
