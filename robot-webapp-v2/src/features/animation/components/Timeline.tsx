import { useEffect, useRef, useCallback } from 'react'
import { MARGE_FIN_PX, useAnimationStore, type EditorTrack } from '../store/animationStore'
import { trackVal } from '../utils/catmullRom'
import styles from './Timeline.module.css'

const TH = 88    // track height px
const RH = 26    // ruler height px
const KR = 7     // keyframe diamond half-size
const PAD = 10   // marge haute et basse d'une piste, px

// ── Helpers coordonnées ──────────────────────────────────────────────────────

function tToX(t: number, pxPerMs: number, scrollX: number) {
  return t * pxPerMs - scrollX
}
function xToT(x: number, pxPerMs: number, scrollX: number) {
  return (x + scrollX) / pxPerMs
}
function vToY(v: number, tr: EditorTrack, ti: number) {
  const h = TH - 2 * PAD
  return ti * TH + PAD + (tr.max - v) / (tr.max - tr.min) * h
}
function yToV(y: number, tr: EditorTrack, ti: number) {
  const h = TH - 2 * PAD
  return tr.max - (y - ti * TH - PAD) / h * (tr.max - tr.min)
}
function tiAtY(y: number) { return Math.floor(y / TH) }

/**
 * Pas des graduations, en ms, selon le zoom. Partagé par la règle et par les repères verticaux des
 * pistes : s'ils le calculaient chacun de leur côté, les lignes finiraient à côté des graduations.
 */
function pasDeLaRegle(pxPerMs: number) {
  return pxPerMs < 0.05 ? 2000 : pxPerMs < 0.12 ? 1000 : pxPerMs < 0.3 ? 500 : 250
}

// ── Aimant ───────────────────────────────────────────────────────────────────

/** En pixels et non en unités : la sensation est la même à tous les zooms, et le doigt s'y retrouve. */
const SEUIL_AIMANT_PX = 8

/** Où l'image-clé déplacée s'est collée ; null sur un axe resté libre. */
interface Guides { t: number | null; v: number | null; ti: number }

/**
 * Colle l'image-clé en cours de déplacement aux repères proches.
 *
 * L'instant se colle à celui d'une image-clé d'une AUTRE piste — deux images-clés au même instant sur
 * une même piste n'ont pas de sens — et, à défaut, à une graduation. La valeur se colle à celle d'une
 * autre image-clé de SA piste — les autres pistes n'ont ni la même unité ni les mêmes butées — et, à
 * défaut, au zéro, la posture de repos. Les images-clés passent avant : ce sont elles qu'on aligne.
 */
function aimanter(tracks: EditorTrack[], ti: number, kfId: string, t: number, v: number, pxPerMs: number) {
  const piste = tracks[ti]
  const seuilMs = SEUIL_AIMANT_PX / pxPerMs
  const seuilDegres = SEUIL_AIMANT_PX * (piste.max - piste.min) / (TH - 2 * PAD)
  const instants = tracks.flatMap((autre, i) => i === ti ? [] : autre.kfs.map(k => k.t))
  const pas = pasDeLaRegle(pxPerMs)
  const guideT = plusProche(t, instants, seuilMs) ?? plusProche(t, [Math.round(t / pas) * pas], seuilMs)
  const valeurs = piste.kfs.filter(k => k.id !== kfId).map(k => k.v)
  const zero = piste.min <= 0 && piste.max >= 0 ? [0] : []
  const guideV = plusProche(v, valeurs, seuilDegres) ?? plusProche(v, zero, seuilDegres)
  return { t: guideT ?? t, v: guideV ?? v, guideT, guideV }
}

function plusProche(valeur: number, cibles: number[], seuil: number): number | null {
  let retenue: number | null = null
  for (const cible of cibles) {
    const ecart = Math.abs(cible - valeur)
    if (ecart <= seuil && (retenue === null || ecart < Math.abs(retenue - valeur))) retenue = cible
  }
  return retenue
}

