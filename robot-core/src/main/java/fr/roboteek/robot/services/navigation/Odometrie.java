package fr.roboteek.robot.services.navigation;

/**
 * Calcul de l'odométrie d'une conduite différentielle : d'où l'on part, de combien chaque chenille
 * a avancé, où l'on arrive.
 * <p>
 * Le calcul est coupé en deux pour que le cap puisse venir d'ailleurs que des encodeurs. Sur des
 * chenilles, il le faut : en virage elles glissent, et les encodeurs comptent des tops qui ne
 * correspondent à aucune rotation réelle. Ils restent justes pour la distance, pas pour le cap.
 * Tant que la centrale inertielle n'est pas montée, {@link #avancer} déduit le cap des encodeurs ;
 * ensuite, {@link #deplacer} prendra la variation de cap mesurée par le gyroscope.
 */
public final class Odometrie {

    private Odometrie() {
    }

    /**
     * Déplacement déduit des seuls encodeurs.
     *
     * @param distanceGaucheMm chemin parcouru par la chenille gauche, positif en marche avant
     * @param distanceDroiteMm chemin parcouru par la chenille droite, positif en marche avant
     */
    public static Pose avancer(Pose depart, double distanceGaucheMm, double distanceDroiteMm, double voieMm) {
        return deplacer(depart,
                (distanceGaucheMm + distanceDroiteMm) / 2,
                (distanceDroiteMm - distanceGaucheMm) / voieMm);
    }

    /**
     * Avance de {@code distanceMm} en tournant de {@code variationCapRad} (positif vers la gauche).
     * <p>
     * Le pas est porté par le cap <b>moyen</b> du déplacement, et non par celui du départ : sur un
     * arc, c'est la corde qu'on parcourt, et elle est orientée à mi-chemin des deux caps.
     */
    public static Pose deplacer(Pose depart, double distanceMm, double variationCapRad) {
        double capMoyen = depart.capRad() + variationCapRad / 2;
        return new Pose(
                depart.xMm() + distanceMm * Math.cos(capMoyen),
                depart.yMm() + distanceMm * Math.sin(capMoyen),
                normaliser(depart.capRad() + variationCapRad));
    }

    /** Ramène un angle dans ]-π, π]. */
    static double normaliser(double angleRad) {
        double angle = Math.IEEEremainder(angleRad, 2 * Math.PI);
        return angle == -Math.PI ? Math.PI : angle;
    }
}
