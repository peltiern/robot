import { baseHz, gainDe, type Morceau, type Reglages, type Timbre, valeurA, type Voyelle, volumes } from './types'

/**
 * La synthèse, échantillon par échantillon, dans le navigateur.
 *
 * <b>Le robot ne fabrique pas les sons.</b> Le Studio doit s'ouvrir robot éteint, donc c'est ici
 * qu'un son existe ; le WAV rendu part ensuite au robot, qui le rejoue par {@code play}. Écouter
 * dans l'éditeur et entendre sur la tête, c'est le même fichier — à l'échantillon près.
 *
 * Le même code sert à écouter et à fabriquer le fichier : {@link rendre} calcule tout dans un
 * {@link OfflineAudioContext}, et ce tampon est à la fois ce qu'on joue et ce qu'on enregistre.
 */

const SR = 44100

/** Formants des voyelles, remontés pour une « petite » voix. */
const FORMANTS: Record<Voyelle, [number, number]> = {
  a: [730, 1090],
  e: [530, 1850],
  i: [300, 2300],
  o: [450, 800],
  u: [330, 700],
  y: [300, 1750],
}

/**
 * Le volume de chaque point, relié d'un point à l'autre comme la hauteur : par-dessus l'attaque et
 * l'extinction, c'est lui qui fait enfler ou retomber un morceau.
 *
 * Rampes exponentielles : l'oreille entend des rapports, et un fondu dessiné en ligne droite finit
 * sec. Plancher à −62 dB, qui est le silence à l'oreille mais pas zéro — une rampe exponentielle ne
 * peut pas viser zéro.
 */
function gainVolume(ctx: BaseAudioContext, sortie: AudioNode, niveaux: number[], t: number, duree: number, note: boolean) {
  const g = ctx.createGain()
  const n = niveaux.length
  const plancher = (v: number) => Math.max(v, 0.0008)
  g.gain.setValueAtTime(plancher(niveaux[0]), t)
  niveaux.forEach((v, i) => {
    if (!i) return
    if (note) {
      const ts = t + (duree * i) / n
      g.gain.setValueAtTime(plancher(niveaux[i - 1]), ts)
      g.gain.exponentialRampToValueAtTime(plancher(v), ts + 0.006)
    } else {
      g.gain.exponentialRampToValueAtTime(plancher(v), t + (duree * i) / (n - 1))
    }
  })
  g.connect(sortie)
  return g
}

/** Une syllabe : une onde riche filtrée par deux formants, ce qui la fait entendre comme une voix. */
function syllabe(
  ctx: BaseAudioContext,
  sortie: AudioNode,
  t: number,
  duree: number,
  courbe: number[],
  v1: Voyelle,
  v2: Voyelle,
  vib: [number, number],
  R: Reglages,
  niveaux: number[],
) {
  const base = 480 * R.hauteur
  const dent = ctx.createOscillator()
  dent.type = 'sawtooth'
  const sinus = ctx.createOscillator()
  sinus.type = 'sine'
  ;[dent, sinus].forEach((o) => {
    o.frequency.setValueAtTime(base * courbe[0], t)
    courbe.forEach((r, i) => {
      if (i) o.frequency.linearRampToValueAtTime(base * r, t + (duree * i) / (courbe.length - 1))
    })
  })

  const [vitesse, profondeur] = vib
  const lfo = ctx.createOscillator()
  lfo.frequency.value = vitesse
  const lfoGain = ctx.createGain()
  lfoGain.gain.value = base * profondeur * R.tremblement
  lfo.connect(lfoGain)
  lfoGain.connect(dent.frequency)
  lfoGain.connect(sinus.frequency)

  const echelle = 1.35 * Math.sqrt(R.hauteur)
  const formants = ctx.createGain()
  formants.gain.value = 2.8 * R.voix
  ;[0, 1].forEach((k) => {
    const filtre = ctx.createBiquadFilter()
    filtre.type = 'bandpass'
    filtre.Q.value = 7
    filtre.frequency.setValueAtTime(FORMANTS[v1][k] * echelle, t)
    filtre.frequency.linearRampToValueAtTime(FORMANTS[v2][k] * echelle, t + duree)
    const dose = ctx.createGain()
    dose.gain.value = k ? 0.55 : 1
    dent.connect(filtre)
    filtre.connect(dose)
    dose.connect(formants)
  })
  const fond = ctx.createGain()
  fond.gain.value = 0.45 * (1 - R.voix * 0.7)
  sinus.connect(fond)

  // Extinction courte, juste de quoi éviter le clic : elle valait le dernier cinquième de la durée,
  // si bien que le volume du dernier point tombait dans un fondu déjà à −78 dB et ne s'entendait
  // pas. C'est au volume des points de dessiner la fin d'un morceau, pas à l'enveloppe.
  const attaque = Math.min(0.018, duree * 0.3)
  const relache = Math.min(0.03, duree * 0.4)
  const env = ctx.createGain()
  env.gain.setValueAtTime(0.0001, t)
  env.gain.exponentialRampToValueAtTime(1, t + attaque)
  env.gain.setValueAtTime(1, Math.max(t + attaque, t + duree - relache))
  env.gain.exponentialRampToValueAtTime(0.0001, t + duree)

  formants.connect(env)
  fond.connect(env)
  env.connect(gainVolume(ctx, sortie, niveaux, t, duree, false))
  ;[dent, sinus, lfo].forEach((o) => {
    o.start(t)
    o.stop(t + duree + 0.05)
  })
}

