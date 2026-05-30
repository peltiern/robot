export interface KF { t: number; v: number }

/** Interpolation Catmull-Rom — portage exact depuis animation-editor.html */
export function catmullRomAt(kfs: KF[], t: number): number {
  if (kfs.length === 0) return 0
  if (kfs.length === 1) return kfs[0].v
  if (t <= kfs[0].t) return kfs[0].v
  if (t >= kfs[kfs.length - 1].t) return kfs[kfs.length - 1].v

  let i = 1
  while (i < kfs.length && kfs[i].t <= t) i++

  const p1 = kfs[i - 1], p2 = kfs[i]
  const dt = p2.t - p1.t
  const p0 = i > 1 ? kfs[i - 2] : { t: p1.t - dt, v: p1.v }
  const p3 = i < kfs.length - 1 ? kfs[i + 1] : { t: p2.t + dt, v: p2.v }

  const u = (t - p1.t) / dt
  const u2 = u * u, u3 = u2 * u

  return 0.5 * (
    2 * p1.v +
    (-p0.v + p2.v) * u +
    (2 * p0.v - 5 * p1.v + 4 * p2.v - p3.v) * u2 +
    (-p0.v + 3 * p1.v - 3 * p2.v + p3.v) * u3
  )
}

export function clamp(v: number, min: number, max: number) {
  return Math.max(min, Math.min(max, v))
}

export function trackVal(kfs: KF[], t: number, min: number, max: number): number {
  const sorted = [...kfs].sort((a, b) => a.t - b.t)
  return clamp(catmullRomAt(sorted, t), min, max)
}
