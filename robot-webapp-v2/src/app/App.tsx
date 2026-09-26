import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { Racine } from '../shared/components/Racine'
import { CoqueRobot } from '../shared/components/CoqueRobot'
import { CoqueAtelier } from '../shared/components/CoqueAtelier'
import { PilotagePage } from '../features/pilotage/PilotagePage'
import { AnimationPage } from '../features/animation/AnimationPage'
import { StudioPage } from '../features/studio/StudioPage'
import { PersonnesPage } from '../features/personnes/PersonnesPage'
import { VoixPage } from '../features/voix/VoixPage'

/**
 * Deux coques, et non une seule : le pilotage n'a de sens que robot allumé, tandis que les
 * établis — atelier d'animation, studio son, répertoire — s'ouvrent avec ou sans lui. La vidéo et
 * les flux du HUD ne sont donc montés que par la coque robot. Ce qui leur est commun, le fond et
 * la connexion, vit au-dessus des deux, dans {@link Racine}.
 *
 * Tout ce qui était onglet du HUD — contrôle, dialogue, monitoring, configuration — est resté un
 * volet du pilotage : seul mérite une vue ce qui ne se consulte pas en pilotant.
 */
export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Racine />}>
          <Route element={<CoqueRobot />}>
            <Route path="/pilotage" element={<PilotagePage />} />
          </Route>
          <Route element={<CoqueAtelier />}>
            <Route path="/atelier" element={<AnimationPage />} />
            <Route path="/studio" element={<StudioPage />} />
            <Route path="/voix" element={<VoixPage />} />
            <Route path="/personnes" element={<PersonnesPage />} />
          </Route>
          <Route index element={<Navigate to="/pilotage" replace />} />
          {/* Anciennes adresses : les favoris de la tablette continuent de tomber juste. */}
          <Route path="/animation" element={<Navigate to="/atelier" replace />} />
          <Route path="*" element={<Navigate to="/pilotage" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
