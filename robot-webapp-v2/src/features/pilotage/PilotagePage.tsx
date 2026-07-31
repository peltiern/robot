import { useEffect, useRef, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { useHudStore } from '../../shared/hud/hudStore'
import { useVideoStore } from '../../shared/video/videoStore'
import { useArretUrgenceStore } from '../../shared/securite/arretUrgenceStore'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import type { DetectedBox } from '../../shared/types/events'
import { PanneauPosture } from './PanneauPosture'
import { BarreEcoute } from './BarreEcoute'
import styles from './pilotage.module.css'

/**
 * Vue de pilotage : l'image du robot en plein cadre, tout le reste posé dessus.
 *
 * C'est le principe du HUD — on ne quitte jamais l'image pour aller voir autre
 * chose. Les volets se montrent et se cachent depuis le rail, sans navigation.
 */
export function PilotagePage() {
  const connecte = useWebSocketStore((s) => s.connected)
  const trame = useVideoStore((s) => s.trame)
  const naturel = useVideoStore((s) => s.naturel)
  const poserNaturel = useVideoStore((s) => s.poserNaturel)
  const calques = useHudStore((s) => s.calques)
  const posture = useHudStore((s) => s.posture)
  const arretUrgence = useArretUrgenceStore((s) => s.actif)
  const origineArret = useArretUrgenceStore((s) => s.origine)

  const visages = trame?.faces ?? []
  const objets = trame?.objects ?? []

  const scene = useRef<HTMLElement>(null)
  const place = useTaille(scene)
  const cadre = naturel && place ? dimensionsContenues(naturel, place) : null

  return (
    <section className={styles.scene} ref={scene}>
      {trame ? (
        <div
          className={styles.cadre}
          style={cadre ? { width: cadre.w, height: cadre.h } : undefined}
        >
          <img
            className={styles.image}
            src={trame.url}
            alt="Flux caméra du robot"
            onLoad={(e) =>
              poserNaturel({
                w: e.currentTarget.naturalWidth,
                h: e.currentTarget.naturalHeight,
              })
            }
          />
          {calques && naturel && (
            <div className={styles.calques}>
              {visages.map((boite, i) => (
                <Boite key={`v${i}`} boite={boite} naturel={naturel} />
              ))}
              {objets.map((boite, i) => (
                <Boite key={`o${i}`} boite={boite} naturel={naturel} objet />
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className={styles.attente}>
          <Icone nom="camera" taille={56} />
          <p className={styles.attenteTitre}>
            {connecte ? 'En attente du flux vidéo…' : 'Robot déconnecté'}
          </p>
          <p className={styles.attenteAide}>
            {connecte
              ? 'Aucune trame reçue. La vision doit être activée sur le robot (robot.capteurs.vision.enabled) et le serveur de détection démarré.'
              : 'La connexion WebSocket au robot est interrompue.'}
          </p>
        </div>
      )}

      <div className={styles.calques}>
        {arretUrgence && (
          <div className={styles.bandeauArret}>
            <Icone nom="alerte" taille={26} />
            <div>
              <b>ARRÊT D'URGENCE</b>
              <span>
                Moteurs coupés{origineArret ? ` — déclenché depuis : ${origineArret}` : ''}. Aucun
                mouvement n'est accepté avant réarmement.
              </span>
            </div>
          </div>
        )}

        {/* Pas d'image, pas de compteurs : sans trame, « visages 0 » ne dit rien
            de ce que le robot voit — il dit seulement qu'on ne voit rien. */}
        {calques && trame && (
          <div className={`${styles.hudHaut} ${arretUrgence ? styles.hudHautDecale : ''}`}>
            <div className={`${styles.puce} ${styles.puceAccent}`}>
              <Icone nom="oeil" taille={18} />
              <span>VISION</span>
            </div>
            <div className={styles.pousse} />
            <div
              className={`${styles.puce} ${styles.puceVisages}`}
              title="Visages détectés"
              aria-label={`${visages.length} visage(s) détecté(s)`}
            >
              <Icone nom="visage" taille={19} />
              <b className="data">{visages.length}</b>
            </div>
            <div
              className={`${styles.puce} ${styles.puceObjets}`}
              title="Objets détectés"
              aria-label={`${objets.length} objet(s) détecté(s)`}
            >
              <Icone nom="objet" taille={19} />
              <b className="data">{objets.length}</b>
            </div>
          </div>
        )}

        {/* Bande du bas : la posture et l'écoute se partagent la largeur au lieu
            de se recouvrir quand la scène est étroite. */}
        <div className={styles.bas}>
          {posture && <PanneauPosture />}
          <BarreEcoute />
        </div>
      </div>
    </section>
  )
}

interface Taille {
  w: number
  h: number
}

/** Suit la taille d'un élément (redimensionnement de la fenêtre, volets ouverts/fermés…). */
function useTaille(ref: React.RefObject<HTMLElement | null>): Taille | null {
  const [taille, setTaille] = useState<Taille | null>(null)

  useEffect(() => {
    const element = ref.current
    if (!element) return
    const observateur = new ResizeObserver(([entree]) =>
      setTaille({ w: entree.contentRect.width, h: entree.contentRect.height }),
    )
    observateur.observe(element)
    return () => observateur.disconnect()
  }, [ref])

  return taille
}

/** Plus grande taille au format de la source qui tienne dans la place disponible. */
function dimensionsContenues(source: Taille, place: Taille): Taille {
  const format = source.w / source.h
  return place.w / place.h > format
    ? { w: place.h * format, h: place.h }
    : { w: place.w, h: place.w / format }
}

/**
 * Boîte de détection. Les coordonnées viennent en pixels de l'image source : on
 * les ramène en pourcentages du cadre, qui épouse l'image affichée — l'alignement
 * tient donc quelle que soit la taille de l'écran.
 */
function Boite({
  boite,
  naturel,
  objet = false,
}: {
  boite: DetectedBox
  naturel: { w: number; h: number }
  objet?: boolean
}) {
  return (
    <div
      className={`${styles.boite} ${objet ? styles.boiteObjet : ''}`}
      style={{
        left: `${(boite.x / naturel.w) * 100}%`,
        top: `${(boite.y / naturel.h) * 100}%`,
        width: `${(boite.width / naturel.w) * 100}%`,
        height: `${(boite.height / naturel.h) * 100}%`,
      }}
    >
      {boite.name && <span className={styles.boiteNom}>{boite.name}</span>}
    </div>
  )
}
