package fr.roboteek.robot.systemenerveux.event;

/**
 * Un visage vu par le robot : sa boîte englobante dans l'image, et l'identité de la personne
 * quand la reconnaissance a abouti ({@code null} sinon — c'est alors un inconnu).
 * <p>
 * L'identifiant <i>et</i> le prénom voyagent ensemble : le premier sert à décider (c'est lui
 * qui identifie, le prénom ne le fait pas), le second à parler et à afficher.
 * <p>
 * Type volontairement distinct de {@code RecognizedFace}, qui sert au flux vidéo : celui-ci
 * hérite de {@code Rectangle}, lequel traîne un {@code Parallelogram} de commons-geometry.
 * Sérialisé à chaque cycle de reconnaissance vers les clients, ce graphe pèserait bien plus
 * lourd que les quatre entiers qui nous intéressent.
 *
 * @param idPersonne identifiant de la {@code Personne} reconnue, {@code null} si inconnue
 * @param prenom     prénom de la personne reconnue, {@code null} si inconnue
 * @param x          abscisse du coin haut-gauche de la boîte englobante, en pixels
 * @param y          ordonnée du coin haut-gauche de la boîte englobante, en pixels
 * @param largeur    largeur de la boîte englobante, en pixels
 * @param hauteur    hauteur de la boîte englobante, en pixels
 */
public record VisagePercu(String idPersonne, String prenom, int x, int y, int largeur, int hauteur) {

    /** Indique si le visage a été rattaché à une personne connue. */
    public boolean estConnu() {
        return idPersonne != null;
    }
}