// Les butées du cou sortent d'une transmission (−39,8 = (75 − 67) × −4,97) et arrivent du robot avec
// toutes leurs décimales : « 52.980199999999996° » dans l'étiquette d'une piste.
function auDixieme(v: number) { return Math.round(v * 10) / 10 }

// « 0,25 s » et non « 0:25 », qui se lisait comme vingt-cinq secondes. Pas plus de décimales que le
// pas de la règle n'en demande : « 1 s », « 1,5 s », « 1,25 s ».
function enSecondes(ms: number) {
  return (ms / 1000).toLocaleString('fr-FR', { maximumFractionDigits: 2 }) + ' s'
}

// ── Rendu canvas ─────────────────────────────────────────────────────────────

function renderTimeline(
  ctx: CanvasRenderingContext2D,
  W: number, H: number,
  tracks: EditorTrack[],
  playhead: number, totalMs: number,
  pxPerMs: number, scrollX: number,
  selectedKf: { trackId: string; kfId: string } | null,
  guides: Guides | null,
  couleurGuide: string,
) {
  ctx.clearRect(0, 0, W, H)
  const pas = pasDeLaRegle(pxPerMs)
  const tPremierRepere = Math.floor(xToT(0, pxPerMs, scrollX) / pas) * pas

  tracks.forEach((tr, ti) => {
    const y0 = ti * TH
    const disabled = !tr.enabled

    ctx.save()
    if (disabled) ctx.globalAlpha = 0.28

    // Fond alterné
    ctx.fillStyle = ti % 2 === 0 ? '#161b22' : '#0d1117'
    ctx.fillRect(0, y0, W, TH)

    // Repères verticaux sous les graduations : sans eux, aligner une image-clé sur celle d'une autre
    // piste se faisait à l'œil. Plus marqués aux secondes rondes, pour qu'on les compte sans la règle.
    ctx.lineWidth = 1
    for (let t = tPremierRepere; t <= totalMs; t += pas) {
      const x = tToX(t, pxPerMs, scrollX)
      if (x < 0 || x > W) continue
      const xNet = Math.round(x) + 0.5   // sur un demi-pixel, un trait d'un pixel ne bave pas sur deux
      ctx.strokeStyle = t % 1000 === 0 ? '#2c343e' : '#1f252d'
      ctx.beginPath(); ctx.moveTo(xNet, y0); ctx.lineTo(xNet, y0 + TH); ctx.stroke()
    }

    // Ligne zéro
    const zy = vToY(0, tr, ti)
    ctx.strokeStyle = '#30363d'
    ctx.lineWidth = 1
    ctx.setLineDash([4, 4])
    ctx.beginPath(); ctx.moveTo(0, zy); ctx.lineTo(W, zy); ctx.stroke()
    ctx.setLineDash([])

    // Courbe Catmull-Rom
    const sorted = [...tr.kfs].sort((a, b) => a.t - b.t)
    if (sorted.length >= 2) {
      ctx.strokeStyle = disabled ? '#555' : tr.color + 'aa'
      ctx.lineWidth = 1.5
      ctx.beginPath()
      const step = Math.max(1, Math.round(1 / pxPerMs))
      for (let t = 0; t <= totalMs; t += step) {
        const x = tToX(t, pxPerMs, scrollX)
        const v = trackVal(sorted, t, tr.min, tr.max)
        const y = vToY(v, tr, ti)
        t === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
      }
      ctx.stroke()
    }

    // Keyframes (diamants)
    tr.kfs.forEach(kf => {
      const x = tToX(kf.t, pxPerMs, scrollX)
      const y = vToY(kf.v, tr, ti)
      const isSelected = selectedKf?.trackId === tr.id && selectedKf?.kfId === kf.id
      ctx.save()
      ctx.translate(x, y)
      ctx.rotate(Math.PI / 4)
      ctx.fillStyle = disabled ? '#444' : (isSelected ? '#fff' : tr.color)
      ctx.strokeStyle = isSelected ? tr.color : '#000a'
      ctx.lineWidth = isSelected ? 2 : 1
      ctx.fillRect(-KR, -KR, KR * 2, KR * 2)
      ctx.strokeRect(-KR, -KR, KR * 2, KR * 2)
      ctx.restore()
    })

    ctx.restore()

    // Séparateur bas (toujours pleine opacité)
    ctx.strokeStyle = '#30363d'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(0, y0 + TH); ctx.lineTo(W, y0 + TH)
    ctx.stroke()
  })

  // Guides de l'aimant, par-dessus les courbes : ils disent à quoi l'image-clé vient de se coller.
  if (guides) {
    ctx.save()
    ctx.strokeStyle = couleurGuide
    ctx.lineWidth = 1
    ctx.setLineDash([5, 4])
    if (guides.t !== null) {
      const x = Math.round(tToX(guides.t, pxPerMs, scrollX)) + 0.5
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke()
    }
    if (guides.v !== null) {
      const y = Math.round(vToY(guides.v, tracks[guides.ti], guides.ti)) + 0.5
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke()
    }
    ctx.restore()
  }

  // Playhead
  const px = tToX(playhead, pxPerMs, scrollX)
  ctx.strokeStyle = '#fff'
  ctx.lineWidth = 1.5
  ctx.beginPath(); ctx.moveTo(px, 0); ctx.lineTo(px, H); ctx.stroke()

  // Triangle tête du playhead
  ctx.fillStyle = '#fff'
  ctx.beginPath()
  ctx.moveTo(px - 5, 0); ctx.lineTo(px + 5, 0); ctx.lineTo(px, 8)
  ctx.fill()
}

