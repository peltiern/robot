/**
 * Chemin relatif, et non une URL absolue : en développement le proxy Vite renvoie
 * `/api` sur le robot (cf. `vite.config.ts`), et en production Spring sert le front
 * lui-même — dans les deux cas c'est la bonne machine.
 *
 * Une base bâtie sur `VITE_API_URL` avec `http://localhost:8080` en repli visait la
 * tablette elle-même dès que la variable manquait — et elle manque, `.env` ne
 * définit que `VITE_WS_URL`. Toutes les requêtes échouaient sur « Failed to fetch ».
 */
const BASE = '/api/personnes'

/** Une apparition dans la timeline. */
export interface Rencontre {
  /** ISO-8601. */
  instant: string
  type: 'PREMIERE' | 'RETOUR'
  /** Durée de l'absence qui a précédé ; -1 pour une première rencontre. */
  secondesDAbsence: number
}

export interface MessageConversation {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  texte: string
}

/**
 * Une personne connue du robot.
 *
 * `rencontres` et `conversation` sont absents de la liste et remplis par la
 * fiche : `undefined` veut dire « pas demandé », un tableau vide veut dire
 * « rien à montrer ». Confondre les deux ferait afficher « aucune conversation »
 * sur une fiche pas encore chargée.
 */
export interface Personne {
  id: string
  prenom: string
  /** ISO-8601, ou null si le robot ne l'a jamais rencontrée. */
  derniereRencontre: string | null
  /** Zéro veut dire : connue, mais elle ne sera jamais reconnue. */
  nombreDeVisages: number
  /** Vrai si un portrait existe ; l'image se demande à part, cf. `urlVignette`. */
  aUneVignette: boolean
  nombreDeRencontres: number
  rencontres?: Rencontre[]
  conversation?: MessageConversation[]
}

async function repondre<T>(reponse: Response): Promise<T> {
  if (!reponse.ok) throw new Error(`${reponse.status} ${reponse.statusText}`)
  return reponse.json() as Promise<T>
}

async function verifier(reponse: Response): Promise<void> {
  if (!reponse.ok) throw new Error(`${reponse.status} ${reponse.statusText}`)
}

/**
 * Adresse du portrait de quelqu'un.
 *
 * Le nombre d'empreintes sert de version : le robot met le cache une heure (une
 * liste de trente personnes redemanderait sinon trente images à chaque
 * affichage), et sans ce paramètre une photo tout juste ajoutée resterait
 * invisible jusqu'à expiration. Or le compte grimpe à chaque apprentissage,
 * caméra comme fichier — il change exactement quand l'image change.
 */
export function urlVignette(personne: Personne): string {
  return `${BASE}/${personne.id}/vignette?v=${personne.nombreDeVisages}`
}

function enMultipart(photos: File[], prenom?: string): FormData {
  const corps = new FormData()
  if (prenom !== undefined) corps.append('prenom', prenom)
  photos.forEach((photo) => corps.append('photos', photo))
  return corps
}

/** Message d'erreur du robot s'il en donne un, sinon le code HTTP. */
async function refus(reponse: Response): Promise<string> {
  try {
    const corps = (await reponse.json()) as { erreur?: string }
    if (corps.erreur) return corps.erreur
  } catch {
    // Réponse sans corps JSON : le code suffira.
  }
  return `${reponse.status} ${reponse.statusText}`
}

export const personneApi = {
  toutes: (): Promise<Personne[]> => fetch(BASE).then((r) => repondre<Personne[]>(r)),

  fiche: (id: string): Promise<Personne> =>
    fetch(`${BASE}/${id}`).then((r) => repondre<Personne>(r)),

  renommer: (id: string, prenom: string): Promise<Personne> =>
    fetch(`${BASE}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prenom }),
    }).then((r) => repondre<Personne>(r)),

  supprimer: (id: string): Promise<void> =>
    fetch(`${BASE}/${id}`, { method: 'DELETE' }).then(verifier),

  oublierLaConversation: (id: string): Promise<void> =>
    fetch(`${BASE}/${id}/conversation`, { method: 'DELETE' }).then(verifier),

  /** Crée quelqu'un d'un coup : son prénom et les photos qui apprendront son visage. */
  creer: async (prenom: string, photos: File[]): Promise<Personne> => {
    const reponse = await fetch(BASE, { method: 'POST', body: enMultipart(photos, prenom) })
    if (!reponse.ok) throw new Error(await refus(reponse))
    return (await reponse.json()) as Personne
  },

  /** Ajoute des photos à quelqu'un de déjà connu. */
  ajouterDesVisages: async (id: string, photos: File[]): Promise<Personne> => {
    const reponse = await fetch(`${BASE}/${id}/visages`, { method: 'POST', body: enMultipart(photos) })
    if (!reponse.ok) throw new Error(await refus(reponse))
    return (await reponse.json()) as Personne
  },
}
