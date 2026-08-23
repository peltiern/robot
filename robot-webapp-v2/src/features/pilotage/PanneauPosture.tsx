import { useCallback, useMemo, useRef, useState, type CSSProperties } from 'react'
import { useWebSocketStore } from '../../shared/stores/websocketStore'
import { useTopic } from '../../shared/websocket/useTopic'
import {
  actionneurs,
  useOrganesStore,
  type Articulation,
} from '../../shared/stores/organesStore'
import { COMMANDE, estPilotable, evenementsRecentrage } from '../../shared/api/commandes'
import { useArretUrgenceStore } from '../../shared/stores/arretUrgenceStore'
import styles from './pilotage.module.css'

/**
 * Fenêtre (ms) pendant laquelle un axe que l'utilisateur vient de piloter ignore
 * la télémétrie : le temps que le moteur rejoigne la consigne, sinon le retour de
 * position « tirerait » le curseur à contre-sens du geste. Au-delà, la télémétrie
 * reprend la main (manette, animation…).
 */
const FENETRE_PILOTAGE_UTILISATEUR = 800

/** Borne une valeur dans [min, max]. */
const clamp = (v: number, min: number, max: number) => Math.max(min, Math.min(max, v))

/** Symbole d'unité pour l'affichage (deg → °). */
const symboleUnite = (unite: string) => (unite === 'deg' ? '°' : ` ${unite}`)

/** Envoi throttlé (leading + trailing, 80 ms) par articulation, pour ne pas inonder le WebSocket au drag. */
function useEnvoiThrottle() {
  const sendRobotEvent = useWebSocketStore((s) => s.sendRobotEvent)
  const minuteries = useRef<Record<string, number>>({})
  const enAttente = useRef<Record<string, Record<string, unknown>>>({})

  return useCallback(
    (cle: string, evenement: Record<string, unknown>) => {
      if (minuteries.current[cle]) {
        enAttente.current[cle] = evenement
        return
      }
      sendRobotEvent(evenement)
      minuteries.current[cle] = window.setTimeout(() => {
        delete minuteries.current[cle]
        const dernier = enAttente.current[cle]
        if (dernier) {
          delete enAttente.current[cle]
          sendRobotEvent(dernier)
        }
      }, 80)
    },
    [sendRobotEvent],
  )
}

/**
 * Posture : où en sont les articulations, et de quoi les reprendre à la main.
 *
 * Ce n'est plus une page de contrôle — la manette pilote, l'écran montre. Le
 * volet affiche donc d'abord l'état réel (télémétrie live, même quand le
 * mouvement vient d'ailleurs), et laisse la reprise en main possible d'un doigt.
 */
