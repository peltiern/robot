import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { Layout } from '../shared/components/Layout'
import { PilotagePage } from '../features/pilotage/PilotagePage'
import { AnimationPage } from '../features/animation/AnimationPage'

/**
 * Deux vues, pas une de plus : le pilotage (l'image et tout ce qui se passe
 * autour) et l'atelier (l'éditeur d'animation, qui se fait robot à l'arrêt et
 * mérite l'écran entier). Tout ce qui était onglet — contrôle, dialogue,
 * monitoring, configuration — est devenu un volet du pilotage.
 */
export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/pilotage" replace />} />
          <Route path="/pilotage" element={<PilotagePage />} />
          <Route path="/atelier" element={<AnimationPage />} />
          {/* Anciennes adresses : les favoris de la tablette continuent de tomber juste. */}
          <Route path="/animation" element={<Navigate to="/atelier" replace />} />
          <Route path="*" element={<Navigate to="/pilotage" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
