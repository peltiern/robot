import { useEffect, useRef, useState } from 'react'
import { useAnimationStore } from '../store/animationStore'
import { creerMaquette, type Maquette, type Vue } from '../maquette/scene'
import { SimulateurServos } from '../maquette/simulateur'
import type { Disposition } from '../AnimationPage'
import styles from './Maquette3D.module.css'

/**
 * Le robot en 3D, qui joue la timeline comme il la jouerait.
 *
 * Elle montre la même chose dans les deux modes : en Simulation c'est tout ce qui bouge, en Robot
 * c'est ce qu'on demande à la vraie tête — de quoi voir d'un coup d'œil si elle suit.
 *
 * Exportée par défaut pour être chargée à part (voir PropertiesPanel).
 */
interface Proprietes {
  className?: string
  disposition: Disposition
  onChangerDisposition: (d: Disposition) => void
}

export default function Maquette3D({ className, disposition, onChangerDisposition }: Proprietes) {
  const hoteRef = useRef<HTMLDivElement>(null)
  const maquetteRef = useRef<Maquette | null>(null)
  // la vue qui va avec la place : le visage dans le bandeau du haut, le robot entier dans la colonne
  const vueDe = (d: Disposition): Vue => d === 'haut' ? 'visage' : 'entier'
  const [vue, setVue] = useState<Vue>(vueDe(disposition))

  useEffect(() => {
    const hote = hoteRef.current
    if (!hote) return
    const maquette = creerMaquette(vueDe(disposition))
    maquetteRef.current = maquette
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
      maquetteRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- la vue initiale seulement ; la suite passe par cadrer
  }, [])

  function cadrer(nouvelle: Vue) {
    setVue(nouvelle)
    maquetteRef.current?.cadrer(nouvelle)
  }

  function deplacer() {
    const nouvelle: Disposition = disposition === 'haut' ? 'droite' : 'haut'
    onChangerDisposition(nouvelle)
    cadrer(vueDe(nouvelle))
  }

  return (
    <div className={`${styles.cadre} ${className ?? ''}`}>
      <div ref={hoteRef} className={styles.hote} />
      <div className={styles.vues}>
        {(['entier', 'visage'] as const).map(v => (
          <button key={v} className={vue === v ? styles.actif : undefined} onClick={() => cadrer(v)}
                  title="Clic droit ou Maj pour déplacer · double-clic pour recadrer">
            {v === 'entier' ? 'Robot' : 'Visage'}
          </button>
        ))}
        <button onClick={deplacer} title={disposition === 'haut' ? 'Mettre la maquette à droite' : 'Mettre la maquette en haut'}>
          {disposition === 'haut' ? 'À droite ⇥' : 'En haut ⤒'}
        </button>
      </div>
    </div>
  )
}
