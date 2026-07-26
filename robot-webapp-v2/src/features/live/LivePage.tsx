import { useCallback, useRef, useState } from 'react'
import type { IMessage } from '@stomp/stompjs'
import { useTopic } from '../../shared/websocket/useTopic'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useAudioStream } from './useAudioStream'
import type { VideoEvent } from '../../shared/types/events'
import styles from './LivePage.module.css'

export function LivePage() {
  const connected = useWebSocketStore((s) => s.connected)
  const { muted, toggleMute } = useAudioStream()
  const [frame, setFrame] = useState<VideoEvent | null>(null)
  const [natural, setNatural] = useState<{ w: number; h: number } | null>(null)
  const [fps, setFps] = useState(0)

  // Fenêtre glissante des horodatages de trames pour estimer le FPS.
  const stamps = useRef<number[]>([])

  const onVideo = useCallback((msg: IMessage) => {
    let event: VideoEvent
    try {
      event = JSON.parse(msg.body)
    } catch {
      return
    }
    if (!event?.imageBase64) return
    setFrame(event)

    const now = performance.now()
    const s = stamps.current
    s.push(now)
    while (s.length > 0 && now - s[0] > 1000) s.shift()
    setFps(s.length)
  }, [])

  useTopic('/video', onVideo)

  const faces = frame?.faces ?? []
  const objects = frame?.objects ?? []

  return (
    <div className={styles.page}>
      <div className={styles.stage}>
        {frame ? (
          <div className={styles.frameWrap}>
            <img
              className={styles.image}
              src={`data:image/jpeg;base64,${frame.imageBase64}`}
              alt="Flux caméra du robot"
              onLoad={(e) =>
                setNatural({
                  w: e.currentTarget.naturalWidth,
                  h: e.currentTarget.naturalHeight,
                })
              }
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
