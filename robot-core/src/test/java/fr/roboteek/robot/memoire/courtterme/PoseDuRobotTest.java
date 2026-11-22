package fr.roboteek.robot.memoire.courtterme;

import fr.roboteek.robot.services.navigation.GeometrieChenilles;
import fr.roboteek.robot.services.navigation.Pose;
import fr.roboteek.robot.systemenerveux.event.EncodeursChenillesEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La géométrie est celle de {@code RobotConfig}, lue et non recopiée : le test reste juste quel que
 * soit le réglage.
 */
class PoseDuRobotTest {

    private PoseDuRobot poseDuRobot;

    /**
     * Dix centimètres roulés, en tops et en millimètres (arrondis au top près). Un mètre d'un seul
     * relevé passerait pour un compteur remis à zéro : le robot en met cinq à le parcourir.
     */
    private long topsParDecimetre;
    private double millimetresParDecimetre;

    @BeforeEach
    void preparer() {
        poseDuRobot = new PoseDuRobot();
        GeometrieChenilles geometrie = GeometrieChenilles.depuis(robotConfig());
        topsParDecimetre = Math.round(geometrie.topsParMetre() / 10);
        millimetresParDecimetre = geometrie.millimetres(topsParDecimetre);
    }

    @Test
    void lePremierReleveNeDeplaceRien() {
        relever(5000, 5000);

        assertEquals(Pose.ORIGINE, poseDuRobot.pose());
    }

    @Test
    void unMetreRouleEnDixRelevesFaitAvancerDUnMetre() {
        relever(5000, 5000);
        for (int i = 1; i <= 10; i++) {
            relever(5000 + i * topsParDecimetre, 5000 + i * topsParDecimetre);
        }

        assertEquals(10 * millimetresParDecimetre, poseDuRobot.pose().xMm(), 1e-6);
        assertEquals(0, poseDuRobot.pose().yMm(), 1e-6);
    }

    /** Un compteur remis à zéro par un rebranchement ne doit pas faire reculer le robot de 10 m. */
    @Test
    void unSautDeCompteurReprendLaReferenceSansDeplacer() {
        relever(100_000, 100_000);
        relever(0, 0);

        assertEquals(Pose.ORIGINE, poseDuRobot.pose());

        relever(topsParDecimetre, topsParDecimetre);

        assertEquals(millimetresParDecimetre, poseDuRobot.pose().xMm(), 1e-6);
    }

    private void relever(long topsGauche, long topsDroit) {
        poseDuRobot.handleEncodeursChenillesEvent(new EncodeursChenillesEvent(topsGauche, topsDroit, false));
    }
}
