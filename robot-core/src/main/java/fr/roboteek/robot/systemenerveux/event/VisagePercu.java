package fr.roboteek.robot.systemenerveux.event;

/**
 * Un visage vu par le robot : sa boîte englobante dans l'image, et son nom si la
 * reconnaissance a abouti ({@code null} sinon — c'est alors un inconnu).
 * <p>
 * Type volontairement distinct de {@code RecognizedFace}, qui sert au flux vidéo : celui-ci
 * hérite de {@code Rectangle}, lequel traîne un {@code Parallelogram} de commons-geometry.
 * Sérialisé à chaque cycle de reconnaissance vers les clients, ce graphe pèserait bien plus
 * lourd que les quatre entiers qui nous intéressent.
 *
 * @param nom     nom de la personne reconnue, {@code null} si le visage n'est pas identifié
 * @param x       abscisse du coin haut-gauche de la boîte englobante, en pixels
 * @param y       ordonnée du coin haut-gauche de la boîte englobante, en pixels
 * @param largeur largeur de la boîte englobante, en pixels
 * @param hauteur hauteur de la boîte englobante, en pixels
 */
public record VisagePercu(String nom, int x, int y, int largeur, int hauteur) {

    /** Indique si le visage a été identifié. */
    public boolean estConnu() {
        return nom != null;
    }
}
