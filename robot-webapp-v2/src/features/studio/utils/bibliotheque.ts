import { sonApi } from '../../../shared/api/sonApi'
import { rendre } from '../synthese/moteur'
import type { Son } from '../synthese/types'
import { enBase64, enWav } from '../synthese/wav'

/*
 * La bibliothèque vue du Studio : celle du robot, plus ce qui attend de lui être envoyé.
 *
 * On fabrique des sons robot éteint, sur le PC, et on les entend dans le navigateur — c'est tout
 * l'intérêt du Studio. Tant que le robot ne répond pas, Enregistrer garde le son dans le
 * navigateur ; il part dès qu'il revient. Même mécanisme que pour les animations, pour les mêmes
 * raisons : sans lui, un rechargement emportait le travail.
 *
 * Le navigateur n'est qu'une salle d'attente. Un son envoyé en sort, et l'export en fichier reste
 * le recours : des données de navigateur, ça s'efface.
 */

const CLE = 'studio.sonsEnAttente'

type EnAttente = Record<string, Son>

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

/** Le WAV du son, prêt à voyager. C'est le navigateur qui le fabrique, jamais le robot. */
async function wavDe(son: Son): Promise<string> {
  const tampon = await rendre(son.morceaux, son.reglages)
  return enBase64(enWav(tampon))
}

export const bibliotheque = {
  async lister(): Promise<Liste> {
    const enAttente = Object.keys(lireAttente())
    try {
      return { robot: await sonApi.noms(), enAttente }
    } catch {
      return { robot: null, enAttente }
    }
  },

  /** La version en attente passe avant celle du robot : c'est la plus récente. */
  async charger(nom: string): Promise<Son> {
    return lireAttente()[nom] ?? (await sonApi.recette(nom))
  },

  estEnAttente(nom: string): boolean {
    return nom in lireAttente()
  },

  /**
   * Au robot s'il l'accepte, sinon dans le navigateur, avec la raison. Robot éteint et robot qui
   * refuse ne se distinguent pas toujours — derrière le proxy, les deux font une erreur HTTP —
   * alors on garde dans les deux cas, et la raison dit lequel.
   */
  async enregistrer(son: Son): Promise<{ ou: 'robot' } | { ou: 'navigateur'; raison: string }> {
    try {
      await sonApi.enregistrer(son.nom, son, await wavDe(son))
      const attente = lireAttente()
      delete attente[son.nom]
      ecrireAttente(attente)
      return { ou: 'robot' }
    } catch (e) {
      ecrireAttente({ ...lireAttente(), [son.nom]: son })
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
      await sonApi.supprimer(nom)
    } catch (e) {
      if (!locale) throw e
    }
  },

  /**
   * Envoie tout ce qui attend. Un son que le robot refuse reste en attente : on ne perd rien, et
   * on le retrouve au prochain essai.
   */
  async envoyerEnAttente(): Promise<{ envoyes: string[]; remplaces: string[] }> {
    const attente = lireAttente()
    const noms = Object.keys(attente)
    if (noms.length === 0) return { envoyes: [], remplaces: [] }
    const surLeRobot = new Set(await sonApi.noms())
    const envoyes: string[] = []
    for (const nom of noms) {
      try {
        await sonApi.enregistrer(nom, attente[nom], await wavDe(attente[nom]))
        envoyes.push(nom)
      } catch {
        // reste en attente
      }
    }
    const restants = lireAttente()
    envoyes.forEach((nom) => delete restants[nom])
    ecrireAttente(restants)
    return { envoyes, remplaces: envoyes.filter((nom) => surLeRobot.has(nom)) }
  },
}

/** Le WAV tel quel, pour l'écouter ailleurs ou le déposer à la main sur le robot. */
export async function exporterWav(son: Son) {
  const tampon = await rendre(son.morceaux, son.reglages)
  const blob = new Blob([enWav(tampon) as BlobPart], { type: 'audio/wav' })
  const lien = document.createElement('a')
  lien.href = URL.createObjectURL(blob)
  lien.download = `${son.nom}.wav`
  lien.click()
  URL.revokeObjectURL(lien.href)
}
