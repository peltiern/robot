import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useWebSocketStore } from '../../shared/websocket/websocketStore'
import { useTopic } from '../../shared/websocket/useTopic'
import styles from './ControlPage.module.css'

/**
 * Fenêtre (ms) pendant laquelle un axe que l'utilisateur vient de piloter (drag / recentrage)
 * ignore la télémétrie : le temps que le moteur rejoigne la consigne, sinon le retour de position
 * « tirerait » le curseur à contre-sens du geste. Au-delà, la télémétrie reprend la main (manette…).
 */
const FENETRE_PILOTAGE_UTILISATEUR = 800

/**
 * Page de contrôle : curseurs pilotant chaque articulation du robot.
 *
 * Le robot se décrit lui-même via `GET /api/organes` : la page ne code en dur ni la liste des
 * organes, ni les libellés, ni les butées, ni l'orientation, ni la position — tout vient du
 * backend. Elle boucle simplement sur les organes reçus et rend un curseur par articulation,
 * borné par les vraies butées moteur et positionné sur l'état réel du robot au chargement.
 *
 * Seul couplage restant (assumé en P1 : la commande passe encore par WebSocket) : la
 * correspondance articulation → évènement/champ WS à publier. Ce serait le dernier « en dur »
 * à supprimer en passant à une commande REST (PUT sur la position de l'articulation).
 */
const COMMANDE: Record<string, { event: 'mouvement-yeux' | 'mouvement-cou'; field: string }> = {
  oeilGauche: { event: 'mouvement-yeux', field: 'positionOeilGauche' },
  oeilDroit: { event: 'mouvement-yeux', field: 'positionOeilDroit' },
  pan: { event: 'mouvement-cou', field: 'positionPanoramique' },
  tilt: { event: 'mouvement-cou', field: 'positionInclinaison' },
  upDown: { event: 'mouvement-cou', field: 'positionMonterDescendre' },
}

type Orientation = 'VERTICAL' | 'HORIZONTAL' | 'ROTATION'

interface Articulation {
  id: string
  libelle: string
  unite: string
  min: number
  max: number
  orientation: Orientation
  position: number | null
}

interface Organe {
  id: string
  libelle: string
  type: 'ACTIONNEUR' | 'CAPTEUR'
  articulations: Articulation[]
}

/** Borne une valeur dans [min, max]. */
const clamp = (v: number, min: number, max: number) => Math.max(min, Math.min(max, v))

/** Symbole d'unité pour l'affichage (deg → °). */
const symboleUnite = (unite: string) => (unite === 'deg' ? '°' : ` ${unite}`)

/** Envoi throttlé (leading + trailing, 80 ms) par articulation, pour ne pas inonder le WebSocket au drag. */
function useThrottledSender() {
  const sendRobotEvent = useWebSocketStore((s) => s.sendRobotEvent)
  const timers = useRef<Record<string, number>>({})
  const pending = useRef<Record<string, Record<string, unknown>>>({})

  return useCallback(
    (key: string, event: Record<string, unknown>) => {
      if (timers.current[key]) {
        pending.current[key] = event
        return
      }
      sendRobotEvent(event)
      timers.current[key] = window.setTimeout(() => {
        delete timers.current[key]
        const last = pending.current[key]
        if (last) {
          delete pending.current[key]
          sendRobotEvent(last)
        }
      }, 80)
    },
    [sendRobotEvent],
  )
}

type LoadState = 'loading' | 'ready' | 'error'

