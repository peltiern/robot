package fr.roboteek.robot.services.navigation;

/**
 * Où est le robot et vers où il regarde, dans le repère fixé à son démarrage : origine là où il
 * se trouvait, axe x droit devant lui, y à sa gauche, cap compté positivement vers la gauche.
 * <p>
 * Le millimètre en {@code double}, et non en entier : l'ébauche de 2025 arrondissait chaque pas au
 * millimètre, et à vingt relevés par seconde de quelques millimètres chacun, l'arrondi mangeait une
 * part sensible du trajet.
 *
 * @param xMm    abscisse, en millimètres
 * @param yMm    ordonnée, en millimètres
 * @param capRad cap en radians, ramené dans ]-π, π]
 */
public record Pose(double xMm, double yMm, double capRad) {

    public static final Pose ORIGINE = new Pose(0, 0, 0);

    public double capDegres() {
        return Math.toDegrees(capRad);
    }

    @Override
    public String toString() {
        return String.format("x=%.0f mm, y=%.0f mm, cap=%.1f°", xMm, yMm, capDegres());
    }
}
