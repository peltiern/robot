import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { oeilDepuisServo, servoDepuisOeil } from './tringlerie'
import urlModele from './robot-walle.glb?url'

/*
 * Le robot entier, relevé dans « Wall-E GoBilda Full.stl » par robot-core/3d/mesures/releve_robot.py :
 * chaque corps rigide décimé et ramené au neutre, dans un GLB hiérarchisé dont la scène porte en
 * extras les pivots et les cotes des deux quadrilatères (voir MESURES-ROBOT.md). Ce fichier ne fait
 * qu'animer les nœuds : aucune cote n'y est recopiée.
 *
 * La maquette précédente faisait pivoter la tête autour de l'arbre des yeux. Les axes du panoramique
 * et de l'inclinaison sont 113,5 mm plus en arrière, et l'inclinaison 53,7 mm plus bas : 30° de
 * panoramique déplacent le visage de 119 mm, pas de 63. Un geste cadré sur l'ancienne maquette
 * sortait deux fois plus ample sur le robot.
 */

const RAD = Math.PI / 180

const TROIS_QUARTS = { theta: 0.5, phi: Math.PI / 2 - 0.15 }
/** Chaque vue est une sphère, prise sur le modèle, à faire tenir dans le panneau. */
export type Vue = 'entier' | 'visage'

const BARRES = ['barre_avant', 'barre_milieu', 'barre_arriere'] as const

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
  cadrer: (vue: Vue) => void
  liberer: () => void
}

type Point = [number, number, number]

/** Les extras de la scène du GLB, écrits par releve_robot.py dans le repère three, en mm. */
interface Cinematique {
  pivots: Record<string, Point>
  export: { monterDescendre: number }
  /** Roue de 80 dents fixée au panoramique, pignon de 40 porté par la tête : rapport 2. */
  pignon: { rapport: number }
  /**
   * Le vrai monter/descendre : servo → bras de 28 → tige de 87 → levier de 48 de la barre du milieu.
   * Le CAD n'a ni ce bras ni cette tige ; le calage vient de la hauteur mesurée au HUD sur le robot,
   * que la loi reproduit au millimètre.
   */
  monter: {
    O2: Point; B: Point; R0: Point; barre: number; theta0: number; levier: number; bras: number; tige: number
    degParUnite: number; calage: number; sens: number; branche: number; xBras: number; xLevier: number
  }
  oeil: Record<'droit' | 'gauche', { S: Point; P: Point; D: Point }>
}

/**
 * Le monter/descendre pour une valeur d'organe (l'unité du HUD). Plan de travail (u, v) = (z, y) de
 * three, u vers l'arrière ; angles en radians.
 */
function monter(m: Cinematique['monter'], valeur: number) {
  const O2 = [m.O2[2], m.O2[1]], B = [m.B[2], m.B[1]]
  const phi = (m.calage + m.sens * m.degParUnite * valeur) * RAD
  const M = [O2[0] + m.bras * Math.cos(phi), O2[1] + m.bras * Math.sin(phi)]
  const vx = M[0] - B[0], vy = M[1] - B[1], q = Math.hypot(vx, vy)
  const k = (q * q + m.levier * m.levier - m.tige * m.tige) / (2 * q * m.levier)
  const levier = Math.atan2(vy, vx) + m.branche * Math.acos(Math.max(-1, Math.min(1, k)))
  const R = [B[0] + m.levier * Math.cos(levier), B[1] + m.levier * Math.sin(levier)]
  return { barre: levier + Math.PI, phi, M, R }
}

function tendre(objet: THREE.Object3D, a: THREE.Vector3, b: THREE.Vector3) {
  const d = b.clone().sub(a)
  objet.position.copy(a)
  objet.scale.set(1, d.length(), 1)
  objet.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), d.normalize())
}

