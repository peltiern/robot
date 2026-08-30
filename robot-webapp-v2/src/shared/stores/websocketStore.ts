import { Client, type IMessage } from '@stomp/stompjs'
import { create } from 'zustand'
import type { Animation } from '../types/animation'

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/wsendpoint'

interface WebSocketState {
  connected: boolean
  client: Client | null

  connect: () => void
  disconnect: () => void

  // Animation : seul le curseur passe par le websocket. Jouer et arrêter sont des ressources
  // REST (voir animationApi) — un ordre isolé qui attend une réponse n'a rien à faire ici.
  // Le curseur, lui, est un flot d'instants sans réponse dont seul le dernier compte.
  deplacerCurseur: (animation: Animation, instant: number) => void

  // Dialogue : fait dire un texte au robot (ParoleEvent → /app/robotevents)
  speak: (texte: string) => void

  // Contrôle : publie un évènement robot brut (doit contenir `eventType`) sur /app/robotevents
  sendRobotEvent: (event: Record<string, unknown>) => void

  // Souscription aux topics
  subscribe: (topic: string, callback: (msg: IMessage) => void) => (() => void)
}

export const useWebSocketStore = create<WebSocketState>((set, get) => ({
  connected: false,
  client: null,

  connect() {
    if (get().client?.active) return

    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 3000,
      onConnect: () => {
        set({ connected: true })
      },
      onDisconnect: () => {
        set({ connected: false })
      },
      // Indispensable en plus de `onDisconnect` : ce dernier n'est appelé que sur réception du
      // reçu DISCONNECT, donc uniquement lors d'une fermeture propre. Quand le serveur coupe la
      // session (tampon d'envoi saturé) ou que le WiFi tombe, seul `onWebSocketClose` est appelé.
      // Sans ça, `connected` restait à true : stompjs se reconnectait bien tout seul, mais l'état
      // ne changeait jamais, donc les effets de `useTopic` ne se relançaient pas et PLUS AUCUN
      // topic n'était réabonné (stompjs ne réabonne pas de lui-même). L'appli restait alors
      // muette — vidéo et télémétrie figées — jusqu'à un rechargement manuel de la page.
      onWebSocketClose: () => {
        set({ connected: false })
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame)
        set({ connected: false })
      },
    })

    client.activate()
    set({ client })
  },

  disconnect() {
    get().client?.deactivate()
    set({ connected: false, client: null })
  },

  deplacerCurseur(animation, instant) {
    get().client?.publish({
      destination: '/app/animation/curseur',
      body: JSON.stringify({ animation, instant }),
    })
  },

  speak(texte) {
    const message = texte.trim()
    if (!message) return
    get().client?.publish({
      destination: '/app/robotevents',
      body: JSON.stringify({ eventType: 'parole', texte: message }),
    })
  },

  sendRobotEvent(event) {
    get().client?.publish({
      destination: '/app/robotevents',
      body: JSON.stringify(event),
    })
  },

  subscribe(topic, callback) {
    const client = get().client
    if (!client?.active) return () => {}
    const sub = client.subscribe(topic, callback)
    return () => sub.unsubscribe()
  },
}))
