package fr.roboteek.robot.systemenerveux.event;

/**
 * Un visage vu par le robot : sa boîte englobante en pixels, et qui c'est quand la reconnaissance
 * a abouti — {@code idPersonne} et {@code prenom} sont {@code null} pour un inconnu.
 * <p>
 * Volontairement distinct de {@code RecognizedFace}, qui sert au flux vidéo : celui-ci hérite de
 * {@code Rectangle}, lequel traîne un {@code Parallelogram} de commons-geometry. Sérialisé vers
 * les clients à chaque cycle, ce graphe pèserait bien plus que les quatre entiers qui nous
 * intéressent.
 */
public record VisagePercu(String idPersonne, String prenom, int x, int y, int largeur, int hauteur) {

    public boolean estConnu() {
        return idPersonne != null;
    }
}
