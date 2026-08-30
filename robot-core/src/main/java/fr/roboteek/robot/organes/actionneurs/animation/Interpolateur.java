package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Où se trouve chaque axe à un instant donné de l'animation.
 * <p>
 * L'interpolation est une spline de Catmull-Rom, <b>strictement la même que celle que l'éditeur
 * dessine</b> ({@code catmullRom.ts}) : c'est ce qui fait que le robot exécute la courbe affichée
 * à l'écran et non une approximation. Le contrôle en est fait par un test qui compare les deux
 * implémentations sur les mêmes points.
 * <p>
 * Une spline plutôt qu'une interpolation linéaire parce qu'elle passe exactement par les points
 * posés tout en gardant une vitesse continue à leur traversée : un mouvement peut franchir une
 * image-clé sans marquer l'arrêt, ce qu'une suite de segments droits ne sait pas faire.
 * <p>
 * Elle ne connaît rien aux évènements ni aux moteurs : elle rend des degrés, le lecteur en fait
 * des consignes. C'est ce qui permet de la tester sans robot.
 */
public final class Interpolateur {

    private Interpolateur() {
    }

    /**
     * Position de chaque axe commandé par l'animation à l'instant demandé. Les axes que
     * l'animation ne commande pas sont absents de la table — et non à zéro.
     */
    public static Map<Axe, Double> positionsA(Animation animation, long instant) {
        Map<Axe, Double> positions = new EnumMap<>(Axe.class);
        for (Piste piste : animation.pistes()) {
            if (!piste.estVide()) {
                positions.put(piste.axe(), valeurA(piste, instant));
            }
        }
        return positions;
    }

    /** Position d'un axe à l'instant demandé. */
    public static double valeurA(Piste piste, long instant) {
        return valeurA(piste.imagesCles(), instant);
    }

    /**
     * Interpolation de Catmull-Rom sur des images-clés <b>déjà triées</b> par instant croissant
     * ({@link Piste} s'en charge à la construction).
     * <p>
     * Hors des bornes, la valeur est tenue : avant la première image-clé et après la dernière,
     * l'axe ne bouge pas. Extrapoler donnerait une position que personne n'a demandée, sur un axe
     * qui a des butées.
     */
    public static double valeurA(List<ImageCle> imagesCles, long instant) {
        if (imagesCles.isEmpty()) {
            return 0;
        }
        ImageCle premiere = imagesCles.getFirst();
        ImageCle derniere = imagesCles.getLast();
        if (imagesCles.size() == 1 || instant <= premiere.instant()) {
            return premiere.valeur();
        }
        if (instant >= derniere.instant()) {
            return derniere.valeur();
        }

        // Intervalle [p1, p2] qui encadre l'instant
        int indice = 1;
        while (indice < imagesCles.size() && imagesCles.get(indice).instant() <= instant) {
            indice++;
        }
        ImageCle p1 = imagesCles.get(indice - 1);
        ImageCle p2 = imagesCles.get(indice);

        // Points fantômes aux extrémités : sans eux la tangente du premier et du dernier segment
        // serait indéfinie. Les prolonger à valeur constante donne une courbe qui démarre et
        // finit à plat, ce qu'on veut d'une animation qui part du repos et y revient.
        long duree = p2.instant() - p1.instant();
        ImageCle p0 = indice > 1 ? imagesCles.get(indice - 2) : new ImageCle(p1.instant() - duree, p1.valeur());
        ImageCle p3 = indice < imagesCles.size() - 1 ? imagesCles.get(indice + 1) : new ImageCle(p2.instant() + duree, p2.valeur());

        double u = (double) (instant - p1.instant()) / duree;
        double u2 = u * u;
        double u3 = u2 * u;
        double v0 = p0.valeur();
        double v1 = p1.valeur();
        double v2 = p2.valeur();
        double v3 = p3.valeur();

        return 0.5 * (2 * v1
                + (-v0 + v2) * u
                + (2 * v0 - 5 * v1 + 4 * v2 - v3) * u2
                + (-v0 + 3 * v1 - 3 * v2 + v3) * u3);
    }
}