function renderRuler(
  ctx: CanvasRenderingContext2D,
  W: number,
  totalMs: number, pxPerMs: number, scrollX: number,
) {
  ctx.clearRect(0, 0, W, RH)
  ctx.fillStyle = '#161b22'
  ctx.fillRect(0, 0, W, RH)

  const step = pasDeLaRegle(pxPerMs)
  const tStart = Math.floor(xToT(0, pxPerMs, scrollX) / step) * step

  ctx.strokeStyle = '#30363d'
  ctx.fillStyle = '#8b949e'
  ctx.font = '10px monospace'
  ctx.lineWidth = 1

  for (let t = tStart; t <= totalMs + step; t += step) {
    const x = tToX(t, pxPerMs, scrollX)
    if (x < 0 || x > W) continue
    ctx.beginPath(); ctx.moveTo(x, RH - 6); ctx.lineTo(x, RH); ctx.stroke()
    ctx.fillText(enSecondes(t), x + 3, RH - 8)
  }
}

// ── Composant ────────────────────────────────────────────────────────────────

export function Timeline() {
  const store = useAnimationStore()
  const tlRef  = useRef<HTMLCanvasElement>(null)
  const rulRef = useRef<HTMLCanvasElement>(null)
  const wrapRef = useRef<HTMLDivElement>(null)
  const rulWrapRef = useRef<HTMLDivElement>(null)
  const barreRef = useRef<HTMLDivElement>(null)
  const guidesRef = useRef<Guides | null>(null)
  const dpr = window.devicePixelRatio || 1

  // État drag local (ref pour ne pas trigger de re-render)
  const drag = useRef<{
    type: 'kf' | 'ph' | 'ruler'
    trackId?: string
    kfId?: string
    startX?: number
    startY?: number
    origT?: number
    origV?: number
  } | null>(null)

  // ── Resize + redraw ──────────────────────────────────────────────────────

  const resize = useCallback(() => {
    const tl = tlRef.current, rul = rulRef.current
    const wrap = wrapRef.current, rulWrap = rulWrapRef.current
    if (!tl || !rul || !wrap || !rulWrap) return

    const cw = wrap.clientWidth
    const ch = TH * store.tracks.length
    const rw = rulWrap.clientWidth

    tl.style.width = cw + 'px'; tl.style.height = ch + 'px'
    tl.width = cw * dpr; tl.height = ch * dpr
    rul.style.width = rw + 'px'; rul.style.height = RH + 'px'
    rul.width = rw * dpr; rul.height = RH * dpr
    useAnimationStore.getState().setLargeurVisible(cw)

    draw()
  }, [store.tracks.length, dpr])

  function draw() {
    const { tracks, playhead, totalMs, pxPerMs, scrollX, selectedKf } = useAnimationStore.getState()
    const tl = tlRef.current, rul = rulRef.current
    if (!tl || !rul) return

    const ctx  = tl.getContext('2d')!
    const rctx = rul.getContext('2d')!
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    rctx.setTransform(dpr, 0, 0, dpr, 0, 0)

    const couleurGuide = getComputedStyle(tl).getPropertyValue('--accent').trim() || '#f4a72c'
    renderTimeline(ctx, tl.clientWidth, tl.clientHeight, tracks, playhead, totalMs, pxPerMs, scrollX, selectedKf,
      guidesRef.current, couleurGuide)
    renderRuler(rctx, rul.clientWidth, totalMs, pxPerMs, scrollX)
  }

  useEffect(() => {
    draw()
  })

  useEffect(() => {
    const obs = new ResizeObserver(resize)
    if (wrapRef.current)    obs.observe(wrapRef.current)
    if (rulWrapRef.current) obs.observe(rulWrapRef.current)
    return () => obs.disconnect()
  }, [resize])

  // La barre suit le store, et non l'inverse : molette, zoom et suivi du curseur écrivent tous
  // scrollX, et elle doit refléter chacun d'eux.
  useEffect(() => {
    const barre = barreRef.current
    if (barre && Math.abs(barre.scrollLeft - store.scrollX) > 0.5) barre.scrollLeft = store.scrollX
  })

  // Le curseur qui sort de la vue la ramène à lui — en lecture comme après Début ou Fin. Sans ça,
  // une lecture zoomée filait hors de l'écran, et Début renvoyait à zéro sans montrer zéro. Un
  // défilement à la main ne bouge pas le curseur : il n'est donc jamais contrarié.
  useEffect(() => {
    const { playhead, pxPerMs, scrollX, largeurVisible, setScrollX } = useAnimationStore.getState()
    if (largeurVisible <= 0) return
    const x = playhead * pxPerMs - scrollX
    if (x < 0 || x > largeurVisible) setScrollX(playhead * pxPerMs - largeurVisible * 0.1)
  }, [store.playhead])

  // Les événements wheel doivent être non-passifs pour pouvoir appeler preventDefault()
  // et éviter que Ctrl+Wheel déclenche le zoom du navigateur.
  useEffect(() => {
    const tl  = tlRef.current
    const rul = rulRef.current
    if (!tl || !rul) return

    const handleTlWheel  = (e: WheelEvent) => { e.preventDefault(); onWheelNative(e, false) }
    const handleRulWheel = (e: WheelEvent) => { e.preventDefault(); onWheelNative(e, true)  }

    tl.addEventListener ('wheel', handleTlWheel,  { passive: false })
    rul.addEventListener('wheel', handleRulWheel, { passive: false })
    return () => {
      tl.removeEventListener ('wheel', handleTlWheel)
      rul.removeEventListener('wheel', handleRulWheel)
    }
  })

  // ── Hit tests ────────────────────────────────────────────────────────────

  function hitKf(x: number, y: number) {
    const { tracks, pxPerMs, scrollX } = store
    const ti = tiAtY(y)
    if (ti < 0 || ti >= tracks.length) return null
    const tr = tracks[ti]
    for (const kf of tr.kfs) {
      const kx = tToX(kf.t, pxPerMs, scrollX)
      const ky = vToY(kf.v, tr, ti)
      if (Math.hypot(x - kx, y - ky) <= KR + 5)
        return { trackId: tr.id as any, kfId: kf.id, tr, kf, ti }
    }
    return null
  }

  function hitPlayhead(x: number) {
    return Math.abs(x - tToX(store.playhead, store.pxPerMs, store.scrollX)) < 8
  }

  // ── Souris timeline ──────────────────────────────────────────────────────

  function onMouseDown(e: React.MouseEvent<HTMLCanvasElement>) {
    if (e.button !== 0) return
    const rect = tlRef.current!.getBoundingClientRect()
    const x = e.clientX - rect.left, y = e.clientY - rect.top
    const { pxPerMs, scrollX, totalMs, tracks } = store

    if (hitPlayhead(x)) {
      store.setPlaying(false)
      drag.current = { type: 'ph' }
      return
    }

    const hit = hitKf(x, y)
    if (hit) {
      store.snapshot()
      store.selectKf(hit.trackId, hit.kfId)
      drag.current = { type: 'kf', trackId: hit.trackId, kfId: hit.kfId, startX: x, startY: y, origT: hit.kf.t, origV: hit.kf.v }
      return
    }

    // Ajouter keyframe
    const ti = tiAtY(y)
    if (ti >= 0 && ti < tracks.length) {
      const tr = tracks[ti]
      const t = Math.max(0, Math.min(Math.round(xToT(x, pxPerMs, scrollX) / 50) * 50, totalMs))
      const v = Math.max(tr.min, Math.min(tr.max, yToV(y, tr, ti)))
      const kf = store.addKf(tr.id as any, t, v)
      store.selectKf(tr.id as any, kf.id)
      drag.current = { type: 'kf', trackId: tr.id, kfId: kf.id, startX: x, startY: y, origT: t, origV: v }
    }
  }

  function onMouseMove(e: React.MouseEvent<HTMLCanvasElement>) {
    const rect = tlRef.current!.getBoundingClientRect()
    const x = e.clientX - rect.left, y = e.clientY - rect.top
    const { pxPerMs, scrollX, totalMs, tracks } = store

    if (!drag.current) {
      const isOnPh = hitPlayhead(x)
      const isOnKf = !!hitKf(x, y)
      tlRef.current!.style.cursor = isOnPh ? 'ew-resize' : isOnKf ? 'grab' : 'crosshair'
      return
    }

    if (drag.current.type === 'ph') {
      store.setPlayhead(Math.max(0, Math.min(xToT(x, pxPerMs, scrollX), totalMs)))
      return
    }

    if (drag.current.type === 'kf') {
      const { trackId, kfId, startX, startY, origT, origV } = drag.current as any
      const ti = tracks.findIndex(t => t.id === trackId)
      const tr = tracks[ti]
      const h = TH - 2 * PAD
      let newT = Math.max(0, Math.min(origT + (x - startX) / pxPerMs, totalMs))
      let newV = Math.max(tr.min, Math.min(tr.max, origV - (y - startY) * (tr.max - tr.min) / h))
      // Alt inverse l'aimant le temps d'un geste : aller le couper dans la barre pour un seul
      // placement serait plus pénible que de le subir.
      if (useAnimationStore.getState().aimant !== e.altKey) {
        const collage = aimanter(tracks, ti, kfId, newT, newV, pxPerMs)
        newT = collage.t
        newV = collage.v
        guidesRef.current = collage.guideT !== null || collage.guideV !== null
          ? { t: collage.guideT, v: collage.guideV, ti } : null
      } else {
        guidesRef.current = null
      }
      store.moveKf(trackId, kfId, newT, newV)
    }
  }

  function onMouseUp() {
    drag.current = null
    if (guidesRef.current) {
      guidesRef.current = null
      draw()
    }
  }

  function onDblClick(e: React.MouseEvent<HTMLCanvasElement>) {
    const rect = tlRef.current!.getBoundingClientRect()
    const hit = hitKf(e.clientX - rect.left, e.clientY - rect.top)
    if (hit) store.deleteKf(hit.trackId, hit.kfId)
  }

  function onWheelNative(e: WheelEvent, fromRuler: boolean) {
    const rect = (fromRuler ? rulRef.current! : tlRef.current!).getBoundingClientRect()
    const x = e.clientX - rect.left
    if (e.ctrlKey || e.metaKey) {
      store.zoom(e.deltaY < 0 ? 1.18 : 1 / 1.18, x, rect.width)
    } else {
      const { scrollX } = useAnimationStore.getState()
      store.setScrollX(Math.max(0, scrollX + (e.deltaX || 0) + e.deltaY * 0.4))
    }
  }

  // ── Ruler scrub ──────────────────────────────────────────────────────────

  function onRulerDown(e: React.MouseEvent<HTMLCanvasElement>) {
    const rect = rulRef.current!.getBoundingClientRect()
    store.setPlaying(false)
    store.setPlayhead(Math.max(0, Math.min(xToT(e.clientX - rect.left, store.pxPerMs, store.scrollX), store.totalMs)))
    drag.current = { type: 'ruler' }
  }

  function onRulerMove(e: React.MouseEvent<HTMLCanvasElement>) {
    if (!drag.current) return
    const rect = rulRef.current!.getBoundingClientRect()
    store.setPlayhead(Math.max(0, Math.min(xToT(e.clientX - rect.left, store.pxPerMs, store.scrollX), store.totalMs)))
  }

  const H = TH * store.tracks.length

  return (
    <div className={styles.tlArea}>
      {/* Ruler */}
      <div className={styles.tlHead}>
        <div className={styles.labelHeader}>ACTUATEURS</div>
        <div className={styles.rulerWrap} ref={rulWrapRef}>
          <canvas
            ref={rulRef}
            onMouseDown={onRulerDown}
            onMouseMove={onRulerMove}
            onMouseUp={onMouseUp}
          />
        </div>
      </div>

      {/* Body */}
      <div className={styles.tlBody}>
        {/* Labels */}
        <div className={styles.labelsCol}>
          {store.tracks.map(tr => {
            const v = trackVal([...tr.kfs].sort((a, b) => a.t - b.t), store.playhead, tr.min, tr.max)
            return (
              <div
                key={tr.id}
                className={`${styles.trackLabel} ${!tr.enabled ? styles.trackDisabled : ''}`}
                style={{ height: TH }}
              >
                <div className={styles.tlName}>
                  <button
                    className={styles.trackToggle}
                    onClick={() => store.toggleTrack(tr.id)}
                    title={tr.enabled ? 'Désactiver la track' : 'Activer la track'}
                  >
                    <span className={styles.dot} style={{ background: tr.enabled ? tr.color : '#444' }} />
                  </button>
                  {tr.name}
                </div>
                <div className={styles.tlRange}>{auDixieme(tr.min)}° / {auDixieme(tr.max)}°</div>
                <div className={styles.tlVal}>{v.toFixed(1)}°</div>
              </div>
            )
          })}
        </div>

        <div className={styles.colonnePistes}>
          <div className={styles.canvasWrap} ref={wrapRef} style={{ height: H }}>
            <canvas
              ref={tlRef}
              onMouseDown={onMouseDown}
              onMouseMove={onMouseMove}
              onMouseUp={onMouseUp}
              onDoubleClick={onDblClick}
            />
          </div>
          <div
            ref={barreRef}
            className={styles.barre}
            onScroll={e => {
              const s = e.currentTarget.scrollLeft
              if (Math.abs(s - useAnimationStore.getState().scrollX) > 0.5) store.setScrollX(s)
            }}
          >
            <div style={{ width: store.totalMs * store.pxPerMs + MARGE_FIN_PX }} />
          </div>
        </div>
      </div>
    </div>
  )
}
