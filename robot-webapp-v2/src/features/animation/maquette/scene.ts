import * as THREE from 'three'
import { MECA, oeilDepuisServo, servoDepuisOeil } from './tringlerie'

/*
 * La scène de robot-core/3d/yeux-walle.html, portée telle quelle : toutes les cotes viennent de
 * « Wall-E Eye.stl », redressé de 0,2666° et moyenné entre les deux yeux. Seuls ont changé ce
 * qu'imposait l'éditeur — la pose vient de la timeline et non de curseurs — et ce qu'imposait
 * three.js depuis la r128 (voir les lumières).
 */

/** Contour extérieur d'une coque, relevé au 1/10 de mm (86 points), x vers l'extérieur. */
const CONTOUR = [
  152.3, -6.0, 151.7, -3.8, 149.9, 0.6, 148.7, 4.2, 142.7, 16.8, 138.1, 24.7,
  133.5, 30.1, 132.3, 30.9, 131.0, 32.4, 124.7, 36.9, 119.7, 39.3, 117.7, 40.0,
  117.3, 40.5, 109.5, 42.6, 105.5, 43.2, 102.1, 43.2, 101.6, 43.5, 10.7, 43.5,
  10.0, 43.2, 4.8, 39.8, 4.8, 9.1, 6.6, 0.7, 9.3, -7.0, 9.2, -7.5,
  12.6, -15.4, 17.0, -23.7, 19.6, -27.5, 20.4, -29.1, 22.6, -31.9, 23.0, -32.7,
  23.4, -33.0, 24.0, -34.1, 24.4, -34.4, 26.0, -36.5, 27.0, -37.3, 27.4, -38.1,
  31.8, -42.3, 33.8, -44.7, 34.6, -45.1, 34.8, -45.5, 40.0, -49.6, 40.2, -50.1,
  48.6, -55.7, 48.9, -56.1, 55.3, -59.4, 55.7, -59.9, 60.7, -62.0, 62.2, -62.8,
  62.7, -62.9, 63.6, -63.5, 68.7, -65.3, 69.3, -65.2, 73.7, -66.9, 75.3, -67.1,
  80.0, -68.3, 85.6, -69.3, 94.2, -70.1, 107.3, -70.1, 109.4, -69.7, 111.1, -69.7,
  114.8, -68.9, 123.0, -66.5, 124.5, -65.9, 125.7, -65.0, 128.8, -63.9, 130.7, -62.6,
  132.9, -61.5, 133.4, -60.9, 136.1, -59.1, 139.2, -56.6, 144.9, -50.9, 146.1, -48.8,
  147.5, -46.9, 149.7, -42.6, 151.1, -38.8, 151.5, -38.1, 151.9, -36.6, 152.3, -35.9,
  153.5, -31.2, 154.3, -25.7, 154.6, -19.3, 154.3, -18.5, 154.4, -15.4, 153.9, -13.8,
  153.5, -10.6, 152.5, -6.8,
]

const COQUE = { AVANT: -125.30, ARRIERE: 0.0 }
const LENTILLE = { X: 65.645, Y: -6.033, OUVERTURE: 46.98 }

/** Méridiens du bloc optique, en [rayon, z]. */
const CERCLAGE: [number, number][] = [
  [46.98, -111.71], [41.589, -111.71], [41.589, -105.00], [34.949, -105.00], [34.949, -111.71],
  [35.00, -111.71], [34.90, -100.02], [34.20, -97.95], [33.80, -96.58], [33.40, -95.54],
  [33.00, -94.67], [32.60, -93.88], [32.20, -93.20], [31.80, -92.59], [31.00, -91.50],
  [30.20, -90.54], [29.40, -89.74], [28.60, -89.03], [27.80, -88.38], [27.00, -87.84],
  [26.20, -87.32], [25.40, -86.91], [24.60, -86.50], [23.80, -86.19], [23.00, -85.88],
  [22.20, -85.66], [21.40, -85.44], [20.60, -85.29], [19.80, -85.16], [19.00, -85.07],
  [18.20, -85.03],
]
const PUPILLE: [number, number][] = [
  [17.774, -85.03], [17.774, -89.00], [11.658, -89.00], [11.658, -93.00],
  [7.489, -93.00], [7.489, -93.05], [6.20, -93.12], [3.80, -93.30], [0, -93.40],
]

const RAD = Math.PI / 180

/**
 * Le monter / descendre est une translation dont on n'a pas relevé la cinématique : un
 * millimètre par degré de servo est une convention d'affichage, pas une mesure.
 */
const MM_PAR_DEGRE_MONTER = 1

