import type { Animation, AnimationEnCours } from '../types/animation'

// Chemins relatifs, comme tout le reste de l'application : c'est le proxy Vite qui pointe sur
// le robot (`wall-e.local:8080` par défaut, cf. vite.config.ts), et en production la page est
// servie par le robot lui-même. Une URL absolue tirée d'une variable d'environnement — ce que
// faisait le brouillon d'origine — vise localhost quand la variable manque, et la bibliothèque
// s'affiche « Backend non disponible » alors que tout le reste de la page fonctionne.
const BIBLIOTHEQUE = '/api/animations'
const EN_COURS = '/api/animation-en-cours'

/**
 * Le message d'erreur, en préférant celui du robot au code HTTP.
 *
 * Un « 404 Not Found » nu envoie chercher une faute d'URL. Le robot, lui, sait pourquoi il
 * refuse — un fichier d'animation écrit dans une version antérieure du format, par exemple — et
 * le dit dans les avertissements de sa réponse. Un essai s'est perdu là-dessus le 2026-09-04.
 */
async function raison(reponse: Response): Promise<string> {
  try {
    const corps = await reponse.json()
    if (Array.isArray(corps?.avertissements) && corps.avertissements.length > 0) {
      return corps.avertissements.join(' ')
    }
  } catch {
    // Corps absent ou illisible : le code HTTP est tout ce qu'on a.
  }
  return `${reponse.status} ${reponse.statusText}`
}

async function requete<T>(url: string, init?: RequestInit): Promise<T> {
  const reponse = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!reponse.ok) throw new Error(await raison(reponse))
  if (reponse.status === 204) return undefined as T
  return reponse.json()
}

/**
 * La bibliothèque d'animations et la lecture en cours.
 *
 * Deux ressources et non des verbes : ce qui s'appelait `/{nom}/play` et `/stop` est devenu
 * `/api/animation-en-cours`, qu'on remplace ou qu'on retire. Le robot n'en joue qu'une.
 */
export const animationApi = {
  noms: (): Promise<string[]> =>
    requete(BIBLIOTHEQUE),

  charger: (nom: string): Promise<Animation> =>
    requete(`${BIBLIOTHEQUE}/${encodeURIComponent(nom)}`),

  /** Crée ou remplace. Rend les avertissements du vérificateur, qui n'empêchent pas d'enregistrer. */
  enregistrer: (nom: string, animation: Animation): Promise<string[]> =>
    requete(`${BIBLIOTHEQUE}/${encodeURIComponent(nom)}`, {
      method: 'PUT',
      body: JSON.stringify(animation),
    }),

  supprimer: (nom: string): Promise<void> =>
    requete(`${BIBLIOTHEQUE}/${encodeURIComponent(nom)}`, { method: 'DELETE' }),

  /** Joue une animation enregistrée, par son nom. */
  jouer: (nom: string): Promise<AnimationEnCours> =>
    requete(EN_COURS, { method: 'PUT', body: JSON.stringify({ nom }) }),

  /**
   * Joue un brouillon sans l'enregistrer — ce dont l'éditeur a besoin pendant qu'on écrit un
   * geste. L'obliger à enregistrer pour voir bouger la tête polluerait la bibliothèque d'essais.
   */
  jouerBrouillon: (animation: Animation): Promise<AnimationEnCours> =>
    requete(EN_COURS, { method: 'PUT', body: JSON.stringify({ animation }) }),

  arreter: (): Promise<void> =>
    requete(EN_COURS, { method: 'DELETE' }),

  /** Ce que le robot joue en ce moment, ou null. */
  enCours: (): Promise<AnimationEnCours | null> =>
    requete<AnimationEnCours | undefined>(EN_COURS).then(r => r ?? null),
}
