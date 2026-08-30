import type { AxeAnimable } from '../types/animation'

// Chemin relatif : c'est le proxy Vite qui pointe sur le robot (cf. animationApi).
const URL_AXES = '/api/axes-animables'

/**
 * Les axes que le robot sait animer, avec leurs vraies butées.
 *
 * Un axe absent de cette liste n'est pas réglé sur ce robot : l'éditeur ne doit pas proposer
 * d'animer ce qu'aucun servo ne suivrait.
 */
export const axeApi = {
  tous: (): Promise<AxeAnimable[]> =>
    fetch(URL_AXES).then(r => {
      if (!r.ok) throw new Error(r.statusText)
      return r.json()
    }),
}
