import { useEffect, useRef, useState } from 'react'
import { dessinerOnde } from '../dessin'
import { fenetreDe } from '../geometrie'
import { useStudioStore } from '../store/studioStore'
import styles from './Onde.module.css'

/**
 * Ce que le robot jouera, dessiné sous la frise et sur la même échelle de temps.
 *
 * C'est l'onde du fichier rendu, pas une vue d'artiste : le WAV envoyé au robot est ce tampon-là.
 * Pendant qu'un rendu est en retard sur une retouche, l'onde pâlit plutôt que de mentir.
 */
export function Onde({ rendu, perime, tete }: { rendu: AudioBuffer | null; perime: boolean; tete: number | null }) {
  const son = useStudioStore((s) => s.son)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [taille, setTaille] = useState({ w: 0, h: 0 })

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const observateur = new ResizeObserver(() => {
      const r = canvas.getBoundingClientRect()
      setTaille({ w: r.width, h: r.height })
    })
    observateur.observe(canvas)
    return () => observateur.disconnect()
  }, [])

  useEffect(() => {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx || !taille.w) return
    const dpr = window.devicePixelRatio || 1
    canvas.width = Math.round(taille.w * dpr)
    canvas.height = Math.round(taille.h * dpr)
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    dessinerOnde(ctx, { ...taille, fenetre: fenetreDe(son.morceaux, son.reglages) }, son, rendu, perime, tete)
  }, [son, rendu, perime, tete, taille])

  return (
    <div className={styles.cadre}>
      <canvas ref={canvasRef} className={styles.toile} />
      <span className={styles.etiquette}>Ce que le robot jouera</span>
    </div>
  )
}
