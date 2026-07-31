import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Polices embarquées (aucun appel réseau : la tablette peut être hors ligne).
// Nunito porte toute l'interface, Baloo 2 la marque et les gros titres.
import '@fontsource/nunito/latin-400.css'
import '@fontsource/nunito/latin-ext-400.css'
import '@fontsource/nunito/latin-700.css'
import '@fontsource/nunito/latin-ext-700.css'
import '@fontsource/baloo-2/latin-700.css'
import '@fontsource/baloo-2/latin-ext-700.css'

import './shared/theme/theme.css'
import { App } from './app/App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
