import {
  type Cadre,
  MARGE_D,
  MARGE_G,
  morceauA,
  REGLE,
  tDe,
  traceRuban,
  xDe,
  xDuPoint,
  yDe,
  yDuPoint,
  yPastille,
} from './geometrie'
import type { Morceau, Reglages, Son, Timbre } from './synthese/types'

/**
 * Le dessin de la frise et de l'onde.
 *
 * Tout passe par le canevas et non par du SVG : un morceau se trace en quelques centaines de
 * segments, et il y en a une douzaine à redessiner à chaque image pendant qu'on tire un point.
 */

const COULEURS: Record<Timbre, string> = {
  voix: '--t-voix',
  sweep: '--t-sweep',
  note: '--t-note',
  trill: '--t-trill',
  warble: '--t-warble',
  blat: '--t-blat',
}

const jeton = (nom: string) => getComputedStyle(document.documentElement).getPropertyValue(nom).trim()

export const couleurTimbre = (timbre: Timbre) => jeton(COULEURS[timbre])

const motifs = new Map<string, CanvasPattern | null>()

/** Hachures de la bande de roulement, fabriquées une fois par couleur. */
function hachures(ctx: CanvasRenderingContext2D, couleur: string): string | CanvasPattern {
  const connu = motifs.get(couleur)
  if (connu) return connu
  const carreau = document.createElement('canvas')
  carreau.width = 8
  carreau.height = 8
  const g = carreau.getContext('2d')
  if (!g) return couleur
  g.strokeStyle = couleur
  g.lineWidth = 1.4
  ;[
    [0, 8, 8, 0],
    [-2, 2, 2, -2],
    [6, 10, 10, 6],
  ].forEach(([x1, y1, x2, y2]) => {
    g.beginPath()
    g.moveTo(x1, y1)
    g.lineTo(x2, y2)
    g.stroke()
  })
  const motif = ctx.createPattern(carreau, 'repeat')
  motifs.set(couleur, motif)
  return motif ?? couleur
}

export interface VueFrise {
  son: Son
  cadre: Cadre
  selId: number | null
  /** Instant de lecture, ou null quand le robot ne joue rien. */
  tete: number | null
  /** Trait en cours de dessin au crayon. */
  trait: { x: number; y: number }[] | null
  timbreCrayon: Timbre
}

export function dessinerFrise(ctx: CanvasRenderingContext2D, vue: VueFrise) {
  const { son, cadre, selId, tete, trait } = vue
  const R = son.reglages
  const { w, h } = cadre
  const texte = jeton('--texte')
  const texteFaible = jeton('--texte-faible')
  const bordure = jeton('--bordure')

  ctx.clearRect(0, 0, w, h)

  const fond = ctx.createLinearGradient(0, REGLE, 0, h)
  fond.addColorStop(0, 'rgba(91,214,226,.07)')
  fond.addColorStop(1, 'rgba(244,167,44,.05)')
  ctx.fillStyle = fond
  ctx.fillRect(0, REGLE, w, h - REGLE)

  ctx.font = '700 10px Nunito, sans-serif'
  ;[125, 250, 500, 1000, 2000].forEach((f) => {
    const y = yDe(f, cadre)
    ctx.strokeStyle = 'rgba(255,255,255,.05)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(MARGE_G, y)
    ctx.lineTo(w, y)
    ctx.stroke()
  })
  ctx.fillStyle = texteFaible
  ctx.textAlign = 'center'
  ctx.fillText('aigu', MARGE_G / 2, REGLE + 20)
  ctx.fillText('▲', MARGE_G / 2, REGLE + 34)
  ctx.fillText('▼', MARGE_G / 2, h - 22)
  ctx.fillText('grave', MARGE_G / 2, h - 8)

  // Règle en secondes, comme celle de l'Atelier.
  ctx.fillStyle = jeton('--carter')
  ctx.fillRect(0, 0, w, REGLE)
  ctx.strokeStyle = bordure
  ctx.beginPath()
  ctx.moveTo(0, REGLE + 0.5)
  ctx.lineTo(w, REGLE + 0.5)
  ctx.stroke()
  const pas = cadre.fenetre > 4 ? 0.5 : 0.1
  for (let s = 0; s <= cadre.fenetre + 1e-6; s += pas) {
    const x = Math.round(xDe(s, cadre)) + 0.5
    const grand = Math.abs(s * 2 - Math.round(s * 2)) < 1e-6
    ctx.strokeStyle = grand ? texteFaible : bordure
    ctx.beginPath()
    ctx.moveTo(x, REGLE - (grand ? 9 : 5))
    ctx.lineTo(x, REGLE)
    ctx.stroke()
    if (grand) {
      ctx.fillStyle = texteFaible
      ctx.textAlign = 'left'
      ctx.fillText(`${s.toFixed(1).replace('.', ',')} s`, x + 3, 12)
      ctx.strokeStyle = 'rgba(255,255,255,.035)'
      ctx.beginPath()
      ctx.moveTo(x, REGLE)
      ctx.lineTo(x, h)
      ctx.stroke()
    }
  }

  son.morceaux.forEach((m) => dessinerMorceau(ctx, m, cadre, R, m.id === selId, texte, texteFaible))

  if (trait && trait.length > 1) {
    ctx.strokeStyle = couleurTimbre(vue.timbreCrayon)
    ctx.lineWidth = 4
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.setLineDash([2, 7])
    ctx.beginPath()
    trait.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)))
    ctx.stroke()
    ctx.setLineDash([])
  }

  if (tete != null) {
    const x = xDe(tete, cadre)
    ctx.strokeStyle = jeton('--accent')
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, h)
    ctx.stroke()
  }
}

