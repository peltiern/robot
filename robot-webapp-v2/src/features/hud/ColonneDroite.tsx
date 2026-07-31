import { useHudStore } from '../../shared/hud/hudStore'
import { PanneauDialogue } from './PanneauDialogue'
import { PanneauVitaux } from './PanneauVitaux'
import styles from './hud.module.css'

/**
 * Colonne de droite : dialogue et vitaux, côte à côte avec la vidéo plutôt qu'à
 * la place. C'est tout l'objet du HUD — voir en même temps ce que le robot voit,
 * ce qu'il dit et comment il se porte.
 *
 * Quand les deux volets sont fermés, la colonne disparaît et rend sa largeur à
 * l'image.
 */
export function ColonneDroite() {
  const dialogue = useHudStore((s) => s.dialogue)
  const vitaux = useHudStore((s) => s.vitaux)

  if (!dialogue && !vitaux) return null

  return (
    <aside className={styles.col}>
      {dialogue && <PanneauDialogue />}
      {vitaux && <PanneauVitaux />}
    </aside>
  )
}
