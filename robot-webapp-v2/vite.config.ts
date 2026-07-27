import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Hôte du backend robot-core (HTTP + WebSocket). Le robot tourne en Docker sur la Jetson :
// on cible donc `wall-e.local:8080` par défaut. Surchargeable via VITE_BACKEND_HOST
// (ex. `localhost:8080` pour un back lancé en local, ou une IP). `/api` et `/wsendpoint`
// pointent sur le MÊME backend — vidéo/WS et API REST restent cohérents.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const host = env.VITE_BACKEND_HOST || 'wall-e.local:8080'
  return {
    plugins: [react()],
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: `http://${host}`,
          changeOrigin: true,
        },
        '/wsendpoint': {
          target: `ws://${host}`,
          ws: true,
          changeOrigin: true,
        },
      },
    },
  }
})