/** Un bip électronique : glissé, sauts de notes, trille, vibration, ou bourdon grinçant. */
function bip(
  ctx: BaseAudioContext,
  sortie: AudioNode,
  t: number,
  duree: number,
  courbe: number[],
  sorte: Timbre,
  R: Reglages,
  niveaux: number[],
) {
  const base = 900 * R.hauteur
  const attaque = Math.min(0.01, duree * 0.3)
  const relache = Math.min(0.03, duree * 0.4)
  const crete = 0.6
  const env = ctx.createGain()
  env.gain.setValueAtTime(0, t)
  env.gain.linearRampToValueAtTime(crete, t + attaque)
  env.gain.setValueAtTime(crete, Math.max(t + attaque, t + duree - relache))
  env.gain.linearRampToValueAtTime(0, t + duree)
  env.connect(gainVolume(ctx, sortie, niveaux, t, duree, sorte === 'note'))

  const oscillateurs: OscillatorNode[] = []
  const suivre = (param: AudioParam, mult: number) => {
    param.setValueAtTime(base * mult * courbe[0], t)
    courbe.forEach((r, i) => {
      if (!i) return
      if (sorte === 'note') {
        const ts = t + (duree * i) / courbe.length
        param.setValueAtTime(base * mult * courbe[i - 1], ts)
        param.linearRampToValueAtTime(base * mult * r, ts + 0.006)
      } else {
        param.linearRampToValueAtTime(base * mult * r, t + (duree * i) / (courbe.length - 1))
      }
    })
  }

  if (sorte === 'blat') {
    // Grognement : deux oscillateurs graves légèrement désaccordés, filtrés, avec un roulement doux.
    const o1 = ctx.createOscillator()
    o1.type = 'sawtooth'
    suivre(o1.frequency, 0.28)
    const o2 = ctx.createOscillator()
    o2.type = 'triangle'
    suivre(o2.frequency, 0.283)
    const passeBas = ctx.createBiquadFilter()
    passeBas.type = 'lowpass'
    passeBas.frequency.value = 1100
    passeBas.Q.value = 3
    const tremolo = ctx.createGain()
    tremolo.gain.value = 0.75
    const lfo = ctx.createOscillator()
    lfo.frequency.value = 11
    const dose = ctx.createGain()
    dose.gain.value = 0.22 * Math.max(R.tremblement, 0.3)
    lfo.connect(dose)
    dose.connect(tremolo.gain)
    const niveau = ctx.createGain()
    niveau.gain.value = 0.18
    o1.connect(passeBas)
    o2.connect(passeBas)
    passeBas.connect(tremolo)
    tremolo.connect(niveau)
    niveau.connect(env)
    oscillateurs.push(o1, o2, lfo)
  } else {
    const sinus = ctx.createOscillator()
    sinus.type = 'sine'
    const carre = ctx.createOscillator()
    carre.type = 'square'
    suivre(sinus.frequency, 1)
    suivre(carre.frequency, 1)
    const passeBas = ctx.createBiquadFilter()
    passeBas.type = 'lowpass'
    passeBas.frequency.value = 2000
    const doseSinus = ctx.createGain()
    doseSinus.gain.value = 0.5 * (1 - R.voix * 0.6)
    const doseCarre = ctx.createGain()
    doseCarre.gain.value = 0.22 * R.voix
    sinus.connect(doseSinus)
    carre.connect(passeBas)
    passeBas.connect(doseCarre)
    doseSinus.connect(env)
    doseCarre.connect(env)
    oscillateurs.push(sinus, carre)

    if (sorte === 'trill' || sorte === 'warble') {
      const lfo = ctx.createOscillator()
      lfo.type = sorte === 'trill' ? 'square' : 'sine'
      lfo.frequency.value = sorte === 'trill' ? 22 : 38
      const dose = ctx.createGain()
      dose.gain.value = base * (sorte === 'trill' ? 0.22 : 0.12) * Math.max(R.tremblement, 0.2)
      lfo.connect(dose)
      dose.connect(sinus.frequency)
      dose.connect(carre.frequency)
      oscillateurs.push(lfo)
    }
  }

  oscillateurs.forEach((o) => {
    o.start(t)
    o.stop(t + duree + 0.06)
  })
}

