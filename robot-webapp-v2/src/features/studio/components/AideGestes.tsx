import styles from './AideGestes.module.css'

/**
 * Les gestes de la frise, rappelés en permanence sous la bibliothèque.
 *
 * Ils vivaient dans le panneau du morceau, où ils disparaissaient dès qu'on choisissait un morceau
 * — c'est-à-dire au moment précis où l'on s'en sert.
 */
export function AideGestes() {
  return (
    <section className={styles.aide}>
      <h2 className={styles.titre}>Gestes</h2>
      <ul>
        <li>
          <b>Glisser un morceau</b> : le déplacer dans le temps, ou le monter et le descendre.
        </li>
        <li>
          <b>Tirer un bord</b> : l’allonger, le raccourcir.
        </li>
        <li>
          <b>Tirer un rond</b> : changer l’intonation.
        </li>
        <li>
          <b>Tirer sa pastille</b> : plus fort vers le haut, plus doux vers le bas — le ruban s’épaissit ou s’affine.
        </li>
        <li>
          <b>Double-toucher</b> un morceau pour ajouter un point, ou le vide pour ajouter un morceau.
        </li>
        <li>
          <b>Le crayon</b> : dessiner le son à main levée — une montée finit en question, une descente en soupir.
        </li>
        <li>
          <b>Espace</b> pour écouter, <b>Suppr</b> pour effacer, <b>Ctrl+Z</b> pour revenir.
        </li>
      </ul>
    </section>
  )
}
