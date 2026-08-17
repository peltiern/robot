import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { Layout } from '../shared/components/Layout'
import { PilotagePage } from '../features/pilotage/PilotagePage'
import { AnimationPage } from '../features/animation/AnimationPage'
import { PersonnesPage } from '../features/personnes/PersonnesPage'

/**
 * Trois vues : le pilotage (l'image et tout ce qui se passe autour), l'atelier
 * (l'éditeur d'animation, qui se fait robot à l'arrêt et mérite l'écran entier)
 * et le répertoire (ceux que le robot connaît). Tout ce qui était onglet —
 * contrôle, dialogue, monitoring, configuration — est resté un volet du pilotage :
 * seul mérite une vue ce qui ne se consulte pas en pilotant.
 */
export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/pilotage" replace />} />
          <Route path="/pilotage" element={<PilotagePage />} />
          <Route path="/atelier" element={<AnimationPage />} />
          <Route path="/personnes" element={<PersonnesPage />} />
          {/* Anciennes adresses : les favoris de la tablette continuent de tomber juste. */}
          <Route path="/animation" element={<Navigate to="/atelier" replace />} />
          <Route path="*" element={<Navigate to="/pilotage" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
