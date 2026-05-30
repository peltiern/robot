import { useEffect, useRef, useCallback } from 'react'
import { useAnimationStore, type EditorTrack } from '../store/animationStore'
import { trackVal } from '../utils/catmullRom'
import styles from './Timeline.module.css'

const TH = 88    // track height px
const RH = 26    // ruler height px
const KR = 7     // keyframe diamond half-size

// ── Helpers coordonnées ──────────────────────────────────────────────────────

function tToX(t: number, pxPerMs: number, scrollX: number) {
  return t * pxPerMs - scrollX
}
function xToT(x: number, pxPerMs: number, scrollX: number) {
  return (x + scrollX) / pxPerMs
}
function vToY(v: number, tr: EditorTrack, ti: number) {
  const PAD = 10, h = TH - 2 * PAD
  return ti * TH + PAD + (tr.max - v) / (tr.max - tr.min) * h
}
function yToV(y: number, tr: EditorTrack, ti: number) {
  const PAD = 10, h = TH - 2 * PAD
  return tr.max - (y - ti * TH - PAD) / h * (tr.max - tr.min)
}
function tiAtY(y: number) { return Math.floor(y / TH) }

// ── Rendu canvas ─────────────────────────────────────────────────────────────

function renderTimeline(
  ctx: CanvasRenderingContext2D,
  W: number, H: number,
  tracks: EditorTrack[],
  playhead: number, totalMs: number,
  pxPerMs: number, scrollX: number,
  selectedKf: { trackId: string; kfId: string } | null,
) {
  ctx.clearRect(0, 0, W, H)

  tracks.forEach((tr, ti) => {
    const y0 = ti * TH

    // Fond alterné
    ctx.fillStyle = ti % 2 === 0 ? '#161b22' : '#0d1117'
    ctx.fillRect(0, y0, W, TH)

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
      ctx.strokeStyle = tr.color + 'aa'
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
      ctx.fillStyle = isSelected ? '#fff' : tr.color
      ctx.strokeStyle = isSelected ? tr.color : '#000a'
      ctx.lineWidth = isSelected ? 2 : 1
      ctx.fillRect(-KR, -KR, KR * 2, KR * 2)
      ctx.strokeRect(-KR, -KR, KR * 2, KR * 2)
      ctx.restore()
    })

    // Séparateur bas
    ctx.strokeStyle = '#30363d'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(0, y0 + TH); ctx.lineTo(W, y0 + TH)
    ctx.stroke()
  })

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

  const step = pxPerMs < 0.05 ? 2000 : pxPerMs < 0.12 ? 1000 : pxPerMs < 0.3 ? 500 : 250
  const tStart = Math.floor(xToT(0, pxPerMs, scrollX) / step) * step

  ctx.strokeStyle = '#30363d'
  ctx.fillStyle = '#8b949e'
  ctx.font = '10px monospace'
  ctx.lineWidth = 1

  for (let t = tStart; t <= totalMs + step; t += step) {
    const x = tToX(t, pxPerMs, scrollX)
    if (x < 0 || x > W) continue
    ctx.beginPath(); ctx.moveTo(x, RH - 6); ctx.lineTo(x, RH); ctx.stroke()
    const s = Math.floor(t / 1000)
    const cs = Math.floor((t % 1000) / 10)
    ctx.fillText(`${s}:${String(cs).padStart(2, '0')}`, x + 3, RH - 8)
  }
}

// ── Composant ────────────────────────────────────────────────────────────────

export function Timeline() {
  const store = useAnimationStore()
  const tlRef  = useRef<HTMLCanvasElement>(null)
  const rulRef = useRef<HTMLCanvasElement>(null)
  const wrapRef = useRef<HTMLDivElement>(null)
  const rulWrapRef = useRef<HTMLDivElement>(null)
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

    renderTimeline(ctx, tl.clientWidth, tl.clientHeight, tracks, playhead, totalMs, pxPerMs, scrollX, selectedKf)
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
      const tr = tracks.find(t => t.id === trackId)!
      const PAD = 10, h = TH - 2 * PAD
      const newT = Math.max(0, Math.min(origT + (x - startX) / pxPerMs, totalMs))
      const newV = Math.max(tr.min, Math.min(tr.max, origV - (y - startY) * (tr.max - tr.min) / h))
      store.moveKf(trackId, kfId, newT, newV)
    }
  }

  function onMouseUp() { drag.current = null }

  function onDblClick(e: React.MouseEvent<HTMLCanvasElement>) {
    const rect = tlRef.current!.getBoundingClientRect()
    const hit = hitKf(e.clientX - rect.left, e.clientY - rect.top)
    if (hit) store.deleteKf(hit.trackId, hit.kfId)
  }

  function onWheel(e: React.WheelEvent<HTMLCanvasElement>, fromRuler = false) {
    e.preventDefault()
    const rect = (fromRuler ? rulRef.current! : tlRef.current!).getBoundingClientRect()
    const x = e.clientX - rect.left
    if (e.ctrlKey || e.metaKey) {
      store.zoom(e.deltaY < 0 ? 1.18 : 1 / 1.18, x, rect.width)
    } else {
      store.setScrollX(Math.max(0, store.scrollX + (e.deltaX || 0) + e.deltaY * 0.4))
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
            onWheel={e => onWheel(e, true)}
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
              <div key={tr.id} className={styles.trackLabel} style={{ height: TH }}>
                <div className={styles.tlName}>
                  <span className={styles.dot} style={{ background: tr.color }} />
                  {tr.name}
                </div>
                <div className={styles.tlRange}>{tr.min}° / {tr.max}°</div>
                <div className={styles.tlVal}>{v.toFixed(1)}°</div>
              </div>
            )
          })}
        </div>

        {/* Canvas */}
        <div className={styles.canvasWrap} ref={wrapRef} style={{ height: H }}>
          <canvas
            ref={tlRef}
            onMouseDown={onMouseDown}
            onMouseMove={onMouseMove}
            onMouseUp={onMouseUp}
            onDoubleClick={onDblClick}
            onWheel={e => onWheel(e, false)}
          />
        </div>
      </div>
    </div>
  )
}
