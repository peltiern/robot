package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Rect;

/**
 * Le carré qu'on découpe autour d'un visage détecté.
 * <p>
 * Partagé par {@link DecoupeDeVignette} et {@link NetteteDuVisage} : les deux doivent regarder
 * exactement la même zone, sans quoi on jugerait la qualité d'une image et on en enregistrerait
 * une autre.
 */
public final class CadrageDuVisage {

    /**
     * Marge autour de la boîte du visage, en proportion de sa taille. La détection cadre au plus
     * près des sourcils et du menton : sans marge, le portrait est un gros plan sur des yeux et
     * une bouche, où l'on ne reconnaît personne.
     */
    private static final double MARGE = 0.33;

    private CadrageDuVisage() {
    }

    /**
     * Carré centré sur le visage, marge comprise, ramené dans les bords de l'image.
     * <p>
     * Carré, parce qu'une vignette carrée à l'écran déformerait un portrait rectangulaire. Ramené
     * dans les bords, parce qu'un visage au ras du cadre — ce qui arrive tout le temps, le robot
     * suit les gens du regard — déborderait, et OpenCV lèverait en pleine boucle vidéo.
     *
     * @return {@code null} si le visage est trop près du bord pour qu'il reste quelque chose
     */
    public static Rect carreAutourDu(VisageDetecte visage, int largeurImage, int hauteurImage) {
        int cote = (int) Math.round(Math.max(visage.width(), visage.height()) * (1 + 2 * MARGE));
        cote = Math.min(cote, Math.min(largeurImage, hauteurImage));
        if (cote <= 0) {
            return null;
        }
        int centreX = visage.x() + visage.width() / 2;
        int centreY = visage.y() + visage.height() / 2;
        int x = borner(centreX - cote / 2, 0, largeurImage - cote);
        int y = borner(centreY - cote / 2, 0, hauteurImage - cote);
        return new Rect(x, y, cote, cote);
    }

    private static int borner(int valeur, int minimum, int maximum) {
        return Math.max(minimum, Math.min(valeur, maximum));
    }
}