export function PanneauPosture() {
  const connecte = useWebSocketStore((s) => s.connected)
  const envoyer = useWebSocketStore((s) => s.sendRobotEvent)
  const organesBruts = useOrganesStore((s) => s.organes)
  const etat = useOrganesStore((s) => s.etat)
  const envoiThrottle = useEnvoiThrottle()

  const arretUrgence = useArretUrgenceStore((s) => s.actif)

  /** Le robot accepte-t-il des ordres de mouvement ? */
  const pilotable = connecte && !arretUrgence

  const organes = useMemo(() => actionneurs(organesBruts, estPilotable), [organesBruts])
  const articulations = useMemo(() => organes.flatMap((o) => o.articulations), [organes])

  const [positions, setPositions] = useState<Record<string, number>>({})

  // Horodatage du dernier pilotage utilisateur par axe (cf. FENETRE_PILOTAGE_UTILISATEUR).
  const dernierPilotage = useRef<Record<string, number>>({})

  const parId = useMemo(
    () => Object.fromEntries(articulations.map((a) => [a.id, a])) as Record<string, Articulation>,
    [articulations],
  )

  // Positions de départ : l'état réel du robot au moment de la découverte (il a
  // pu bouger avant l'ouverture de l'appli). Réinitialisées pendant le rendu, et
  // non dans un effet, pour ne pas afficher une image intermédiaire à zéro : le
  // jeu d'articulations ne change qu'à la découverte, pas à chaque trame.
  const [signature, setSignature] = useState('')
  const signatureCourante = articulations.map((a) => a.id).join('|')
  if (signatureCourante !== signature) {
    setSignature(signatureCourante)
    setPositions(
      Object.fromEntries(
        articulations.map((a) => [
          a.id,
          a.position == null ? 0 : Math.round(clamp(a.position, a.min, a.max)),
        ]),
      ),
    )
  }

  // Télémétrie live : Cou et Yeux publient leur position réelle sur le topic
  // partagé par tous les organes (on ignore ici les identifiants qu'on ne pilote
  // pas). Les axes mus ailleurs — manette, animation — suivent tout seuls.
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
        const mesures = evenement.valeurs
        if (!mesures) return
        const maintenant = Date.now()
        setPositions((actuelles) => {
          let change = false
          const suivantes = { ...actuelles }
          for (const [id, position] of Object.entries(mesures)) {
            const articulation = parId[id]
            if (!articulation) continue
            if (maintenant - (dernierPilotage.current[id] ?? 0) < FENETRE_PILOTAGE_UTILISATEUR) continue
            const arrondie = Math.round(clamp(position, articulation.min, articulation.max))
            if (suivantes[id] !== arrondie) {
              suivantes[id] = arrondie
              change = true
            }
          }
          return change ? suivantes : actuelles
        })
      },
      [parId],
    ),
  )

  const glisser = (articulation: Articulation, valeur: number) => {
    dernierPilotage.current[articulation.id] = Date.now()
    setPositions((p) => ({ ...p, [articulation.id]: valeur }))
    const commande = COMMANDE[articulation.id]
    envoiThrottle(articulation.id, { eventType: commande.event, [commande.field]: valeur })
  }

  const recentrer = () => {
    const maintenant = Date.now()
    setPositions((p) => {
      const suivantes = { ...p }
      for (const articulation of articulations) {
        suivantes[articulation.id] = 0
        dernierPilotage.current[articulation.id] = maintenant
      }
      return suivantes
    })
    for (const evenement of evenementsRecentrage(organes)) envoyer(evenement)
  }

  return (
    <div className={`${styles.posture} verre`}>
      <div className={styles.postureTitre}>
        <span className="eyebrow">Posture</span>
        <span
          className="eyebrow"
          style={
            arretUrgence
              ? { color: 'var(--alarme-clair)' }
              : connecte
                ? { color: 'var(--accent)' }
                : undefined
          }
        >
          {arretUrgence ? 'coupé' : connecte ? 'live' : 'hors ligne'}
        </span>
      </div>

      {/* Hors ligne, on n'affiche aucun angle : la dernière position reçue n'a plus
          de rapport avec celle du robot, et un curseur qu'on ne peut pas bouger
          n'a rien à montrer. */}
      {!connecte && <p className={styles.postureMessage}>En attente du robot.</p>}

      {/* Arrêt d'urgence : les curseurs disparaissent au lieu d'être seulement grisés. Un curseur
          qu'on peut saisir sans que rien ne bouge se lit comme une panne. */}
      {connecte && arretUrgence && (
        <p className={styles.postureMessage}>Moteurs coupés (arrêt d'urgence).</p>
      )}

      {pilotable && etat !== 'pret' && (
        <p className={styles.postureMessage}>
          {etat === 'erreur' ? 'Articulations illisibles.' : 'Lecture des articulations…'}
        </p>
      )}

      {pilotable && etat === 'pret' && articulations.length === 0 && (
        <p className={styles.postureMessage}>Aucune articulation pilotable.</p>
      )}

      {pilotable &&
        articulations.map((articulation) => (
          <Axe
            key={articulation.id}
            articulation={articulation}
            valeur={positions[articulation.id] ?? 0}
            onGlisser={glisser}
          />
        ))}

      {pilotable && articulations.length > 0 && (
        <button className={styles.recentrer} onClick={recentrer}>
          RECENTRER À 0°
        </button>
      )}
    </div>
  )
}

/** Un axe : les curseurs ne sont rendus que robot connecté (cf. `PanneauPosture`). */
function Axe({
  articulation,
  valeur,
  onGlisser,
}: {
  articulation: Articulation
  valeur: number
  onGlisser: (articulation: Articulation, valeur: number) => void
}) {
  const { min, max } = articulation
  // Fractions de l'échelle : le CSS en tire la géométrie du décor, en tenant
  // compte de la course réelle du bouton (cf. `--pouce` dans la feuille de style).
  const zero = (0 - min) / (max - min)
  const courante = clamp((valeur - min) / (max - min), 0, 1)
  const zeroVisible = zero >= 0 && zero <= 1

  return (
    <div className={styles.axe}>
      <span className={styles.axeNom}>{abrege(articulation.libelle)}</span>
      <span className={styles.piste}>
        <i
          className={styles.remplissage}
          style={
            {
              '--a': Math.min(courante, zeroVisible ? zero : 0),
              '--b': Math.max(courante, zeroVisible ? zero : 0),
            } as CSSProperties
          }
        />
        {zeroVisible && (
          <i className={styles.zero} style={{ '--z': zero } as CSSProperties} />
        )}
        <input
          className={styles.curseur}
          type="range"
          min={min}
          max={max}
          step={1}
          value={valeur}
          aria-label={articulation.libelle}
          onChange={(e) => onGlisser(articulation, Number(e.target.value))}
        />
      </span>
      <span className={`${styles.axeMesure} data`}>
        {/* `|| 0` : sans lui, une valeur négative arrondie à zéro s'affiche « -0° ». */}
        {(Math.round(valeur) || 0) + symboleUnite(articulation.unite)}
      </span>
    </div>
  )
}

/**
 * Le volet fait 268 px : on garde le libellé du robot, sans sa parenthèse
 * explicative (« Panoramique (gauche / droite) » → « Panoramique »).
 */
function abrege(libelle: string): string {
  return libelle.replace(/\s*\(.*\)\s*$/, '')
}
