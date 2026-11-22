package fr.roboteek.robot.services.navigation;

import fr.roboteek.robot.configuration.RobotConfig;

/**
 * Ce qu'il faut savoir des chenilles pour changer des tops d'encodeur en millimètres.
 * <p>
 * Un seul nombre pour la distance, les tops par mètre, et non un diamètre de barbotin et un
 * rapport de transmission : les moteurs (réducteurs 71,2:1) ne se tournent pas à la main, on ne
 * peut donc pas compter les tops d'un tour de barbotin. Ce qui se mesure, c'est un trajet roulé au
 * mètre ruban, et il englobe d'un coup le rapport, le diamètre et l'épaisseur de la chenille.
 *
 * @param topsParMetre tops d'encodeur comptés pour un mètre roulé
 * @param voieMm       écart entre les deux chenilles, en millimètres
 */
public record GeometrieChenilles(double topsParMetre, double voieMm) {

    public static GeometrieChenilles depuis(RobotConfig config) {
        return new GeometrieChenilles(config.chenillesEncodeurTopsParMetre(), config.chenillesVoieMm());
    }

    public double millimetres(long tops) {
        return tops * 1000 / topsParMetre;
    }
}
