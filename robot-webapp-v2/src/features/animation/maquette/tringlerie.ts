/**
 * La tringlerie des yeux, relevée sur « Wall-E Eye.stl » (voir robot-core/3d/yeux-walle.html).
 *
 * Le servo n'est pas sur l'axe de l'œil : vissé dans la coque, il pousse une bielle ancrée sur la
 * platine fixe. C'est un quadrilatère O–S–P–D, dont le rapport va de 1,25 au neutre à 3,70 en bout
 * de course. Même relevé et même loi que TransmissionOeil côté Java : un aperçu qui ferait tourner
 * l'œil de l'angle du servo mentirait justement là où les gestes se jouent.
 *
 * Repère d'un œil : origine sur l'arbre des coques, x vers l'extérieur, y vers le haut, z vers
 * l'arrière, en millimètres. Angle d'œil positif = bord extérieur vers le haut, comme les pistes.
 */
export const MECA = {
  S: [86.132, -16.082],   // axe de sortie du servo, porté par la coque
  D: [13.0, -26.001],     // ancrage de la bielle, sur la platine fixe
  BRAS: 32.0,
  BIELLE: 84.728,
  PSI0: -73.652,          // orientation du bras au neutre
  Z_SERVO: 66.4,
  Z_BIELLE: 74.5,
  Z_ANCRAGE: 74.5,
  SERVO_MIN: -5.6,        // les deux coques se touchent
  SERVO_MAX: 21.3,
} as const

const RAD = Math.PI / 180
const DEG = 180 / Math.PI
const D_R = Math.hypot(MECA.D[0], MECA.D[1])
const D_A = Math.atan2(MECA.D[1], MECA.D[0])

/** Angle de la coque pour un angle de servo : fermeture exacte du quadrilatère, par Al-Kashi. */
export function oeilDepuisServo(servo: number): number {
  const a = (MECA.PSI0 + servo) * RAD
  const qx = MECA.S[0] + MECA.BRAS * Math.cos(a)
  const qy = MECA.S[1] + MECA.BRAS * Math.sin(a)
  const q = Math.hypot(qx, qy)
  const b = Math.atan2(qy, qx)
  const k = (q * q + D_R * D_R - MECA.BIELLE * MECA.BIELLE) / (2 * q * D_R)
  return (D_A - b + Math.acos(Math.max(-1, Math.min(1, k)))) * DEG
}

/** Réciproque, par dichotomie : la loi est strictement décroissante sur toute la course. */
export function servoDepuisOeil(oeil: number): number {
  let bas: number = MECA.SERVO_MIN
  let haut: number = MECA.SERVO_MAX
  if (oeil >= oeilDepuisServo(bas)) return bas
  if (oeil <= oeilDepuisServo(haut)) return haut
  for (let i = 0; i < 60; i++) {
    const milieu = (bas + haut) / 2
    if (oeilDepuisServo(milieu) > oeil) bas = milieu
    else haut = milieu
  }
  return (bas + haut) / 2
}
