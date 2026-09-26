/**
 * Le tampon rendu, mis en fichier WAV — ce que le robot recevra, et rien d'autre.
 *
 * 16 bits, mono, 44,1 kHz : le format que {@code play} joue sans discuter sur le Jetson, et que la
 * bibliothèque du robot vérifie à l'en-tête avant d'accepter un son.
 */

/** Écrit un tampon audio en WAV. */
export function enWav(tampon: AudioBuffer): Uint8Array {
  const echantillons = tampon.getChannelData(0)
  const n = echantillons.length
  const octets = new Uint8Array(44 + n * 2)
  const vue = new DataView(octets.buffer)
  const texte = (position: number, mot: string) => {
    for (let i = 0; i < mot.length; i++) vue.setUint8(position + i, mot.charCodeAt(i))
  }

  texte(0, 'RIFF')
  vue.setUint32(4, 36 + n * 2, true)
  texte(8, 'WAVE')
  texte(12, 'fmt ')
  vue.setUint32(16, 16, true)
  vue.setUint16(20, 1, true)
  vue.setUint16(22, 1, true)
  vue.setUint32(24, tampon.sampleRate, true)
  vue.setUint32(28, tampon.sampleRate * 2, true)
  vue.setUint16(32, 2, true)
  vue.setUint16(34, 16, true)
  texte(36, 'data')
  vue.setUint32(40, n * 2, true)

  for (let i = 0; i < n; i++) {
    const v = Math.min(1, Math.max(-1, echantillons[i]))
    vue.setInt16(44 + i * 2, v < 0 ? v * 0x8000 : v * 0x7fff, true)
  }
  return octets
}

/**
 * Le même WAV en base64, tel qu'il voyage vers le robot.
 *
 * Encodé par paquets : `String.fromCharCode(...octets)` d'un seul coup dépasse la taille maximale
 * d'un appel de fonction dès quelques dizaines de milliers d'échantillons, et un son d'une seconde
 * en fait 44 100.
 */
export function enBase64(octets: Uint8Array): string {
  let binaire = ''
  const paquet = 0x8000
  for (let i = 0; i < octets.length; i += paquet) {
    binaire += String.fromCharCode(...octets.subarray(i, i + paquet))
  }
  return btoa(binaire)
}

/** Le chemin inverse : les échantillons d'un WAV du Studio (16 bits mono) gardé en base64. */
export function lireWavBase64(base64: string): Float32Array<ArrayBuffer> {
  const binaire = atob(base64)
  const octets = new Uint8Array(binaire.length)
  for (let i = 0; i < binaire.length; i++) octets[i] = binaire.charCodeAt(i)
  const vue = new DataView(octets.buffer)
  // On cherche le bloc « data » plutôt que de le supposer à l'octet 44 : un WAV peut porter d'autres
  // blocs avant lui.
  let position = 12
  while (position + 8 <= octets.length) {
    const identifiant = String.fromCharCode(...octets.subarray(position, position + 4))
    const taille = vue.getUint32(position + 4, true)
    if (identifiant === 'data') {
      const n = Math.floor(Math.min(taille, octets.length - position - 8) / 2)
      const echantillons = new Float32Array(n)
      for (let i = 0; i < n; i++) echantillons[i] = vue.getInt16(position + 8 + i * 2, true) / 0x8000
      return echantillons
    }
    position += 8 + taille + (taille & 1)
  }
  return new Float32Array(0)
}

/** Taille du fichier en kilo-octets, pour la dire à l'utilisateur. */
export const enKo = (octets: Uint8Array): number => Math.round(octets.length / 1024)
