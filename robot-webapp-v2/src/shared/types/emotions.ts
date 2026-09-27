/**
 * Une émotion du robot, par sa clé (« joie », « colere »…).
 *
 * La liste elle-même n'est pas ici : elle vit dans l'enum `Emotion` du robot, arrive par
 * `/api/emotions`, et le navigateur en garde une copie pour l'Atelier robot éteint (voir
 * `useEmotions`).
 */
export type Emotion = string

export interface EmotionConnue {
  cle: Emotion
  libelle: string
  emoji: string
}
