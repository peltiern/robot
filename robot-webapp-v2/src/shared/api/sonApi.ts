import type { Son } from '../../features/studio/synthese/types'

// Chemins relatifs, comme le reste de l'application : c'est le proxy Vite qui pointe sur le robot.
const BIBLIOTHEQUE = '/api/sons'
const EN_COURS = '/api/son-en-cours'

/** Le message du robot plutôt que le code HTTP : il dit pourquoi il refuse, le code ne dit rien. */
async function raison(reponse: Response): Promise<string> {
  try {
    const texte = await reponse.text()
    if (texte) return texte
  } catch {
    // Corps absent ou illisible.
  }
  return `${reponse.status} ${reponse.statusText}`
}

async function requete<T>(url: string, init?: RequestInit): Promise<T> {
  const reponse = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...init })
  if (!reponse.ok) throw new Error(await raison(reponse))
  // Le corps décide, pas le code : le robot répond 201 sans corps à la création d'un son. Lu comme
  // du JSON, ce vide faisait échouer l'enregistrement — le son partait pourtant bien au robot, et
  // le Studio le gardait en plus dans le navigateur en annonçant un robot injoignable.
  const texte = await reponse.text()
  return (texte ? JSON.parse(texte) : undefined) as T
}

/**
 * La bibliothèque des sons du robot.
 *
 * Un son y voyage en deux morceaux d'un seul envoi : sa <b>recette</b>, que le robot range sans la
 * relire, et son <b>WAV</b> en base64, qui est tout ce qu'il jouera. Le navigateur est seul à
 * savoir fabriquer le son ; le robot est seul à savoir le faire entendre sur sa tête.
 */
export const sonApi = {
  noms: (): Promise<string[]> => requete(BIBLIOTHEQUE),

  recette: (nom: string): Promise<Son> => requete(`${BIBLIOTHEQUE}/${encodeURIComponent(nom)}`),

  /** Crée. Le robot répond 409 si le nom est déjà pris — ce qui protège l'« Enregistrer sous ». */
  creer: (son: Son, wav: string): Promise<void> =>
    requete(BIBLIOTHEQUE, { method: 'POST', body: JSON.stringify({ nom: son.nom, recette: son, wav }) }),

  /** Crée ou remplace le son de ce nom : c'est l'URL qui fait foi, pas le nom de la recette. */
  enregistrer: (nom: string, son: Son, wav: string): Promise<void> =>
    requete(`${BIBLIOTHEQUE}/${encodeURIComponent(nom)}`, {
      method: 'PUT',
      body: JSON.stringify({ nom, recette: son, wav }),
    }),

  supprimer: (nom: string): Promise<void> =>
    requete(`${BIBLIOTHEQUE}/${encodeURIComponent(nom)}`, { method: 'DELETE' }),

  /** Fait entendre un son sur le haut-parleur du robot, qui ne sonne pas comme un poste. */
  jouer: (nom: string): Promise<{ nom: string }> =>
    requete(EN_COURS, { method: 'PUT', body: JSON.stringify({ nom }) }),

  arreter: (): Promise<void> => requete(EN_COURS, { method: 'DELETE' }),
}
