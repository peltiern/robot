// Chemin relatif, comme le reste de l'application : c'est le proxy Vite qui pointe sur le robot.
const VOIX = '/api/voix'

/** Les réglages de la voix, dans les unités du robot — voir `ReglagesVoix` côté robot. */
export interface ReglagesVoix {
  /** demi-tons, négatif : plus grave */
  hauteur: number
  /** multiplie la vitesse d'élocution */
  debit: number
  /** Hz, 0 : rien de coupé */
  passeHaut: number
  /** Hz, 20000 : rien de coupé */
  passeBas: number
  /** dB de saturation */
  grain: number
  /** chorus et flanger, 0 à 1 */
  machine: number
  /** écho de boîte, 0 à 1 */
  metal: number
  /** Hz de hachage, 0 : rien */
  modulation: number
}

/** Une voix de base : un modèle Piper, et pour un modèle à plusieurs voix, laquelle. */
export interface ModeleVoix {
  fichier: string
  locuteur: number | null
  nom: string
}

/** Une voix complète. `modele` à null : celui de robot.properties. */
export interface Voix {
  modele: string | null
  locuteur: number | null
  reglages: ReglagesVoix
}

export interface EtatVoix {
  adoptee: Voix
  /** La coloration Wall-E d'origine. */
  origine: ReglagesVoix
  /** Faux si le robot parle avec Google : la voix ne se règle que pour Piper. */
  reglable: boolean
  /** Les modèles déposés sur le robot. */
  modeles: ModeleVoix[]
  modeleParDefaut: string
}

async function requete<T>(url: string, init?: RequestInit): Promise<T> {
  const reponse = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...init })
  if (!reponse.ok) throw new Error((await reponse.text()) || `${reponse.status} ${reponse.statusText}`)
  const texte = await reponse.text()
  return (texte ? JSON.parse(texte) : undefined) as T
}

export const voixApi = {
  etat: (): Promise<EtatVoix> => requete(VOIX),

  /** Répond la voix gardée : le robot borne les réglages à ce que sox accepte. */
  adopter: (voix: Voix): Promise<Voix> => requete(VOIX, { method: 'PUT', body: JSON.stringify(voix) }),

  /** Le robot dit la phrase avec cette voix, sans l'adopter. */
  essayer: (texte: string, voix: Voix): Promise<void> =>
    requete(`${VOIX}/essai`, { method: 'POST', body: JSON.stringify({ texte, voix }) }),
}
