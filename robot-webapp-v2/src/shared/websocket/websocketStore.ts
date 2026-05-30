import { Client, type IMessage } from '@stomp/stompjs'
import { create } from 'zustand'
import type { Animation } from '../types/animation'

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/wsendpoint'

interface WebSocketState {
  connected: boolean
  client: Client | null

  connect: () => void
  disconnect: () => void

  // Animation
  playAnimation: (animation: Animation) => void
  stopAnimation: () => void
  scrubAnimation: (animation: Animation, time: number) => void

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

  playAnimation(animation) {
    get().client?.publish({
      destination: '/app/animation/play',
      body: JSON.stringify({ animation }),
    })
  },

  stopAnimation() {
    get().client?.publish({ destination: '/app/animation/stop', body: '' })
  },

  scrubAnimation(animation, time) {
    get().client?.publish({
      destination: '/app/animation/scrub',
      body: JSON.stringify({ animation, time }),
    })
  },

  subscribe(topic, callback) {
    const client = get().client
    if (!client?.active) return () => {}
    const sub = client.subscribe(topic, callback)
    return () => sub.unsubscribe()
  },
}))
