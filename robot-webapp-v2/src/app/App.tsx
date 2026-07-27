import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from '../shared/components/Layout'
import { LivePage }       from '../features/live/LivePage'
import { ControlPage }    from '../features/control/ControlPage'
import { AnimationPage }  from '../features/animation/AnimationPage'
import { DialoguePage }   from '../features/dialogue/DialoguePage'
import { ConfigPage }     from '../features/config/ConfigPage'
import { MonitoringPage } from '../features/monitoring/MonitoringPage'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/live" replace />} />
          <Route path="/live"       element={<LivePage />} />
          <Route path="/controle"   element={<ControlPage />} />
          <Route path="/animation"  element={<AnimationPage />} />
          <Route path="/dialogue"   element={<DialoguePage />} />
          <Route path="/config"     element={<ConfigPage />} />
          <Route path="/monitoring" element={<MonitoringPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
