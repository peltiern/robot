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

export interface EtatVoix {
  adoptee: ReglagesVoix
  origine: ReglagesVoix
  /** Faux si le robot parle avec Google : les réglages ne valent que pour Piper. */
  reglable: boolean
}

async function requete<T>(url: string, init?: RequestInit): Promise<T> {
  const reponse = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...init })
  if (!reponse.ok) throw new Error((await reponse.text()) || `${reponse.status} ${reponse.statusText}`)
  const texte = await reponse.text()
  return (texte ? JSON.parse(texte) : undefined) as T
}

export const voixApi = {
  etat: (): Promise<EtatVoix> => requete(VOIX),

  /** Répond les réglages gardés : le robot les borne à ce que sox accepte. */
  adopter: (reglages: ReglagesVoix): Promise<ReglagesVoix> =>
    requete(VOIX, { method: 'PUT', body: JSON.stringify(reglages) }),

  /** Le robot dit la phrase avec ces réglages, sans les adopter. */
  essayer: (texte: string, reglages: ReglagesVoix): Promise<void> =>
    requete(`${VOIX}/essai`, { method: 'POST', body: JSON.stringify({ texte, reglages }) }),
}