/** Clic ou souffle d'attaque. Graine fixe : un même son doit rendre le même WAV à chaque fois. */
function bruit(ctx: BaseAudioContext, sortie: AudioNode, t: number, duree: number, frequence: number) {
  const tampon = ctx.createBuffer(1, Math.ceil(ctx.sampleRate * 0.2), ctx.sampleRate)
  const echantillons = tampon.getChannelData(0)
  let x = 12345
  for (let i = 0; i < echantillons.length; i++) {
    x = (x * 1103515245 + 12345) & 0x7fffffff
    echantillons[i] = x / 0x3fffffff - 1
  }
  const source = ctx.createBufferSource()
  source.buffer = tampon
  const filtre = ctx.createBiquadFilter()
  filtre.type = 'bandpass'
  filtre.frequency.value = frequence
  filtre.Q.value = 1.5
  const env = ctx.createGain()
  env.gain.setValueAtTime(0.0001, t)
  env.gain.exponentialRampToValueAtTime(0.4, t + 0.006)
  env.gain.exponentialRampToValueAtTime(0.0001, t + duree)
  source.connect(filtre)
  filtre.connect(env)
  env.connect(sortie)
  source.start(t)
  source.stop(t + duree + 0.02)
}

/**
 * Combien le bruit d'attaque d'un morceau le précède, en secondes entendues : le clic et le
 * souffle se posent juste avant le morceau, pour qu'il démarre dessus.
 */
export const avanceAttaque = (m: Morceau): number => (m.attaque === 'click' ? 0.012 : m.attaque === 'hiss' ? 0.035 : 0)

/**
 * Durée du son rendu, en secondes.
 *
 * Le fichier s'arrête juste après le dernier morceau dessiné. Il durait au moins 0,15 s, plus
 * 0,1 s après le dernier morceau : du silence que personne n'avait dessiné, et qui se serait
 * entendu comme un retard dans une animation. Restent 20 ms, parce que le compresseur et le
 * limiteur regardent chacun 6 ms en avance et décalent d'autant la sortie : sans cette marge, la
 * fin du son serait coupée.
 */
export function dureeDe(morceaux: Morceau[], R: Reglages): number {
  return Math.max(0, ...morceaux.map((m) => (m.debut + m.duree) / R.debit)) + 0.02
}

/**
 * Calcule le son en entier, sans le jouer. Le tampon rendu est à la fois ce qu'on écoute et ce
 * qu'on enregistre : il n'y a jamais deux versions d'un même son.
 */
export function rendre(morceaux: Morceau[], R: Reglages): Promise<AudioBuffer> {
  const fin = dureeDe(morceaux, R)
  const ctx = new OfflineAudioContext(1, Math.ceil(SR * fin), SR)

  // La présence resserre l'écart entre les passages forts et faibles, puis rattrape ce qui a été
  // rabattu : le son gagne en densité sans monter en crête, c'est ce qui fait qu'il « porte ».
  // Compression lente à dessein : vive, elle suivait le relief à l'intérieur d'un morceau et
  // reprenait aussitôt le volume qu'on montait sur un point.
  const compresseur = ctx.createDynamicsCompressor()
  compresseur.threshold.value = -6 - 18 * R.presence
  compresseur.knee.value = 10
  compresseur.ratio.value = 3 + 5 * R.presence
  compresseur.attack.value = 0.04
  compresseur.release.value = 0.35

  const rattrapage = ctx.createGain()
  rattrapage.gain.value = Math.pow(10, (R.presence * 14) / 20)

  // Limiteur : dernier garde-fou, il tient la crête sous le plafond quoi que la présence rattrape.
  // Seuil et marge choisis à la mesure — à −1 dB sans marge, son attaque laissait passer assez de
  // crêtes pour écrêter 171 échantillons sur une présence à fond.
  const limiteur = ctx.createDynamicsCompressor()
  limiteur.threshold.value = -3
  limiteur.knee.value = 0
  limiteur.ratio.value = 20
  limiteur.attack.value = 0.001
  limiteur.release.value = 0.05
  const marge = ctx.createGain()
  marge.gain.value = 0.9

  // Le volume passe APRÈS toute la chaîne : placé avant, il était aussitôt rabaissé par la
  // compression, et son curseur ne changeait rien à l'oreille — 0,15 et 1,00 rendaient la même
  // crête.
  const volume = ctx.createGain()
  volume.gain.value = gainDe(R.volume)

  const melange = ctx.createGain()
  melange.connect(compresseur)
  compresseur.connect(rattrapage)
  rattrapage.connect(limiteur)
  limiteur.connect(marge)
  marge.connect(volume)
  volume.connect(ctx.destination)

  morceaux.forEach((m) => {
    const t = m.debut / R.debit
    const duree = m.duree / R.debit
    if (m.attaque === 'click') bruit(ctx, melange, Math.max(0, t - avanceAttaque(m)), 0.018, 1400)
    else if (m.attaque === 'hiss') bruit(ctx, melange, Math.max(0, t - avanceAttaque(m)), 0.05, 4500)
    if (m.timbre === 'voix') syllabe(ctx, melange, t, duree, m.courbe, m.v1, m.v2, m.vib, R, volumes(m))
    else bip(ctx, melange, t, duree, m.courbe, m.timbre, R, volumes(m))
  })

  return ctx.startRendering()
}

/** La fréquence entendue au milieu du ruban, hors roulement — ce que la frise dessine comme ligne. */
export const frequenceA = (m: Morceau, u: number, R: Reglages): number =>
  baseHz(m.timbre) * R.hauteur * valeurA(m.timbre, m.courbe, u)
