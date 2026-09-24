import { useCallback, useEffect, useRef, useState } from 'react'
import { dessinerFrise } from '../dessin'
import {
  type Cadre,
  ECART_PASTILLE,
  epaisseurMax,
  fDe,
  fenetreDe,
  frequenceCentre,
  tDe,
  traceRuban,
  xDuPoint,
  yDe,
  yDuPoint,
  yPastille,
} from '../geometrie'
import { morceauA, useStudioStore } from '../store/studioStore'
import { avancementDuPoint, baseHz, borner, morceau as nouveauMorceau, valeurA, volumes, type Morceau, type Voyelle } from '../synthese/types'
import styles from './Frise.module.css'

/**
 * La frise : on y dessine le son.
 *
 * Tout s'y fait à la main — glisser un morceau le déplace et le monte, tirer un bord l'allonge,
 * tirer un rond change l'intonation, tirer une pastille change le volume. Le crayon, lui, crée un
 * morceau à partir d'un trait libre : sa durée est celle du geste, son intonation celle de la
 * ligne.
 */

type Geste =
  | { sorte: 'point'; id: number; i: number; bouge: boolean }
  | { sorte: 'volume'; id: number; i: number; yPoint: number; bouge: boolean }
  | { sorte: 'corps'; id: number; x0: number; y0: number; origine: Morceau; bouge: boolean }
  | { sorte: 'bordG' | 'bordD'; id: number; x0: number; origine: Morceau; bouge: boolean }
  | { sorte: 'trait'; bouge: boolean }

interface Cible {
  sorte: 'point' | 'volume' | 'corps' | 'bordG' | 'bordD'
  m: Morceau
  i?: number
  yPoint?: number
}