// Assez près pour que la tête remplisse le bandeau, assez loin — et visée assez haut — pour qu'elle y
// reste aux extrêmes : relevée de 53° et montée de 60 mm, le haut des coques passe à 186 mm au-dessus
// de l'arbre ; baissée de 40°, leur bas descend à −134.
const VUE_DE_FACE = { rayon: 580, theta: 0, phi: Math.PI / 2 - 0.12 }
const POINT_VISE = new THREE.Vector3(0, 15, -55)

/** Ce que la maquette sait montrer, en degrés d'organe — le contrat des pistes. */
export interface Pose {
  oeilGauche: number
  oeilDroit: number
  panoramique: number
  inclinaison: number
  monterDescendre: number
}

export interface Maquette {
  element: HTMLCanvasElement
  poser: (pose: Pose) => void
  rendre: () => void
  dimensionner: (largeur: number, hauteur: number) => void
  liberer: () => void
}

export function creerMaquette(): Maquette {
  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x101318)
  const camera = new THREE.PerspectiveCamera(38, 1, 1, 4000)
  const cible = POINT_VISE.clone()
  let orbite = { ...VUE_DE_FACE }
  const renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.06

  // Intensités de la maquette d'origine multipliées par π, et lueur sans atténuation : depuis la
  // r155, three compte les lumières en unités physiques, et les mêmes chiffres donnaient une
  // scène presque éteinte.
  scene.add(new THREE.HemisphereLight(0xc6ddf0, 0x1b1f26, 0.55 * Math.PI))
  const cle = new THREE.DirectionalLight(0xffffff, 0.95 * Math.PI)
  cle.position.set(200, 280, -360)
  const remplissage = new THREE.DirectionalLight(0xa8c4de, 0.32 * Math.PI)
  remplissage.position.set(-340, -90, -240)
  const contre = new THREE.DirectionalLight(0x9fc8ec, 0.45 * Math.PI)
  contre.position.set(40, 150, 380)
  const lueur = new THREE.PointLight(0xffffff, 0.5 * Math.PI, 1500, 0)
  lueur.position.set(70, 100, -320)
  scene.add(cle, remplissage, contre, lueur)

  const M = {
    coque:    new THREE.MeshStandardMaterial({ color: 0x8b8674, roughness: 0.55, metalness: 0.12 }),
    cerclage: new THREE.MeshStandardMaterial({ color: 0x9c957e, roughness: 0.4, metalness: 0.35, side: THREE.DoubleSide }),
    pupille:  new THREE.MeshStandardMaterial({ color: 0x1f52a8, roughness: 0.16, metalness: 0.5, side: THREE.DoubleSide }),
    bride:    new THREE.MeshStandardMaterial({ color: 0x8db6d2, roughness: 0.5, metalness: 0.15 }),
    acier:    new THREE.MeshStandardMaterial({ color: 0xb4bac2, roughness: 0.3, metalness: 0.85 }),
    servo:    new THREE.MeshStandardMaterial({ color: 0x2f333a, roughness: 0.6, metalness: 0.2 }),
    bras:     new THREE.MeshStandardMaterial({ color: 0xf5b324, roughness: 0.35, metalness: 0.4 }),
    bielle:   new THREE.MeshStandardMaterial({ color: 0xd8dde3, roughness: 0.35, metalness: 0.6 }),
  }

  // ── outils ────────────────────────────────────────────────────────────────

  function contour(s: number) {
    const forme = new THREE.Shape()
    for (let i = 0; i < CONTOUR.length; i += 2) {
      const x = s * CONTOUR[i], y = CONTOUR[i + 1]
      if (i === 0) forme.moveTo(x, y)
      else forme.lineTo(x, y)
    }
    forme.closePath()
    return forme
  }

  // Le sens du tracé s'inverse avec le miroir du contour : sans ça, le trou de la lentille
  // serait pris pour une seconde forme pleine sur l'un des deux yeux.
  function hublot(rayon: number, s: number) {
    const trou = new THREE.Path()
    trou.absarc(s * LENTILLE.X, LENTILLE.Y, rayon, 0, Math.PI * 2, s < 0)
    return trou
  }

  function revolution(profil: [number, number][], x: number, y: number, materiau: THREE.Material) {
    const geometrie = new THREE.LatheGeometry(profil.map(([r, z]) => new THREE.Vector2(r, -z)), 96)
    geometrie.rotateX(-Math.PI / 2)
    const maillage = new THREE.Mesh(geometrie, materiau)
    maillage.position.set(x, y, 0)
    return maillage
  }

  function barre(materiau: THREE.Material, epaisseur: number) {
    const geometrie = new THREE.CylinderGeometry(epaisseur, epaisseur, 1, 16)
    geometrie.translate(0, 0.5, 0)
    return new THREE.Mesh(geometrie, materiau)
  }

  function tendre(maillage: THREE.Object3D, a: THREE.Vector3, b: THREE.Vector3) {
    const v = b.clone().sub(a)
    maillage.position.copy(a)
    maillage.scale.set(1, v.length(), 1)
    maillage.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), v.normalize())
  }

  // ── une coque, avec son servo : il est vissé dedans et tourne avec elle ───

  function construireCoque(s: number) {
    const groupe = new THREE.Group()
    const forme = contour(s)
    forme.holes.push(hublot(LENTILLE.OUVERTURE, s))
    const coque = new THREE.Mesh(new THREE.ExtrudeGeometry(forme, {
      depth: COQUE.ARRIERE - COQUE.AVANT - 2, bevelEnabled: true,
      bevelThickness: 1, bevelSize: 1.2, bevelSegments: 2,
    }), M.coque)
    coque.position.z = COQUE.AVANT + 1
    groupe.add(coque)
    groupe.add(revolution(CERCLAGE, s * LENTILLE.X, LENTILLE.Y, M.cerclage))
    groupe.add(revolution(PUPILLE, s * LENTILLE.X, LENTILLE.Y, M.pupille))

    const sx = s * MECA.S[0], sy = MECA.S[1]
    const corps = new THREE.Mesh(new THREE.BoxGeometry(54.5, 20.6, 39.9), M.servo)
    corps.position.set(sx - s * 9.74, sy + 0.53, MECA.Z_SERVO - 21.95)
    groupe.add(corps)
    const sortie = new THREE.Mesh(new THREE.CylinderGeometry(6.4, 6.4, 8, 24), M.acier)
    sortie.rotation.x = Math.PI / 2
    sortie.position.set(sx, sy, MECA.Z_SERVO - 4)
    groupe.add(sortie)

    const bras = new THREE.Group()
    const lame = new THREE.Mesh(new THREE.BoxGeometry(MECA.BRAS + 12, 9, 4.5), M.bras)
    lame.position.x = s * (MECA.BRAS / 2 - 3)
    bras.add(lame)
    const moyeu = new THREE.Mesh(new THREE.CylinderGeometry(6.5, 6.5, 6, 20), M.bras)
    moyeu.rotation.x = Math.PI / 2
    bras.add(moyeu)
    const rotule = new THREE.Mesh(new THREE.CylinderGeometry(4.7, 4.7, 8, 20), M.acier)
    rotule.rotation.x = Math.PI / 2
    rotule.position.set(s * MECA.BRAS, 0, MECA.Z_BIELLE - MECA.Z_SERVO)
    bras.add(rotule)
    bras.position.set(sx, sy, MECA.Z_SERVO)
    groupe.add(bras)

    return { groupe, bras, rotule }
  }

  // ── assemblage ────────────────────────────────────────────────────────────

  const tete = new THREE.Group()
  scene.add(tete)
  const pivotG = new THREE.Group(), pivotD = new THREE.Group()
  const coqueG = construireCoque(-1), coqueD = construireCoque(1)
  pivotG.add(coqueG.groupe)
  pivotD.add(coqueD.groupe)
  tete.add(pivotG, pivotD)

  // la platine fixe : elle porte l'arbre central et les deux ancrages de bielle
  const bride = new THREE.Group()
  const plaque = new THREE.Mesh(new THREE.BoxGeometry(44, 53, 60), M.bride)
  plaque.position.set(0, -15.5, 73)
  const moyeuFixe = new THREE.Mesh(new THREE.CylinderGeometry(16, 16, 60, 40), M.bride)
  moyeuFixe.rotation.x = Math.PI / 2
  moyeuFixe.position.set(0, 0, 73)
  const arbre = new THREE.Mesh(new THREE.CylinderGeometry(4, 4, 100, 28), M.acier)
  arbre.rotation.x = Math.PI / 2
  arbre.position.set(0, 0, 92)
  bride.add(plaque, moyeuFixe, arbre)
  for (const s of [-1, 1]) {
    const axe = new THREE.Mesh(new THREE.CylinderGeometry(2, 2, 40, 16), M.acier)
    axe.rotation.x = Math.PI / 2
    axe.position.set(s * MECA.D[0], MECA.D[1], MECA.Z_ANCRAGE - 15)
    bride.add(axe)
  }
  tete.add(bride)

  // les bielles joignent un point de la coque, mobile, à un point du bâti, fixe : c'est là que
  // se joue toute la non-linéarité, et elles ne se tracent qu'une fois les matrices à jour
  const bielleG = barre(M.bielle, 3.2), bielleD = barre(M.bielle, 3.2)
  tete.add(bielleG, bielleD)
  const ancrageG = new THREE.Vector3(-MECA.D[0], MECA.D[1], MECA.Z_ANCRAGE)
  const ancrageD = new THREE.Vector3(MECA.D[0], MECA.D[1], MECA.Z_ANCRAGE)
  const rotuleMonde = new THREE.Vector3()

  // l'angle de la coque sort du quadrilatère ; le bras, lui, obéit au servo
  function orienter(s: number, pivot: THREE.Group, bras: THREE.Group, servo: number) {
    pivot.rotation.z = s * oeilDepuisServo(servo) * RAD
    bras.rotation.z = s * (MECA.PSI0 + servo) * RAD
  }

  function tirerBielle(rotule: THREE.Object3D, bielle: THREE.Object3D, ancrage: THREE.Vector3) {
    rotule.getWorldPosition(rotuleMonde)
    tete.worldToLocal(rotuleMonde)
    tendre(bielle, rotuleMonde, ancrage)
  }

  // ── navigation : glisser pour tourner, molette ou pincement pour zoomer ───

  function placerCamera() {
    const phi = Math.max(0.25, Math.min(Math.PI - 0.25, orbite.phi))
    camera.position.set(
      cible.x + orbite.rayon * Math.sin(phi) * Math.sin(orbite.theta),
      cible.y + orbite.rayon * Math.cos(phi),
      cible.z - orbite.rayon * Math.sin(phi) * Math.cos(orbite.theta))
    camera.lookAt(cible)
  }

  const canevas = renderer.domElement
  let glisse = false, dernier = { x: 0, y: 0 }, pincement: number | null = null
  const zoomer = (facteur: number) => {
    orbite.rayon = Math.max(220, Math.min(1600, orbite.rayon * facteur))
    placerCamera()
  }
  const surAppui = (e: PointerEvent) => {
    glisse = true
    dernier = { x: e.clientX, y: e.clientY }
    canevas.setPointerCapture(e.pointerId)
  }
  const surDeplacement = (e: PointerEvent) => {
    if (!glisse || pincement !== null) return
    orbite.theta += (e.clientX - dernier.x) * 0.006
    orbite.phi -= (e.clientY - dernier.y) * 0.006
    dernier = { x: e.clientX, y: e.clientY }
    placerCamera()
  }
  const surRelache = () => { glisse = false }
  const surMolette = (e: WheelEvent) => {
    e.preventDefault()
    zoomer(1 + Math.sign(e.deltaY) * 0.09)
  }
  const surDoubleClic = () => {
    orbite = { ...VUE_DE_FACE }
    placerCamera()
  }
  const surToucher = (e: TouchEvent) => {
    if (e.touches.length !== 2) return
    e.preventDefault()
    const d = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY)
    if (pincement !== null) zoomer(pincement / d)
    pincement = d
  }
  const surFinToucher = () => { pincement = null }

  canevas.addEventListener('pointerdown', surAppui)
  canevas.addEventListener('pointermove', surDeplacement)
  for (const type of ['pointerup', 'pointercancel', 'pointerleave'] as const) {
    canevas.addEventListener(type, surRelache)
  }
  canevas.addEventListener('wheel', surMolette, { passive: false })
  canevas.addEventListener('dblclick', surDoubleClic)
  canevas.addEventListener('touchmove', surToucher, { passive: false })
  canevas.addEventListener('touchend', surFinToucher)
  placerCamera()

  return {
    element: canevas,

    poser(pose) {
      // Lacet négatif : dans le repère de three, une rotation positive autour de y tourne la
      // face vers la gauche du robot, et le panoramique positif est sa droite.
      tete.rotation.y = -pose.panoramique * RAD
      tete.rotation.x = pose.inclinaison * RAD
      tete.position.y = pose.monterDescendre * MM_PAR_DEGRE_MONTER
      orienter(-1, pivotG, coqueG.bras, servoDepuisOeil(pose.oeilGauche))
      orienter(1, pivotD, coqueD.bras, servoDepuisOeil(pose.oeilDroit))
      tete.updateMatrixWorld(true)
      tirerBielle(coqueG.rotule, bielleG, ancrageG)
      tirerBielle(coqueD.rotule, bielleD, ancrageD)
    },

    rendre() {
      renderer.render(scene, camera)
    },

    dimensionner(largeur, hauteur) {
      if (largeur === 0 || hauteur === 0) return
      camera.aspect = largeur / hauteur
      camera.updateProjectionMatrix()
      renderer.setSize(largeur, hauteur)
    },

    liberer() {
      canevas.removeEventListener('pointerdown', surAppui)
      canevas.removeEventListener('pointermove', surDeplacement)
      for (const type of ['pointerup', 'pointercancel', 'pointerleave'] as const) {
        canevas.removeEventListener(type, surRelache)
      }
      canevas.removeEventListener('wheel', surMolette)
      canevas.removeEventListener('dblclick', surDoubleClic)
      canevas.removeEventListener('touchmove', surToucher)
      canevas.removeEventListener('touchend', surFinToucher)
      scene.traverse(objet => {
        if (objet instanceof THREE.Mesh) objet.geometry.dispose()
      })
      Object.values(M).forEach(materiau => materiau.dispose())
      renderer.dispose()
    },
  }
}
