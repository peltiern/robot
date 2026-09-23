import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Écouter un son dans le navigateur, et savoir où en est la lecture.
 *
 * Le contexte audio ne s'ouvre qu'au premier geste : un navigateur refuse de jouer avant. Et il
 * n'y a jamais qu'une lecture à la fois — jouer coupe la précédente, comme le robot, qui n'a
 * qu'une bouche.
 */
export interface Lecture {
  /** Instant courant dans le son, en secondes, ou null quand rien ne joue. */
  tete: number | null
  enCours: boolean
  /** `decalage` place la tête de lecture ailleurs qu'à zéro : un morceau seul, joué à sa place. */
  jouer: (tampon: AudioBuffer, decalage?: number) => void
  arreter: () => void
}

export function useLecture(): Lecture {
  const contexteRef = useRef<AudioContext | null>(null)
  const sourceRef = useRef<AudioBufferSourceNode | null>(null)
  const rafRef = useRef<number | null>(null)
  const [tete, setTete] = useState<number | null>(null)
  // En état et non en référence : c'est ce qui fait basculer le bouton entre ▶ et ■.
  const [enCours, setEnCours] = useState(false)

  const arreter = useCallback(() => {
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    rafRef.current = null
    try {
      sourceRef.current?.stop()
    } catch {
      // déjà terminée
    }
    sourceRef.current = null
    setTete(null)
    setEnCours(false)
  }, [])

  const jouer = useCallback(
    (tampon: AudioBuffer, decalage = 0) => {
      arreter()
      if (!contexteRef.current) contexteRef.current = new AudioContext()
      const ctx = contexteRef.current
      if (ctx.state === 'suspended') void ctx.resume()

      const source = ctx.createBufferSource()
      source.buffer = tampon
      source.connect(ctx.destination)
      const depart = ctx.currentTime + 0.03
      source.start(depart)
      sourceRef.current = source
      setEnCours(true)

      const suivre = () => {
        const ecoule = ctx.currentTime - depart
        if (ecoule > tampon.duration) {
          arreter()
          return
        }
        setTete(decalage + Math.max(0, ecoule))
        rafRef.current = requestAnimationFrame(suivre)
      }
      rafRef.current = requestAnimationFrame(suivre)
    },
    [arreter],
  )

  useEffect(() => arreter, [arreter])

  return { tete, enCours, jouer, arreter }
}