export function Frise({ tete, onEcouter }: { tete: number | null; onEcouter: (m: Morceau) => void }) {
  const son = useStudioStore((s) => s.son)
  const selId = useStudioStore((s) => s.selId)
  const mode = useStudioStore((s) => s.mode)
  const timbreCrayon = useStudioStore((s) => s.timbreCrayon)
  const choisir = useStudioStore((s) => s.choisir)
  const modifier = useStudioStore((s) => s.modifier)
  const ajouterMorceau = useStudioStore((s) => s.ajouterMorceau)
  const montrerBarreMorceau = useStudioStore((s) => s.montrerBarreMorceau)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const gesteRef = useRef<Geste | null>(null)
  const fenetreFigeeRef = useRef<number | null>(null)
  // Le trait en cours vit dans une référence, et son double en état ne sert qu'à redessiner :
  // lu en état, il arrivait vide au relâchement dès que deux évènements tombaient dans le même
  // lot de rendu.
  const traitRef = useRef<{ x: number; y: number }[] | null>(null)
  const [trait, setTrait] = useState<{ x: number; y: number }[] | null>(null)
  const [taille, setTaille] = useState({ w: 0, h: 0 })

  /**
   * Le cadre courant, mesuré sur le canevas lui-même.
   *
   * Et non sur une taille gardée en état : celle-ci pouvait rester à zéro dans la fonction qui
   * termine un trait, et le morceau créé tombait alors à l'instant zéro avec une durée minimale.
   * L'élément, lui, dit toujours sa vraie taille.
   */
  const cadre = useCallback((): Cadre => {
    const r = canvasRef.current?.getBoundingClientRect()
    const fenetre = fenetreFigeeRef.current ?? fenetreDe(son.morceaux, son.reglages)
    return { w: r?.width ?? 0, h: r?.height ?? 0, fenetre }
  }, [son])

  // ── Dessin ────────────────────────────────────────────────────────────────

  useEffect(() => {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx || !taille.w) return
    const dpr = window.devicePixelRatio || 1
    canvas.width = Math.round(taille.w * dpr)
    canvas.height = Math.round(taille.h * dpr)
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    dessinerFrise(ctx, { son, cadre: cadre(), selId, tete, trait, timbreCrayon })
  }, [son, selId, tete, trait, timbreCrayon, taille, cadre])

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

  // ── Gestes ────────────────────────────────────────────────────────────────

  function position(e: React.PointerEvent | React.MouseEvent) {
    const r = canvasRef.current!.getBoundingClientRect()
    return { x: e.clientX - r.left, y: e.clientY - r.top }
  }

  /**
   * Ce qu'on attrape à cet endroit. Les pastilles de volume passent avant les ronds de hauteur :
   * elles flottent juste au-dessus d'eux, et c'est le geste le plus fin des deux.
   */
  function toucher(p: { x: number; y: number }): Cible | null {
    const c = cadre()
    const R = son.reglages
    const choisi = son.morceaux.find((m) => m.id === selId)

    if (choisi) {
      for (let i = 0; i < choisi.courbe.length; i++) {
        const x = xDuPoint(choisi, i, c, R)
        const y = yDuPoint(choisi, i, c, R)
        if (Math.hypot(p.x - x, p.y - yPastille(choisi, i, y, R)) < 10) {
          return { sorte: 'volume', m: choisi, i, yPoint: y }
        }
      }
      for (let i = 0; i < choisi.courbe.length; i++) {
        const x = xDuPoint(choisi, i, c, R)
        const y = yDuPoint(choisi, i, c, R)
        if (Math.hypot(p.x - x, p.y - y) < 13) return { sorte: 'point', m: choisi, i }
      }
      const points = traceRuban(choisi, c, R)
      const debut = points[0]
      const fin = points[points.length - 1]
      if (Math.abs(p.x - debut.x) < 9 && Math.abs(p.y - debut.y) < 20) return { sorte: 'bordG', m: choisi }
      if (Math.abs(p.x - fin.x) < 9 && Math.abs(p.y - fin.y) < 20) return { sorte: 'bordD', m: choisi }
    }

    const t = tDe(p.x, c)
    for (let k = son.morceaux.length - 1; k >= 0; k--) {
      const m = son.morceaux[k]
      const a = m.debut / R.debit
      const b = (m.debut + m.duree) / R.debit
      if (t < a - 0.01 || t > b + 0.01) continue
      const u = borner((t - a) / (b - a), 0, 1)
      if (Math.abs(p.y - yDe(frequenceCentre(m, u, R), c)) < 18) return { sorte: 'corps', m }
    }
    return null
  }

  function onPointerDown(e: React.PointerEvent) {
    canvasRef.current?.setPointerCapture(e.pointerId)
    const p = position(e)
    const cible = toucher(p)
    fenetreFigeeRef.current = cadre().fenetre

    if (mode === 'crayon' && (!cible || cible.sorte === 'corps')) {
      traitRef.current = [p]
      setTrait([p])
      gesteRef.current = { sorte: 'trait', bouge: false }
      return
    }
    if (!cible) {
      if (selId != null) choisir(null)
      gesteRef.current = null
      fenetreFigeeRef.current = null
      return
    }
    if (selId !== cible.m.id) choisir(cible.m.id)

    const origine = structuredClone(cible.m)
    gesteRef.current =
      cible.sorte === 'point'
        ? { sorte: 'point', id: cible.m.id, i: cible.i!, bouge: false }
        : cible.sorte === 'volume'
          ? { sorte: 'volume', id: cible.m.id, i: cible.i!, yPoint: cible.yPoint!, bouge: false }
          : cible.sorte === 'corps'
            ? { sorte: 'corps', id: cible.m.id, x0: p.x, y0: p.y, origine, bouge: false }
            : { sorte: cible.sorte, id: cible.m.id, x0: p.x, origine, bouge: false }
  }

  function onPointerMove(e: React.PointerEvent) {
    const p = position(e)
    const geste = gesteRef.current
    if (!geste) {
      const c = toucher(p)
      const canvas = canvasRef.current
      if (canvas) {
        canvas.style.cursor =
          mode === 'crayon'
            ? 'crosshair'
            : !c
              ? 'default'
              : c.sorte === 'point' || c.sorte === 'volume'
                ? 'ns-resize'
                : c.sorte === 'corps'
                  ? 'grab'
                  : 'ew-resize'
      }
      return
    }

    if (geste.sorte === 'trait') {
      traitRef.current = [...(traitRef.current ?? []), p]
      setTrait(traitRef.current)
      return
    }

    const c = cadre()
    const R = son.reglages
    const premier = !geste.bouge
    if (geste.sorte === 'corps' || geste.sorte === 'bordG' || geste.sorte === 'bordD') {
      if (premier && Math.abs(p.x - geste.x0) < 3 && (geste.sorte !== 'corps' || Math.abs(p.y - geste.y0) < 3)) return
    }
    geste.bouge = true

    modifier((s) => {
      const m = s.morceaux.find((x) => x.id === geste.id)
      if (!m) return
      if (geste.sorte === 'point') {
        m.courbe[geste.i] = borner(fDe(p.y, c) / (baseHz(m.timbre) * R.hauteur), 0.3, 3.5)
      } else if (geste.sorte === 'volume') {
        volumes(m)[geste.i] = borner((geste.yPoint - ECART_PASTILLE - p.y) / epaisseurMax(m, R), 0, 1)
      } else {
        const dt = (tDe(p.x, c) - tDe(geste.x0, c)) * R.debit
        if (geste.sorte === 'corps') {
          m.debut = Math.max(0, geste.origine.debut + dt)
          const facteur = fDe(p.y, c) / fDe(geste.y0, c)
          m.courbe = geste.origine.courbe.map((r) => borner(r * facteur, 0.3, 3.5))
        } else if (geste.sorte === 'bordD') {
          m.duree = Math.max(0.04, geste.origine.duree + dt)
        } else {
          const debut = borner(geste.origine.debut + dt, 0, geste.origine.debut + geste.origine.duree - 0.04)
          m.debut = debut
          m.duree = geste.origine.debut + geste.origine.duree - debut
        }
      }
    }, premier)
  }

  function onPointerUp(e: React.PointerEvent) {
    const geste = gesteRef.current
    gesteRef.current = null
    fenetreFigeeRef.current = null
    if (!geste) return
    // Le geste est fini : la barre du morceau peut prendre sa place sans rien déplacer sous la souris.
    montrerBarreMorceau()

    if (geste.sorte === 'trait') {
      finirTrait(position(e))
      return
    }
    if (geste.bouge) {
      const m = son.morceaux.find((x) => x.id === geste.id)
      if (m) onEcouter(m)
    }
  }

  /**
   * Un trait à main levée devient un morceau : quelques points repris sur la ligne, qu'on pourra
   * ensuite reprendre un à un.
   */
  function finirTrait(fin: { x: number; y: number }) {
    const points = traitRef.current
    traitRef.current = null
    setTrait(null)
    if (!points || points.length < 3 || Math.abs(fin.x - points[0].x) < 14) return

    const c = cadre()
    const R = son.reglages
    const suite = points
      .map((q) => ({ t: Math.max(0, tDe(q.x, c)), f: fDe(q.y, c) }))
      .sort((a, b) => a.t - b.t)
    const t0 = suite[0].t
    const t1 = suite[suite.length - 1].t
    const n = borner(Math.round((t1 - t0) / 0.08) + 1, 2, 7)
    const base = baseHz(timbreCrayon) * R.hauteur
    const courbe = Array.from({ length: n }, (_, j) => {
      const vise = t0 + avancementDuPoint(timbreCrayon, n, j) * (t1 - t0)
      let meilleur = suite[0]
      for (const s of suite) if (Math.abs(s.t - vise) < Math.abs(meilleur.t - vise)) meilleur = s
      return borner(meilleur.f / base, 0.3, 3.5)
    })
    const monte = courbe[n - 1] > courbe[0]
    const m = nouveauMorceau({
      timbre: timbreCrayon,
      debut: t0 * R.debit,
      duree: Math.max(0.05, (t1 - t0) * R.debit),
      courbe,
      v1: (monte ? 'o' : 'a') as Voyelle,
      v2: (monte ? 'i' : 'o') as Voyelle,
    })
    ajouterMorceau(m)
    onEcouter(m)
  }

  /** Double-toucher : un point de plus sur un morceau, ou un morceau de plus dans le vide. */
  function onDoubleClick(e: React.MouseEvent) {
    if (mode !== 'main') return
    const p = position(e)
    const cible = toucher(p)
    const c = cadre()
    const R = son.reglages

    if (cible && (cible.sorte === 'corps' || cible.sorte === 'point')) {
      const id = cible.m.id
      modifier((s) => {
        const m = s.morceaux.find((x) => x.id === id)
        if (!m) return
        const n = m.courbe.length + 1
        const note = m.timbre === 'note'
        const relire = (valeurs: number[]) =>
          Array.from({ length: n }, (_, i) => valeurA(m.timbre, valeurs, note ? (i + 0.5) / n : i / (n - 1)))
        const anciensVolumes = [...volumes(m)]
        m.courbe = relire([...m.courbe])
        m.volumes = relire(anciensVolumes)

        const u = borner((tDe(p.x, c) - m.debut / R.debit) / (m.duree / R.debit), 0, 1)
        let proche = 0
        for (let i = 1; i < n; i++) {
          if (Math.abs(avancementDuPoint(m.timbre, n, i) - u) < Math.abs(avancementDuPoint(m.timbre, n, proche) - u)) proche = i
        }
        m.courbe[proche] = borner(fDe(p.y, c) / (baseHz(m.timbre) * R.hauteur), 0.3, 3.5)
      })
      choisir(id)
      return
    }
    if (!cible) {
      const m = morceauA('voix', tDe(p.x, c) * R.debit, fDe(p.y, c), R)
      ajouterMorceau(m)
      onEcouter(m)
    }
  }

  return (
    <div className={styles.cadre}>
      <canvas
        ref={canvasRef}
        className={styles.toile}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onDoubleClick={onDoubleClick}
      />
      {son.morceaux.length === 0 && !trait && (
        <div className={styles.vide}>
          <b>Rien à entendre pour l’instant</b>
          Choisis une humeur, pioche un modèle juste au-dessus,
          <br />
          ou prends le crayon et dessine le son.
        </div>
      )}
    </div>
  )
}