export function creerMaquette(vueInitiale: Vue = 'entier'): Maquette {
  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x101318)
  const camera = new THREE.PerspectiveCamera(38, 1, 5, 12000)
  const cible = new THREE.Vector3(0, 400, -70)
  let orbite = { ...TROIS_QUARTS, rayon: 1800 }
  let vue: Vue = vueInitiale
  // le robot entier, en attendant le modèle : une sphère large, corrigée dès qu'il est chargé
  const englobante = new THREE.Sphere(new THREE.Vector3(0, 400, -70), 520)
  const renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.06

  // Intensités multipliées par π, et lueur sans atténuation : depuis la r155, three compte les
  // lumières en unités physiques, et les chiffres d'origine donnaient une scène presque éteinte.
  scene.add(new THREE.HemisphereLight(0xc6ddf0, 0x1b1f26, 0.55 * Math.PI))
  const cle = new THREE.DirectionalLight(0xffffff, 0.95 * Math.PI)
  cle.position.set(600, 1400, -900)
  const remplissage = new THREE.DirectionalLight(0xa8c4de, 0.32 * Math.PI)
  remplissage.position.set(-900, 300, -500)
  const contre = new THREE.DirectionalLight(0x9fc8ec, 0.45 * Math.PI)
  contre.position.set(100, 800, 1200)
  scene.add(cle, remplissage, contre)
  const sol = new THREE.Mesh(new THREE.CircleGeometry(900, 64), new THREE.MeshStandardMaterial({ color: 0x1a1f26, roughness: 1 }))
  sol.rotation.x = -Math.PI / 2
  scene.add(sol)

  // ── le modèle : tant qu'il charge, les poses sont gardées et la dernière est appliquée à l'arrivée ──

  let N: Record<string, THREE.Object3D> | null = null
  let C: Cinematique | null = null
  let racine: THREE.Object3D | null = null
  let derniere: Pose | null = null
  let libere = false
  const base = { chariot: new THREE.Vector3() }
  // le bras et la tige du robot, dessinés à part : ceux du CAD ne sont pas les bons
  const acier = new THREE.MeshStandardMaterial({ color: 0xd8dde3, roughness: 0.35, metalness: 0.6 })
  const brasMonter = new THREE.Group()
  const lame = new THREE.Mesh(new THREE.BoxGeometry(4, 8, 48),
    new THREE.MeshStandardMaterial({ color: 0xb9c0c7, roughness: 0.35, metalness: 0.7 }))
  // bras goBILDA de 6 trous (48 × 8 × 4) : trous 1 et 4 vissés de part et d'autre de l'axe, rotule au trou 6
  lame.position.z = 8
  brasMonter.add(lame)
  const geoTige = new THREE.CylinderGeometry(1.6, 1.6, 1, 12)
  geoTige.translate(0, 0.5, 0)
  const tige = new THREE.Mesh(geoTige, acier)
  const rotuleBras = new THREE.Mesh(new THREE.SphereGeometry(4, 16, 12), acier)
  const rotuleLevier = rotuleBras.clone()
  const geoAxe = new THREE.CylinderGeometry(2, 2, 1, 12)
  geoAxe.translate(0, 0.5, 0)
  const axeRotule = new THREE.Mesh(geoAxe, acier)
  const a3 = new THREE.Vector3(), b3 = new THREE.Vector3(), c3 = new THREE.Vector3()
  const tmp = new THREE.Vector3()

  new GLTFLoader().load(urlModele, gltf => {
    if (libere) return
    racine = gltf.scene
    C = racine.userData as Cinematique
    const noeuds: Record<string, THREE.Object3D> = {}
    racine.traverse(objet => {
      if (objet.userData.name) noeuds[objet.userData.name] = objet
      // le GLB n'a pas de normales : l'ombrage plat les prend aux faces, à la volée
      if (objet instanceof THREE.Mesh) {
        objet.material.flatShading = true
        objet.material.needsUpdate = true
      }
    })
    N = noeuds
    base.chariot.copy(N.chariot.position)
    brasMonter.position.set(C.monter.xBras, C.monter.O2[1], C.monter.O2[2])
    scene.add(racine, brasMonter, tige, rotuleBras, rotuleLevier, axeRotule)
    if (derniere) appliquer(derniere)
    new THREE.Box3().setFromObject(racine).getBoundingSphere(englobante)
    cadrer(vue)
  }, undefined, erreur => console.error('Maquette : robot-walle.glb illisible', erreur))

  function appliquer(pose: Pose) {
    if (!N || !C || !racine) return
    N.panoramique.rotation.y = -pose.panoramique * RAD
    N.tete.rotation.x = pose.inclinaison * RAD
    // le pignon du Stingray roule autour de la roue fixe : deux fois l'inclinaison, par rapport à la tête
    N.pignon_inclinaison.rotation.x = C.pignon.rapport * pose.inclinaison * RAD

    // le parallélogramme : les barres tournent, le chariot translate sans tourner
    const m = C.monter, q = monter(m, pose.monterDescendre), t0 = m.theta0 * RAD
    for (const b of BARRES) N[b].rotation.x = -(q.barre - t0)
    // le moyeu est dans la pose de l'export : il tourne de l'écart entre les deux angles de bras
    N.palonnier_monter.rotation.x = -(q.phi - monter(m, C.export.monterDescendre).phi)
    N.chariot.position.set(base.chariot.x,
      base.chariot.y + m.barre * (Math.sin(q.barre) - Math.sin(t0)),
      base.chariot.z + m.barre * (Math.cos(q.barre) - Math.cos(t0)))
    brasMonter.rotation.x = -q.phi
    // La tige est plane : ses 87 mm sont ceux que la courbe mesurée confirme dans le plan du mouvement.
    // L'écart latéral jusqu'au levier est l'axe de la rotule, un boulon transversal.
    a3.set(m.xBras, q.M[1], q.M[0])
    b3.set(m.xBras, q.R[1], q.R[0])
    c3.set(m.xLevier, q.R[1], q.R[0])
    tendre(tige, a3, b3)
    tendre(axeRotule, c3, b3)
    rotuleBras.position.copy(a3)
    rotuleLevier.position.copy(b3)

    // les yeux : la coque suit le quadrilatère, le palonnier obéit au servo
    for (const [cote, s, oeil] of [['droit', 1, pose.oeilDroit], ['gauche', -1, pose.oeilGauche]] as const) {
      const servo = servoDepuisOeil(oeil)
      N['coque_' + cote].rotation.z = s * oeilDepuisServo(servo) * RAD
      N['palonnier_oeil_' + cote].rotation.z = s * servo * RAD
    }
    racine.updateMatrixWorld(true)
    // les bielles se tendent entre la rotule du palonnier, mobile, et l'ancrage de la platine, fixe :
    // elles ne se placent qu'une fois les matrices à jour
    const T = C.pivots.tete
    for (const cote of ['droit', 'gauche'] as const) {
      const { S, P, D } = C.oeil[cote]
      tmp.set(P[0] - S[0], P[1] - S[1], P[2] - S[2])
      N['palonnier_oeil_' + cote].localToWorld(tmp)
      N.tete.worldToLocal(tmp)
      const bielle = N['bielle_oeil_' + cote]
      bielle.position.copy(tmp)
      bielle.rotation.z = Math.atan2(D[1] - T[1] - tmp.y, D[0] - T[0] - tmp.x) - Math.atan2(D[1] - P[1], D[0] - P[0])
    }
  }

  // ── navigation : glisser pour tourner, clic droit (ou Maj, ou deux doigts) pour déplacer,
  //    molette ou pincement pour zoomer ───

  function placerCamera() {
    const phi = Math.max(0.25, Math.min(Math.PI - 0.25, orbite.phi))
    camera.position.set(
      cible.x + orbite.rayon * Math.sin(phi) * Math.sin(orbite.theta),
      cible.y + orbite.rayon * Math.cos(phi),
      cible.z - orbite.rayon * Math.sin(phi) * Math.cos(orbite.theta))
    camera.lookAt(cible)
  }

  function cadrer(nouvelle: Vue) {
    vue = nouvelle
    // la tête là où elle est au moment du clic ; dans une colonne étroite, c'est sa largeur qui borne le zoom
    const sphere = englobante.clone()
    if (vue === 'visage' && N) {
      new THREE.Box3().setFromObject(N.tete).getBoundingSphere(sphere)
      sphere.radius *= 1.05
    }
    // la sphère doit tenir dans le plus étroit des deux champs : le panneau est en hauteur
    const demiV = camera.fov * RAD / 2
    const demiH = Math.atan(Math.tan(demiV) * camera.aspect)
    cible.copy(sphere.center)
    orbite = { ...TROIS_QUARTS, rayon: sphere.radius / Math.sin(Math.min(demiV, demiH)) }
    placerCamera()
  }

  /** Glisse la cible dans le plan de l'écran, du nombre de pixels donné. */
  function deplacer(dx: number, dy: number) {
    const mmParPixel = 2 * orbite.rayon * Math.tan(camera.fov * RAD / 2) / canevas.clientHeight
    camera.updateMatrixWorld()
    const droite = new THREE.Vector3().setFromMatrixColumn(camera.matrixWorld, 0)
    const haut = new THREE.Vector3().setFromMatrixColumn(camera.matrixWorld, 1)
    cible.addScaledVector(droite, -dx * mmParPixel).addScaledVector(haut, dy * mmParPixel)
    placerCamera()
  }

  const canevas = renderer.domElement
  let glisse = false, translation = false, dernier = { x: 0, y: 0 }, pincement: number | null = null
  let milieu = { x: 0, y: 0 }
  const zoomer = (facteur: number) => {
    orbite.rayon = Math.max(300, Math.min(3500, orbite.rayon * facteur))
    placerCamera()
  }
  const surAppui = (e: PointerEvent) => {
    glisse = true
    translation = e.button === 2 || e.button === 1 || e.shiftKey
    dernier = { x: e.clientX, y: e.clientY }
    canevas.setPointerCapture(e.pointerId)
  }
  const surDeplacement = (e: PointerEvent) => {
    if (!glisse || pincement !== null) return
    if (translation) {
      deplacer(e.clientX - dernier.x, e.clientY - dernier.y)
    } else {
      orbite.theta += (e.clientX - dernier.x) * 0.006
      orbite.phi -= (e.clientY - dernier.y) * 0.006
      placerCamera()
    }
    dernier = { x: e.clientX, y: e.clientY }
  }
  const surRelache = () => { glisse = false }
  const surMolette = (e: WheelEvent) => {
    e.preventDefault()
    zoomer(1 + Math.sign(e.deltaY) * 0.09)
  }
  const surDoubleClic = () => cadrer(vue)
  const surMenu = (e: MouseEvent) => e.preventDefault()
  const surToucher = (e: TouchEvent) => {
    if (e.touches.length !== 2) return
    e.preventDefault()
    const [a, b] = [e.touches[0], e.touches[1]]
    const d = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY)
    const m = { x: (a.clientX + b.clientX) / 2, y: (a.clientY + b.clientY) / 2 }
    if (pincement !== null) {
      zoomer(pincement / d)
      deplacer(m.x - milieu.x, m.y - milieu.y)
    }
    pincement = d
    milieu = m
  }
  const surFinToucher = () => { pincement = null }

  canevas.addEventListener('pointerdown', surAppui)
  canevas.addEventListener('pointermove', surDeplacement)
  for (const type of ['pointerup', 'pointercancel', 'pointerleave'] as const) {
    canevas.addEventListener(type, surRelache)
  }
  canevas.addEventListener('wheel', surMolette, { passive: false })
  canevas.addEventListener('dblclick', surDoubleClic)
  canevas.addEventListener('contextmenu', surMenu)
  canevas.addEventListener('touchmove', surToucher, { passive: false })
  canevas.addEventListener('touchend', surFinToucher)
  placerCamera()

  return {
    element: canevas,

    poser(pose) {
      derniere = pose
      appliquer(pose)
    },

    rendre() {
      renderer.render(scene, camera)
    },

    dimensionner(largeur, hauteur) {
      if (largeur === 0 || hauteur === 0) return
      camera.aspect = largeur / hauteur
      camera.updateProjectionMatrix()
      renderer.setSize(largeur, hauteur)
      cadrer(vue)
    },

    cadrer,

    liberer() {
      libere = true
      canevas.removeEventListener('pointerdown', surAppui)
      canevas.removeEventListener('pointermove', surDeplacement)
      for (const type of ['pointerup', 'pointercancel', 'pointerleave'] as const) {
        canevas.removeEventListener(type, surRelache)
      }
      canevas.removeEventListener('wheel', surMolette)
      canevas.removeEventListener('dblclick', surDoubleClic)
      canevas.removeEventListener('contextmenu', surMenu)
      canevas.removeEventListener('touchmove', surToucher)
      canevas.removeEventListener('touchend', surFinToucher)
      scene.traverse(objet => {
        if (objet instanceof THREE.Mesh) {
          objet.geometry.dispose()
          objet.material.dispose()
        }
      })
      renderer.dispose()
    },
  }
}
