import { useEffect } from 'react'
import { useEvenement, useLotParFrame } from '../websocket/useEvenement'
import { useWebSocketStore } from '../stores/websocketStore'
import { horodatageRobotEnMs } from '../websocket/horodatageRobot'
import type { ArretUrgenceEvent, ConversationEvent, SanteOrganesEvent } from '../types/events'
import { useArretUrgenceStore } from '../stores/arretUrgenceStore'
import { useSanteStore } from '../stores/santeStore'
import { useAudioStore } from '../stores/audioStore'
import { MAX_MESSAGES, useConversationStore, type MessageRecu } from '../stores/conversationStore'
import { NB_ECHANTILLONS_HISTORIQUE, useTelemetryStore } from '../stores/telemetryStore'

/**
 * Tous les flux du robot, branchés en un seul endroit et sans rien afficher.
 *
 * Monté une seule fois par le Layout, donc vivant pendant toute la session : changer de vue ne
 * coupe aucun abonnement, et rien n'est perdu pendant qu'on regarde ailleurs. C'est la seule
 * raison d'être de ce composant — chaque flux tenait avant dans son propre fichier, tous
 * recopiaient le même décodage, et il fallait penser à monter le nouveau.
 *
 * La vidéo fait bande à part ({@link ../video/VideoProvider}) : elle saute les images en retard au
 * lieu de les empiler, et gère des Blob URL à libérer.
 */
export function FluxRobot() {
  const connecte = useWebSocketStore((s) => s.connected)

  useFluxArretUrgence()
  useFluxSante(connecte)
  useFluxAudio()
  useFluxConversation()
  useFluxTelemetrie(connecte)

  return null
}

/** L'état est décidé par le robot seul : la tablette montre ce qu'il fait, pas ce qu'on demande. */
function useFluxArretUrgence() {
  const appliquer = useArretUrgenceStore((s) => s.appliquer)

  useEvenement<ArretUrgenceEvent>(
    '/events/arret-urgence',
    (evenement) => appliquer(evenement.actif, evenement.origine ?? null),
    (evenement) => typeof evenement.actif === 'boolean',
  )
}

function useFluxSante(connecte: boolean) {
  const appliquer = useSanteStore((s) => s.appliquer)
  const oublier = useSanteStore((s) => s.oublier)

  useEvenement<SanteOrganesEvent>(
    '/events/sante-organes',
    (evenement) => appliquer(evenement.organes),
    (evenement) => Array.isArray(evenement.organes),
  )

  // Hors ligne on n'affiche rien : une pastille verte figée sur la dernière trame reçue dirait
  // que tout va bien d'un robot dont on n'a plus de nouvelles.
  useEffect(() => {
    if (!connecte) oublier()
  }, [connecte, oublier])
}

function useFluxAudio() {
  const traiterTrame = useAudioStore((s) => s.traiterTrame)
  const fermer = useAudioStore((s) => s.fermer)

  useEvenement<{ audioContentBase64?: string }>(
    '/audio',
    (trame) => traiterTrame(trame.audioContentBase64!),
    (trame) => !!trame.audioContentBase64,
  )

  useEffect(() => fermer, [fermer])
}

function useFluxConversation() {
  const ingerer = useConversationStore((s) => s.ingerer)
  const { empiler } = useLotParFrame<MessageRecu>(ingerer, MAX_MESSAGES)

  useEvenement<ConversationEvent>(
    '/events/conversation',
    (evenement) =>
      empiler({
        texte: evenement.texte,
        fromRobot: evenement.duRobot,
        // L'horodatage vient du robot : après un à-coup réseau, les messages rattrapés affichent
        // l'heure à laquelle ils ont été prononcés, et non celle de leur arrivée en rafale.
        time: horodatageRobotEnMs(evenement.dateTime, Date.now()),
        idPersonne: evenement.idPersonne,
        prenom: evenement.prenom,
      }),
    (evenement) => !!evenement.texte,
  )
}

function useFluxTelemetrie(connecte: boolean) {
  const ingerer = useTelemetryStore((s) => s.ingerer)
  const oublier = useTelemetryStore((s) => s.oublier)
  const { empiler, jeter } = useLotParFrame<Record<string, number>>(ingerer, NB_ECHANTILLONS_HISTORIQUE)

  useEvenement<{ valeurs?: Record<string, number> }>(
    '/events/telemetrie-organe',
    (evenement) => empiler(evenement.valeurs!),
    (evenement) => !!evenement.valeurs,
  )

  // Liaison coupée : on oublie tout. Une valeur figée depuis la dernière trame reçue passerait
  // pour une mesure du moment — mieux vaut ne rien afficher que de laisser croire que ça va bien.
  useEffect(() => {
    if (connecte) return
    jeter()
    oublier()
  }, [connecte, jeter, oublier])
}