function dessinerMorceau(
  ctx: CanvasRenderingContext2D,
  m: Morceau,
  cadre: Cadre,
  R: Reglages,
  choisi: boolean,
  texte: string,
  texteFaible: string,
) {
  const points = traceRuban(m, cadre, R)
  const couleur = couleurTimbre(m.timbre)

  // La bande du roulement, sous le ruban : jusqu'où la note roule.
  if (points[0].yb) {
    ctx.beginPath()
    points.forEach((p, i) => (i ? ctx.lineTo(p.x, p.yb) : ctx.moveTo(p.x, p.yb)))
    for (let i = points.length - 1; i >= 0; i--) ctx.lineTo(points[i].x, points[i].yh)
    ctx.closePath()
    ctx.fillStyle = hachures(ctx, couleur)
    ctx.globalAlpha = choisi ? 0.9 : 0.6
    ctx.fill()
    ctx.globalAlpha = choisi ? 0.6 : 0.35
    ctx.strokeStyle = couleur
    ctx.lineWidth = 1
    ctx.stroke()
    ctx.globalAlpha = 1
  }

  ctx.save()
  if (choisi) {
    ctx.shadowColor = couleur
    ctx.shadowBlur = 16
  }
  ctx.beginPath()
  points.forEach((p, i) => (i ? ctx.lineTo(p.ux, p.uy) : ctx.moveTo(p.ux, p.uy)))
  for (let i = points.length - 1; i >= 0; i--) ctx.lineTo(points[i].lx, points[i].ly)
  ctx.closePath()
  ctx.globalAlpha = choisi ? 0.42 : 0.28
  ctx.fillStyle = couleur
  ctx.fill()
  ctx.restore()

  ctx.beginPath()
  points.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)))
  ctx.strokeStyle = couleur
  ctx.lineWidth = choisi ? 3 : 2
  ctx.lineJoin = 'round'
  ctx.stroke()

  const debut = points[0]
  const fin = points[points.length - 1]

  if (m.attaque) {
    ctx.strokeStyle = texte
    ctx.lineWidth = 1.5
    ctx.globalAlpha = 0.8
    const x = debut.x - 9
    const y = debut.y
    if (m.attaque === 'click') {
      ;[
        [0, -5, 0, 5],
        [-4, -3, 4, 3],
        [-4, 3, 4, -3],
      ].forEach(([a, b, c, d]) => {
        ctx.beginPath()
        ctx.moveTo(x + a, y + b)
        ctx.lineTo(x + c, y + d)
        ctx.stroke()
      })
    } else {
      ;[-4, 0, 4].forEach((dy) => {
        ctx.beginPath()
        ctx.moveTo(x - 5, y + dy)
        ctx.quadraticCurveTo(x - 2, y + dy - 2, x + 1, y + dy)
        ctx.quadraticCurveTo(x + 3, y + dy + 2, x + 5, y + dy)
        ctx.stroke()
      })
    }
    ctx.globalAlpha = 1
  }

  if (m.timbre === 'voix') {
    ctx.fillStyle = choisi ? texte : texteFaible
    ctx.font = '800 11px Nunito, sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(m.v1, debut.x + 4, debut.y + debut.th + 16)
    if (m.v2 !== m.v1 || m.duree > 0.2) ctx.fillText(m.v2, fin.x - 4, fin.y + 16)
  }

  if (!choisi) return

  // Les poignées : le rond tient la hauteur, la pastille au-dessus tient le volume.
  m.courbe.forEach((_, i) => {
    const x = xDuPoint(m, i, cadre, R)
    const y = yDuPoint(m, i, cadre, R)
    const hv = yPastille(m, i, y, R)

    ctx.strokeStyle = couleur
    ctx.lineWidth = 1.5
    ctx.setLineDash([2, 3])
    ctx.beginPath()
    ctx.moveTo(x, y - 7)
    ctx.lineTo(x, hv + 6)
    ctx.stroke()
    ctx.setLineDash([])

    ctx.beginPath()
    ctx.moveTo(x, hv - 6)
    ctx.lineTo(x + 6, hv)
    ctx.lineTo(x, hv + 6)
    ctx.lineTo(x - 6, hv)
    ctx.closePath()
    ctx.fillStyle = couleur
    ctx.fill()
    ctx.lineWidth = 2
    ctx.strokeStyle = texte
    ctx.stroke()

    ctx.beginPath()
    ctx.arc(x, y, 7, 0, Math.PI * 2)
    ctx.fillStyle = texte
    ctx.fill()
    ctx.lineWidth = 3
    ctx.strokeStyle = couleur
    ctx.stroke()
  })

  // Les bords, qu'on tire pour allonger ou raccourcir.
  ;[debut, fin].forEach((p) => {
    ctx.fillStyle = couleur
    ctx.beginPath()
    ctx.roundRect(p.x - 3.5, p.y - 16, 7, 32, 3.5)
    ctx.fill()
  })
}

