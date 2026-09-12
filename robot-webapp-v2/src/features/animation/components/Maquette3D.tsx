import { useEffect, useRef } from 'react'
import { useAnimationStore } from '../store/animationStore'
import { creerMaquette } from '../maquette/scene'
import { SimulateurServos } from '../maquette/simulateur'

/**
 * La tête du robot en 3D, qui joue la timeline comme le robot la jouerait.
 *
 * Elle montre la même chose dans les deux modes : en Simulation c'est tout ce qui bouge, en Robot
 * c'est ce qu'on demande à la vraie tête — de quoi voir d'un coup d'œil si elle suit.
 *
 * Exportée par défaut pour être chargée à part (voir PropertiesPanel).
 */
export default function Maquette3D({ className }: { className?: string }) {
  const hoteRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const hote = hoteRef.current
    if (!hote) return
    const maquette = creerMaquette()
    hote.appendChild(maquette.element)
    const observateur = new ResizeObserver(() => maquette.dimensionner(hote.clientWidth, hote.clientHeight))
    observateur.observe(hote)

    const simulateur = new SimulateurServos()
    let precedent = performance.now()
    let image = 0
    // L'état est lu à chaque image plutôt que par abonnement : soixante rendus React par seconde
    // pour déplacer quelques objets 3D, ce serait tout le panneau redessiné pour rien.
    const boucle = (maintenant: number) => {
      image = requestAnimationFrame(boucle)
      const { tracks, playhead } = useAnimationStore.getState()
      const positions = simulateur.avancer(tracks, playhead, maintenant - precedent)
      precedent = maintenant
      maquette.poser({
        oeilGauche:      positions.OEIL_GAUCHE ?? 0,
        oeilDroit:       positions.OEIL_DROIT ?? 0,
        panoramique:     positions.COU_GAUCHE_DROITE ?? 0,
        inclinaison:     positions.COU_HAUT_BAS ?? 0,
        monterDescendre: positions.COU_MONTER_DESCENDRE ?? 0,
      })
      maquette.rendre()
    }
    image = requestAnimationFrame(boucle)

    return () => {
      cancelAnimationFrame(image)
      observateur.disconnect()
      maquette.liberer()
      maquette.element.remove()
    }
  }, [])

  return <div ref={hoteRef} className={className} />
}