export function ControlPage() {
  const connected = useWebSocketStore((s) => s.connected)
  const send = useThrottledSender()

  const [organes, setOrganes] = useState<Organe[]>([])
  const [values, setValues] = useState<Record<string, number>>({})
  const [loadState, setLoadState] = useState<LoadState>('loading')

  // Horodatage du dernier pilotage utilisateur par axe, pour ignorer la télémétrie le temps que
  // le moteur rejoigne la consigne (cf. FENETRE_PILOTAGE_UTILISATEUR).
  const dernierPilotage = useRef<Record<string, number>>({})

  // Butées par articulation, pour borner les positions reçues en télémétrie.
  const articulationParId = useMemo(
    () => Object.fromEntries(organes.flatMap((o) => o.articulations.map((a) => [a.id, a]))) as Record<string, Articulation>,
    [organes],
  )

  // Au montage : découverte des organes/articulations (butées + positions réelles). Les curseurs
  // sont ainsi bornés par les vraies butées et placés sur l'état réel (le robot a pu bouger avant
  // l'arrivée sur la page). Une seule requête, pas de rafraîchissement automatique (sinon on
  // écraserait le drag) — mais rechargeable à la demande (bouton « Réessayer »), utile si le back
  // n'était pas encore démarré au montage.
  const charger = useCallback(() => {
    setLoadState('loading')
    fetch('/api/organes')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<Organe[]>
      })
      .then((organesRecus) => {
        // On ne pilote que les articulations dont on sait publier la commande WS (map COMMANDE).
        const actionneurs = organesRecus
          .filter((o) => o.type === 'ACTIONNEUR')
          .map((o) => ({ ...o, articulations: o.articulations.filter((a) => COMMANDE[a.id]) }))
          .filter((o) => o.articulations.length > 0)
        const valeursInitiales: Record<string, number> = {}
        for (const organe of actionneurs) {
          for (const art of organe.articulations) {
            valeursInitiales[art.id] = art.position == null ? 0 : Math.round(clamp(art.position, art.min, art.max))
          }
        }
        setOrganes(actionneurs)
        setValues(valeursInitiales)
        setLoadState('ready')
      })
      .catch(() => setLoadState('error'))
  }, [])

  useEffect(() => {
    charger()
  }, [charger])

  // Télémétrie live : Cou et Yeux publient chacun leur position réelle sur /events/telemetrie-organe
  // (topic partagé par tous les organes — Cou, Yeux, mais aussi les capteurs comme la page
  // Monitoring ; on ignore ici tout identifiant qu'on ne pilote pas). On met à jour les curseurs
  // des axes mus « ailleurs » (manette, animations…), en laissant tranquilles ceux que
  // l'utilisateur vient de piloter (fenêtre) et en bornant aux butées.
  useTopic(
    '/events/telemetrie-organe',
    useCallback(
      (msg) => {
        let evenement: { valeurs?: Record<string, number> }
        try {
          evenement = JSON.parse(msg.body)
        } catch {
          return
        }
        const positions = evenement.valeurs
        if (!positions) return
        const maintenant = Date.now()
        setValues((v) => {
          let change = false
          const next = { ...v }
          for (const [id, pos] of Object.entries(positions)) {
            const art = articulationParId[id]
            if (!art) continue
            if (maintenant - (dernierPilotage.current[id] ?? 0) < FENETRE_PILOTAGE_UTILISATEUR) continue
            const arrondi = Math.round(clamp(pos, art.min, art.max))
            if (next[id] !== arrondi) {
              next[id] = arrondi
              change = true
            }
          }
          return change ? next : v
        })
      },
      [articulationParId],
    ),
  )

  const onSlide = (art: Articulation, value: number) => {
    dernierPilotage.current[art.id] = Date.now()
    setValues((v) => ({ ...v, [art.id]: value }))
    const commande = COMMANDE[art.id]
    send(art.id, { eventType: commande.event, [commande.field]: value })
  }

  // Recentre tout à 0 : un évènement par organe, regroupant toutes les positions de ses articulations.
  const recentrer = () => {
    const maintenant = Date.now()
    setValues((v) => {
      const next = { ...v }
      for (const organe of organes)
        for (const art of organe.articulations) {
          next[art.id] = 0
          dernierPilotage.current[art.id] = maintenant
        }
      return next
    })
    for (const organe of organes) {
      const event: Record<string, unknown> = { eventType: COMMANDE[organe.articulations[0].id].event }
      for (const art of organe.articulations) event[COMMANDE[art.id].field] = 0
      useWebSocketStore.getState().sendRobotEvent(event)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h2 className={styles.title}>Contrôle</h2>
        <button className={styles.reset} onClick={recentrer} disabled={!connected || loadState !== 'ready'}>
          Recentrer (0°)
        </button>
      </div>

      {!connected && <p className={styles.warning}>Robot déconnecté — les commandes ne seront pas envoyées.</p>}

      {loadState === 'loading' && <p className={styles.info}>Récupération de la configuration du robot…</p>}
      {loadState === 'error' && (
        <p className={styles.warning}>
          Impossible de récupérer la configuration du robot (organes, butées, positions). Le robot est-il démarré ?{' '}
          <button className={styles.retry} onClick={charger}>Réessayer</button>
        </p>
      )}

      {loadState === 'ready' && (
        <div className={styles.sections}>
          {organes.map((organe) => {
            const verticaux = organe.articulations.filter((a) => a.orientation === 'VERTICAL')
            const horizontaux = organe.articulations.filter((a) => a.orientation !== 'VERTICAL')
            return (
              <section key={organe.id} className={styles.section}>
                <h3 className={styles.sectionTitle}>{organe.libelle}</h3>
                {horizontaux.map((art) => (
                  <AxisHorizontal key={art.id} art={art} value={values[art.id]} connected={connected} onSlide={onSlide} />
                ))}
                {verticaux.length > 0 && (
                  <div className={styles.verticalGroup}>
                    {verticaux.map((art) => (
                      <AxisVertical key={art.id} art={art} value={values[art.id]} connected={connected} onSlide={onSlide} />
                    ))}
                  </div>
                )}
              </section>
            )
          })}
        </div>
      )}
    </div>
  )
}

interface AxisProps {
  art: Articulation
  value: number
  connected: boolean
  onSlide: (art: Articulation, value: number) => void
}

function AxisVertical({ art, value, connected, onSlide }: AxisProps) {
  return (
    <div className={styles.axisV}>
      <span className={styles.axisLabel}>{art.libelle}</span>
      <span className={styles.axisValue}>{value}{symboleUnite(art.unite)}</span>
      <span className={styles.bound}>{art.max}</span>
      <input
        className={`${styles.slider} ${styles.sliderV}`}
        type="range"
        min={art.min}
        max={art.max}
        step={1}
        value={value}
        onChange={(e) => onSlide(art, Number(e.target.value))}
        disabled={!connected}
      />
      <span className={styles.bound}>{art.min}</span>
    </div>
  )
}

function AxisHorizontal({ art, value, connected, onSlide }: AxisProps) {
  return (
    <div className={styles.axisH}>
      <div className={styles.axisHead}>
        <span className={styles.axisLabel}>{art.libelle}</span>
        <span className={styles.axisValue}>{value}{symboleUnite(art.unite)}</span>
      </div>
      <div className={styles.sliderRow}>
        <span className={styles.bound}>{art.min}</span>
        <input
          className={styles.slider}
          type="range"
          min={art.min}
          max={art.max}
          step={1}
          value={value}
          onChange={(e) => onSlide(art, Number(e.target.value))}
          disabled={!connected}
        />
        <span className={styles.bound}>{art.max}</span>
      </div>
    </div>
  )
}
