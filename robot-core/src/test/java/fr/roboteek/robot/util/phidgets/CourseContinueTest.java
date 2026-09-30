package fr.roboteek.robot.util.phidgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseContinueTest {

    private static final long SECONDE = 1_000_000_000L;

    /** Le panoramique à la manette : 40 unités/s, accélération raide. */
    private static final CourseContinue VERS_LA_DROITE = new CourseContinue(95, 1, 40, 2000, 0);

    @Test
    void enCroisiereLaPositionAvanceALaVitesseDuControleur() {
        // 1 s à 40 u/s, moins le retard de la rampe de départ (40² / 2·2000 = 0,4)
        assertEquals(95 + 40 - 0.4, VERS_LA_DROITE.positionA(SECONDE), 1e-9);
    }

    /** Sur un axe à accélération douce, ignorer la rampe ferait une erreur de plusieurs unités. */
    @Test
    void pendantLaRampeLaPositionSuitLAcceleration() {
        CourseContinue douce = new CourseContinue(0, 1, 40, 60, 0);

        assertEquals(60 * 0.25 * 0.25 / 2, douce.positionA(SECONDE / 4), 1e-9);
    }

    @Test
    void versLaButeeBasseLaPositionRecule() {
        CourseContinue versLaGauche = new CourseContinue(95, -1, 40, 2000, 0);

        assertEquals(95 - 40 + 0.4, versLaGauche.positionA(SECONDE), 1e-9);
    }

    /**
     * La cible d'arrêt est devant la position, de la distance de freinage : viser la position
     * elle-même ferait dépasser le servo puis revenir, un à-coup en arrière.
     */
    @Test
    void laCibleDArretLaisseLaDistanceDeFreinage() {
        assertEquals(95 + 40 - 0.4 + 0.4, VERS_LA_DROITE.cibleDArretA(SECONDE, 35, 155), 1e-9);
    }

    @Test
    void laCibleDArretNeDepassePasLaButee() {
        assertEquals(155, VERS_LA_DROITE.cibleDArretA(10 * SECONDE, 35, 155));
    }

    /** L'œil droit, monté en miroir, a sa butée « min » au-dessus de sa butée « max ». */
    @Test
    void desButeesInverseesNeFontPasLever() {
        CourseContinue oeilDroit = new CourseContinue(97, -1, 20, 2000, 0);

        assertEquals(88.87, oeilDroit.cibleDArretA(10 * SECONDE, 104.77, 88.87), 1e-9);
    }

    @Test
    void unInstantAnterieurAuLancementLaisseAuDepart() {
        assertEquals(95, new CourseContinue(95, 1, 40, 2000, SECONDE).positionA(0));
    }
}
