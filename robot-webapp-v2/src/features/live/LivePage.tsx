import { useCallback, useEffect, useRef, useState } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { useTopic } from '../../shared/websocket/useTopic'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useAudioStream } from './useAudioStream'
import type { DetectedBox, VideoEvent } from '../../shared/types/events'
import styles from './LivePage.module.css'

/** Intervalle minimal entre deux rafraîchissements de l'indicateur FPS. */
const PERIODE_MAJ_FPS_MS = 500

/** Trame prête à l'affichage : l'image est déjà un Blob URL, plus une chaîne base64. */
interface TrameAffichee {
  url: string
  faces: DetectedBox[]
  objects: DetectedBox[]
}

/** Décode une chaîne base64 en Uint8Array (sans dépendance externe). */
function base64ToBytes(b64: string): Uint8Array<ArrayBuffer> {
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}

export function LivePage() {
  const connected = useWebSocketStore((s) => s.connected)
  const { muted, toggleMute } = useAudioStream()
  const [frame, setFrame] = useState<TrameAffichee | null>(null)
  const [natural, setNatural] = useState<{ w: number; h: number } | null>(null)
  const [fps, setFps] = useState(0)

  // Fenêtre glissante des horodatages d'affichage pour estimer le FPS.
  const stamps = useRef<number[]>([])
  const derniereMajFps = useRef(0)

  // Dernière trame reçue, en attente d'affichage, et rAF programmé pour l'afficher.
  const trameEnAttente = useRef<VideoEvent | null>(null)
  const rafEnCours = useRef<number | null>(null)

  /**
   * Affiche la trame la plus récente reçue, au rythme du navigateur.
   *
   * Le WebSocket est fiable et ordonné : après un à-coup réseau, tout le retard est livré
   * d'un coup. Afficher chaque message de la rafale fait défiler la vidéo en accéléré et
   * décode une pile d'images déjà périmées. On ne garde donc que la dernière : le rattrapage
   * se fait en sautant les images, comme n'importe quel flux live.
   */
  const afficherTrameEnAttente = useCallback(() => {
    rafEnCours.current = null
    const event = trameEnAttente.current
    trameEnAttente.current = null
    if (!event) return

    // Blob URL plutôt que `data:image/jpeg;base64,…` : une data-URI oblige le navigateur à
    // ré-analyser puis mettre en cache une chaîne de ~100 Ko à chaque trame, ce qui saccadait
    // l'affichage et faisait enfler la mémoire. Le blob est libéré dès la trame suivante.
    // Le décodage base64 n'a lieu que pour les trames réellement affichées.
    const url = URL.createObjectURL(
      new Blob([base64ToBytes(event.imageBase64)], { type: 'image/jpeg' })
    )
    setFrame({ url, faces: event.faces ?? [], objects: event.objects ?? [] })

    // FPS des images affichées (et non reçues) : c'est ce que l'œil voit, et ça ne s'envole
    // plus à chaque rafale. L'état n'est rafraîchi que deux fois par seconde, sinon chaque
    // trame provoquerait un second rendu complet.
    const now = performance.now()
    const s = stamps.current
    s.push(now)
    while (s.length > 0 && now - s[0] > 1000) s.shift()
    if (derniereMajFps.current === 0) {
      // Première trame : on amorce la fenêtre sans publier un « 1 fps » trompeur.
      derniereMajFps.current = now
    } else if (now - derniereMajFps.current >= PERIODE_MAJ_FPS_MS) {
      derniereMajFps.current = now
      setFps(s.length)
    }
  }, [])

  const onVideo = useCallback(
    (msg: IMessage) => {
      let event: VideoEvent
      try {
        event = JSON.parse(msg.body)
      } catch {
        return
      }
      if (!event?.imageBase64) return

      // La trame précédente non encore affichée est simplement écrasée.
      trameEnAttente.current = event
      if (rafEnCours.current === null) {
        rafEnCours.current = requestAnimationFrame(afficherTrameEnAttente)
      }
    },
    [afficherTrameEnAttente]
  )

  useTopic('/video', onVideo)

  // Annulation du rAF en attente au démontage.
  useEffect(() => {
    return () => {
      if (rafEnCours.current !== null) cancelAnimationFrame(rafEnCours.current)
    }
  }, [])

  // Libération du Blob URL précédent : le nettoyage s'exécute quand l'URL change, donc une
  // fois la nouvelle image posée dans le DOM (jamais pendant que l'<img> l'affiche encore).
  useEffect(() => {
    const url = frame?.url
    return () => {
      if (url) URL.revokeObjectURL(url)
    }
  }, [frame?.url])

  const faces = frame?.faces ?? []
  const objects = frame?.objects ?? []

  // La taille source ne change qu'au (re)démarrage du flux : on ne remplace l'état que si
  // elle diffère vraiment, sinon chaque `onLoad` (une fois par trame) relançait un rendu.
  const onImageLoad = useCallback((img: HTMLImageElement) => {
    setNatural((precedent) =>
      precedent?.w === img.naturalWidth && precedent?.h === img.naturalHeight
        ? precedent
        : { w: img.naturalWidth, h: img.naturalHeight }
    )
  }, [])

  return (
    <div className={styles.page}>
      <div className={styles.stage}>
        {frame ? (
          <div className={styles.frameWrap}>
            <img
              className={styles.image}
              src={frame.url}
              alt="Flux caméra du robot"
              onLoad={(e) => onImageLoad(e.currentTarget)}
            />
            {natural && (
              <div className={styles.overlay}>
                {faces.map((f, i) => (
                  <Box key={`f${i}`} box={f} natural={natural} variant="face" />
                ))}
                {objects.map((o, i) => (
                  <Box key={`o${i}`} box={o} natural={natural} variant="object" />
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className={styles.placeholder}>
            <span className={styles.placeholderIcon}>📷</span>
            <p className={styles.placeholderTitle}>
              {connected ? 'En attente du flux vidéo…' : 'Robot déconnecté'}
            </p>
            <p className={styles.placeholderHint}>
              {connected
                ? 'Aucune trame reçue. La vision doit être activée sur le robot (robot.capteurs.vision.enabled) et le serveur de détection démarré.'
                : 'La connexion WebSocket au robot est interrompue.'}
            </p>
          </div>
        )}
      </div>

      <div className={styles.hud}>
        <span className={styles.stat}>
          <span className={frame ? styles.dotOn : styles.dotOff} />
          {frame ? `${fps} fps` : '—'}
        </span>
        <span className={styles.stat}>👤 {faces.length}</span>
        <span className={styles.stat}>📦 {objects.length}</span>
        <button
          className={styles.muteBtn}
          onClick={toggleMute}
          title={muted ? 'Activer le son du robot' : 'Couper le son'}
        >
          {muted ? '🔇' : '🔊'}
        </button>
      </div>
    </div>
  )
}

function Box({
  box,
  natural,
  variant,
}: {
  box: { x: number; y: number; width: number; height: number; name?: string }
  natural: { w: number; h: number }
  variant: 'face' | 'object'
}) {
  const style = {
    left: `${(box.x / natural.w) * 100}%`,
    top: `${(box.y / natural.h) * 100}%`,
    width: `${(box.width / natural.w) * 100}%`,
    height: `${(box.height / natural.h) * 100}%`,
  }
  return (
    <div
      className={`${styles.box} ${variant === 'face' ? styles.boxFace : styles.boxObject}`}
      style={style}
    >
      {box.name && <span className={styles.boxLabel}>{box.name}</span>}
    </div>
  )
}
