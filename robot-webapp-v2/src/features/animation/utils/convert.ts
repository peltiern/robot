import { VERSION_ANIMATION, type Animation, type Axe, type Piste } from '../../../shared/types/animation'
import type { EditorTrack } from '../store/animationStore'

/**
 * La frontière entre le modèle de l'éditeur et le contrat du robot.
 *
 * Tout ce qui part sur le fil passe par ici, et nulle part ailleurs : c'est ce qui permet de
 * renommer un champ du contrat en ne touchant qu'à ce fichier, et de voir d'un coup d'oeil ce
 * que le robot reçoit vraiment.
 */

/** L'état de l'éditeur, mis à la forme que le robot lit. */
export function toAnimation(
  nom: string,
  dureeTotale: number,
  tracks: EditorTrack[],
): Animation {
  const pistes: Piste[] = tracks
    .filter(t => t.kfs.length > 0)
    .map(t => ({
      axe: t.id as Axe,
      vitesseParDefaut: t.defaultVelocity,
      accelerationParDefaut: t.defaultAcceleration,
      imagesCles: [...t.kfs]
        .sort((a, b) => a.t - b.t)
        .map(kf => ({ instant: kf.t, valeur: kf.v })),
    }))
  // sons : l'éditeur ne les connaît pas encore. Le backend conserve ceux de la version
  // enregistrée quand la liste arrive vide, donc les envoyer ainsi n'efface rien.
  return { nom, dureeTotale, pistes, sons: [], version: VERSION_ANIMATION }
}

/**
 * Le chemin inverse : les images-clés de toutes les pistes, regroupées par instant.
 *
 * C'est la forme qu'attend `chargerEtapes` — l'éditeur raisonne en « à cet instant, ces axes sont
 * à ces valeurs », alors que le fichier est rangé par axe.
 */
export function versEtapesEditeur(animation: Animation) {
  const parInstant = new Map<number, Partial<Record<Axe, number>>>()
  for (const piste of animation.pistes ?? []) {
    for (const imageCle of piste.imagesCles ?? []) {
      if (!parInstant.has(imageCle.instant)) parInstant.set(imageCle.instant, {})
      parInstant.get(imageCle.instant)![piste.axe] = imageCle.valeur
    }
  }
  return [...parInstant.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([t, vals]) => ({ t, vals }))
}

/**
 * Temps du curseur en secondes : « 1,50 s », et non « 1:50 », qui se lisait comme une minute cinquante.
 * Toujours deux décimales, pour que le compteur ne change pas de largeur pendant la lecture.
 */
export function fmtMs(ms: number): string {
  return (ms / 1000).toFixed(2).replace('.', ',') + ' s'
}

export function uid() { return Math.random().toString(36).slice(2, 9) }
