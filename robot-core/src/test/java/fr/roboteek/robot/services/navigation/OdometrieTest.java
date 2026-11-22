package fr.roboteek.robot.services.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OdometrieTest {

    private static final double VOIE_MM = 380;

    /** Un millimètre : l'ordre de ce que les encodeurs savent distinguer. */
    private static final double TOLERANCE_MM = 1;

    private static final double TOLERANCE_RAD = Math.toRadians(0.1);

    @Test
    void enLigneDroiteLeRobotAvanceSansTourner() {
        Pose arrivee = Odometrie.avancer(Pose.ORIGINE, 1000, 1000, VOIE_MM);

        assertPose(1000, 0, 0, arrivee);
    }

    @Test
    void chenillesEnSensContraireLeRobotPivoteSurPlace() {
        double quartDeTourMm = Math.PI * VOIE_MM / 4;

        Pose arrivee = Odometrie.avancer(Pose.ORIGINE, -quartDeTourMm, quartDeTourMm, VOIE_MM);

        assertPose(0, 0, Math.PI / 2, arrivee);
    }

    /**
     * Un quart de cercle de 50 cm de rayon, parcouru en petits pas comme le feraient les relevés :
     * le robot doit finir 50 cm devant et 50 cm à gauche, tourné vers la gauche.
     */
    @Test
    void unArcSeParcourtParPetitsPasSansDeriver() {
        double rayonMm = 500;
        int pas = 200;
        double variationParPas = (Math.PI / 2) / pas;
        Pose pose = Pose.ORIGINE;
        for (int i = 0; i < pas; i++) {
            pose = Odometrie.avancer(pose,
                    (rayonMm - VOIE_MM / 2) * variationParPas,
                    (rayonMm + VOIE_MM / 2) * variationParPas,
                    VOIE_MM);
        }

        assertPose(rayonMm, rayonMm, Math.PI / 2, pose);
    }

    /**
     * Le défaut de l'ébauche de 2025 : chaque pas arrondi au millimètre. Mille pas de 0,4 mm y
     * faisaient zéro au lieu de 40 cm.
     */
    @Test
    void lesPetitsPasNeSePerdentPas() {
        Pose pose = Pose.ORIGINE;
        for (int i = 0; i < 1000; i++) {
            pose = Odometrie.avancer(pose, 0.4, 0.4, VOIE_MM);
        }

        assertPose(400, 0, 0, pose);
    }

    /** Le cap venu du gyroscope prime : les encodeurs ne donnent plus que la distance. */
    @Test
    void leCapPeutVenirDAilleursQueDesEncodeurs() {
        Pose tourneAGauche = new Pose(0, 0, Math.PI / 2);

        Pose arrivee = Odometrie.deplacer(tourneAGauche, 1000, 0);

        assertPose(0, 1000, Math.PI / 2, arrivee);
    }

    @Test
    void leCapResteDansUnSeulTour() {
        assertEquals(-Math.PI / 2, Odometrie.normaliser(3 * Math.PI / 2), 1e-12);
        assertEquals(Math.PI, Odometrie.normaliser(-Math.PI), 1e-12);
        assertEquals(0, Odometrie.normaliser(4 * Math.PI), 1e-12);
    }

    @Test
    void lesTopsSeChangentEnMillimetres() {
        GeometrieChenilles geometrie = new GeometrieChenilles(12951, VOIE_MM);

        assertEquals(1000, geometrie.millimetres(12951), 1e-9);
        assertEquals(-500, geometrie.millimetres(-12951 / 2), 0.1);
    }

    private static void assertPose(double xMm, double yMm, double capRad, Pose pose) {
        assertEquals(xMm, pose.xMm(), TOLERANCE_MM, "x");
        assertEquals(yMm, pose.yMm(), TOLERANCE_MM, "y");
        assertEquals(capRad, pose.capRad(), TOLERANCE_RAD, "cap");
    }
}