/** L'onde du son rendu, colorée par morceau : ce que le robot jouera, tel quel. */
export function dessinerOnde(
  ctx: CanvasRenderingContext2D,
  cadre: Cadre,
  son: Son,
  rendu: AudioBuffer | null,
  perime: boolean,
  tete: number | null,
) {
  const { w, h } = cadre
  ctx.clearRect(0, 0, w, h)
  ctx.strokeStyle = jeton('--bordure')
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(MARGE_G, h / 2)
  ctx.lineTo(w, h / 2)
  ctx.stroke()

  if (rendu) {
    const echantillons = rendu.getChannelData(0)
    const moitie = h / 2 - 4
    ctx.globalAlpha = perime ? 0.35 : 1
    for (let x = MARGE_G; x < w - MARGE_D; x++) {
      const t0 = tDe(x, cadre)
      const t1 = tDe(x + 1, cadre)
      const i0 = Math.floor(t0 * rendu.sampleRate)
      const i1 = Math.min(echantillons.length, Math.ceil(t1 * rendu.sampleRate))
      if (i0 >= echantillons.length) break
      let bas = 0
      let haut = 0
      for (let i = i0; i < i1; i++) {
        if (echantillons[i] < bas) bas = echantillons[i]
        if (echantillons[i] > haut) haut = echantillons[i]
      }
      const m = morceauA(son.morceaux, (t0 + t1) / 2, son.reglages)
      ctx.fillStyle = m ? couleurTimbre(m.timbre) : jeton('--texte-faible')
      ctx.fillRect(x, h / 2 - haut * moitie * 1.25, 1, Math.max(1, (haut - bas) * moitie * 1.25))
    }
    ctx.globalAlpha = 1
  }

  if (tete != null) {
    const x = xDe(tete, cadre)
    ctx.strokeStyle = jeton('--accent')
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(x, 0)
    ctx.lineTo(x, h)
    ctx.stroke()
  }
}
