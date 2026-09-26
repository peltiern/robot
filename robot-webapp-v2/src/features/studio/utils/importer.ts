import { REGLAGES_PAR_DEFAUT, VERSION_SON, type Son } from '../synthese/types'
import { enBase64, enWav } from '../synthese/wav'

/*
 * Un fichier son du poste, mis au format du Studio.
 *
 * La conversion se fait ici et non sur le robot : sa bande-son d'animation n'assemble que du
 * 44,1 kHz mono 16 bits et saute le reste. Un MP3 stéréo à 48 kHz passerait l'enregistrement puis
 * resterait muet dans toutes les animations.
 */

const SR = 44100

/** −50 dB : sous ce seuil, le début et la fin d'un fichier ne sont que du souffle. */
const SEUIL_SILENCE = Math.pow(10, -50 / 20)

/** Gardé de part et d'autre du son : rogner au ras coupe l'attaque d'une consonne. */
const MARGE_S = 0.01

/**
 * Les noms que le robot accepte — lettres (accents compris), chiffres, `._ -`, voir la règle de
 * `BibliothequeDesSons`. En forme composée : un nom de fichier venu d'un Mac écrit « é » en deux
 * caractères, et le robot range sous la forme composée.
 */
function nomPermis(fichier: string): string {
  const nom = fichier
    .replace(/\.[^.]*$/, '')
    .normalize('NFC')
    .replace(/[^\p{L}\p{M}\p{N}._ -]+/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 60)
    .trim()
  return nom || 'import'
}

/** Un nom libre : un import n'écrase jamais un son déjà rangé sous le même nom. */
function nomLibre(voulu: string, pris: Set<string>): string {
  if (!pris.has(voulu)) return voulu
  let n = 2
  while (pris.has(`${voulu} ${n}`)) n++
  return `${voulu} ${n}`
}

/** Décode, ramène à 44,1 kHz mono, puis retire — si on le demande — les silences du début et de la fin. */
async function convertir(octets: ArrayBuffer, rogner: boolean): Promise<AudioBuffer> {
  const decode = await new OfflineAudioContext(1, 1, SR).decodeAudioData(octets)
  // Le rendu dans un contexte à une voie fait le mélange stéréo → mono, et le changement de
  // fréquence, avec les filtres du navigateur.
  const contexte = new OfflineAudioContext(1, Math.max(1, Math.ceil(decode.duration * SR)), SR)
  const source = contexte.createBufferSource()
  source.buffer = decode
  source.connect(contexte.destination)
  source.start()
  const mono = await contexte.startRendering()
  if (!rogner) return mono

  const echantillons = mono.getChannelData(0)
  let debut = 0
  while (debut < echantillons.length && Math.abs(echantillons[debut]) < SEUIL_SILENCE) debut++
  let fin = echantillons.length - 1
  while (fin > debut && Math.abs(echantillons[fin]) < SEUIL_SILENCE) fin--
  if (debut >= echantillons.length) throw new Error('le fichier ne contient que du silence')

  const marge = Math.round(MARGE_S * SR)
  const garde = echantillons.slice(Math.max(0, debut - marge), Math.min(echantillons.length, fin + 1 + marge))
  const rogne = new AudioBuffer({ numberOfChannels: 1, length: garde.length, sampleRate: SR })
  rogne.copyToChannel(garde, 0)
  return rogne
}

/**
 * Le son importé, prêt à être enregistré. Son volume part au plafond : c'est ce qui le laisse tel
 * qu'il était dans le fichier.
 */
export async function importerFichier(fichier: File, nomsPris: Set<string>): Promise<Son> {
  const tampon = await convertir(await fichier.arrayBuffer(), true)
  return {
    nom: nomLibre(nomPermis(fichier.name), nomsPris),
    version: VERSION_SON,
    reglages: { ...REGLAGES_PAR_DEFAUT(), volume: 1 },
    morceaux: [],
    fichier: { original: enBase64(enWav(tampon)), origine: fichier.name },
  }
}

/**
 * Un WAV du robot devenu son importé : pour un son déposé à la main, dont la recette ne dit rien
 * que le Studio sache refaire. Il n'est pas rogné : on l'ouvre tel que le robot le joue.
 */
export async function originalDepuisWav(octets: ArrayBuffer): Promise<string> {
  return enBase64(enWav(await convertir(octets, false)))
}
