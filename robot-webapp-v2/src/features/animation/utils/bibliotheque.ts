import { animationApi } from '../../../shared/api/animationApi'
import { VERSION_ANIMATION, type Animation } from '../../../shared/types/animation'

/*
 * La bibliothèque vue de l'éditeur : celle du robot, plus ce qui attend de lui être envoyé.
 *
 * On écrit des animations robot éteint, sur le PC, avec la maquette pour seul retour. Tant que le
 * robot ne répond pas, Enregistrer les garde dans le navigateur ; elles partent dès qu'il revient.
 * Avant, l'enregistrement échouait avec un message discret en pied de page, et un rechargement
 * emportait le travail.
 *
 * Le navigateur n'est qu'une salle d'attente : une animation envoyée en sort. C'est aussi pour ça
 * qu'il y a l'export en fichier — des données de navigateur, ça s'efface.
 */

const CLE = 'atelier.animationsEnAttente'

type EnAttente = Record<string, Animation>

function lireAttente(): EnAttente {
  try {
    return JSON.parse(localStorage.getItem(CLE) ?? '{}') as EnAttente
  } catch {
    return {}
  }
}

function ecrireAttente(attente: EnAttente) {
  try {
    localStorage.setItem(CLE, JSON.stringify(attente))
  } catch {
    // Stockage refusé (navigation privée, quota) : l'export en fichier reste le seul recours.
  }
}

export interface Liste {
  /** null : le robot n'a pas répondu. */
  robot: string[] | null
  enAttente: string[]
}

export const bibliotheque = {
  async lister(): Promise<Liste> {
    const enAttente = Object.keys(lireAttente())
    try {
      return { robot: await animationApi.noms(), enAttente }
    } catch {
      return { robot: null, enAttente }
    }
  },

  /** La version en attente passe avant celle du robot : c'est la plus récente. */
  async charger(nom: string): Promise<Animation> {
    return lireAttente()[nom] ?? animationApi.charger(nom)
  },

  estEnAttente(nom: string): boolean {
    return nom in lireAttente()
  },

  /**
   * Au robot s'il l'accepte, sinon dans le navigateur, avec la raison. Robot éteint et robot qui
   * refuse ne se distinguent pas toujours — derrière le proxy Vite, les deux font une erreur HTTP —
   * alors on garde dans les deux cas, et la raison dit lequel.
   */
  async enregistrer(animation: Animation): Promise<{ ou: 'robot' } | { ou: 'navigateur'; raison: string }> {
    try {
      await animationApi.enregistrer(animation.nom, animation)
      const attente = lireAttente()
      delete attente[animation.nom]
      ecrireAttente(attente)
      return { ou: 'robot' }
    } catch (e) {
      ecrireAttente({ ...lireAttente(), [animation.nom]: animation })
      return { ou: 'navigateur', raison: e instanceof Error ? e.message : String(e) }
    }
  },

  /** La copie en attente et celle du robot. Robot injoignable, la sienne reste et reparaîtra. */
  async supprimer(nom: string): Promise<void> {
    const attente = lireAttente()
    const locale = nom in attente
    delete attente[nom]
    ecrireAttente(attente)
    try {
      await animationApi.supprimer(nom)
    } catch (e) {
      if (!locale) throw e
    }
  },

  /**
   * Envoie tout ce qui attend. Une animation que le robot refuse reste en attente : on ne perd
   * rien, et on la retrouve au prochain essai.
   */
  async envoyerEnAttente(): Promise<{ envoyees: string[]; remplacees: string[] }> {
    const attente = lireAttente()
    const noms = Object.keys(attente)
    if (noms.length === 0) return { envoyees: [], remplacees: [] }
    const surLeRobot = new Set(await animationApi.noms())
    const envoyees: string[] = []
    for (const nom of noms) {
      try {
        await animationApi.enregistrer(nom, attente[nom])
        envoyees.push(nom)
      } catch {
        // reste en attente
      }
    }
    const restantes = lireAttente()
    envoyees.forEach(nom => delete restantes[nom])
    ecrireAttente(restantes)
    return { envoyees, remplacees: envoyees.filter(nom => surLeRobot.has(nom)) }
  },
}

// ── Fichiers ─────────────────────────────────────────────────────────────────

export function exporterFichier(animation: Animation) {
  const blob = new Blob([JSON.stringify(animation, null, 2)], { type: 'application/json' })
  const lien = document.createElement('a')
  lien.href = URL.createObjectURL(blob)
  lien.download = `${animation.nom}.json`
  lien.click()
  URL.revokeObjectURL(lien.href)
}

/**
 * Relit un fichier exporté. La version est vérifiée ici plutôt qu'au robot : une animation d'une
 * autre version serait acceptée par l'éditeur, puis ignorée par le robot au redémarrage.
 */
export async function lireFichier(fichier: File): Promise<Animation> {
  let animation: Partial<Animation>
  try {
    animation = JSON.parse(await fichier.text())
  } catch {
    throw new Error(`« ${fichier.name} » n'est pas un fichier JSON`)
  }
  if (!Array.isArray(animation.pistes)) {
    throw new Error(`« ${fichier.name} » n'est pas une animation`)
  }
  if (animation.version !== VERSION_ANIMATION) {
    throw new Error(`« ${fichier.name} » est en version ${animation.version ?? '?'}, l'éditeur attend la ${VERSION_ANIMATION}`)
  }
  const nom = typeof animation.nom === 'string' && animation.nom
    ? animation.nom
    : fichier.name.replace(/\.json$/i, '')
  return { ...animation, nom, sons: animation.sons ?? [] } as Animation
}
