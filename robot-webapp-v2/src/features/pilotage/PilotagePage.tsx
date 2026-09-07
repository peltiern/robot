import { useEffect, useRef, useState } from 'react'
import { Icone } from '../../shared/components/Icone'
import { useHudStore } from '../../shared/stores/hudStore'
import { useVideoStore } from '../../shared/stores/videoStore'
import { useArretUrgenceStore } from '../../shared/stores/arretUrgenceStore'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import type { DetectedBox } from '../../shared/types/events'
import {
  regardApi,
  demiCoteZoneMorte,
  centreViseXRelatif,
  type ReglagesRegard,
} from '../../shared/api/regardApi'
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

  // Réglages relus au montage : ces clés sont rechargées à chaud sur le robot, mais une page de
  // pilotage se rouvre bien plus souvent qu'on ne les édite. L'échec ne remonte pas au HUD — sans
  // le repère la vue reste utilisable, et une alerte de plus sur un écran déjà chargé n'aiderait
  // pas — mais il est dit dans la console : un repère absent sans raison affichée est
  // indiscernable d'un robot qui aurait le suivi éteint, et on cherche alors du mauvais côté.
  const [regard, poserRegard] = useState<ReglagesRegard | null>(null)
  useEffect(() => {
    regardApi
      .reglages()
      .then(poserRegard)
      .catch((erreur) => {
        console.warn('Réglages du regard indisponibles, zone morte non dessinée :', erreur)
        poserRegard(null)
      })
  }, [])

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
              {regard?.actif && <ZoneMorte reglages={regard} naturel={naturel} />}
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
/**
 * La zone morte du regard, dessinée là où le robot la place réellement.
 *
 * Un visage dont le centre tombe dedans est considéré « déjà regardé » : aucune consigne ne part.
 * C'est ce qui explique qu'un robot qui vise visiblement à côté puisse ne rien corriger, et sans ce
 * repère on n'a que l'impression — on ne peut pas dire si la tête court après quelqu'un ou si elle
 * a décidé d'avoir fini.
 *
 * **Un carré, centré là où le robot vise vraiment.** Les choix viennent du code de `Regard`, pas
 * de l'esthétique : il compte ses écarts depuis le point principal mesuré à l'étalonnage (et non
 * depuis le milieu de l'image, dont il s'écarte d'une trentaine de pixels vers le haut), il décale
 * encore le panoramique du déport de la webcam, et il juge les deux axes séparément — la région où
 * il ne commande rien est donc l'intersection de deux bandes, un carré. Dessiner un disque centré
 * sur l'image montrerait un repère faux à l'endroit précis où l'on vient chercher la vérité.
 */
function ZoneMorte({
  reglages,
  naturel,
}: {
  reglages: ReglagesRegard
  naturel: { w: number; h: number }
}) {
  const demiCote = demiCoteZoneMorte(reglages, naturel.w)
  const centreX = centreViseXRelatif(reglages, naturel.w)
  return (
    <div
      className={styles.zoneMorte}
      style={{
        left: `${(centreX - demiCote / naturel.w) * 100}%`,
        top: `${(reglages.centreYRelatif - demiCote / naturel.h) * 100}%`,
        width: `${((2 * demiCote) / naturel.w) * 100}%`,
        height: `${((2 * demiCote) / naturel.h) * 100}%`,
      }}
      title={`Zone morte du regard : ${reglages.zoneMorteDegres}° autour de l'axe optique`}
    >
      <span className={styles.zoneMorteNom}>{reglages.zoneMorteDegres}°</span>
    </div>
  )
}

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
