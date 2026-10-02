package fr.roboteek.robot.organes.capteurs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** De l'accéléromètre à ce qui a secoué le robot. */
class SecousseTest {

    private static final double TOLERANCE = 1e-9;

    private final Secousse secousse = new Secousse();

    @Test
    void sansEchantillonRienNEstMesure() {
        assertNull(secousse.relever());
    }

    /** Immobile, le robot ne lit que la gravité : penché ou non, il n'est pas secoué. */
    @Test
    void lInclinaisonNEstPasUneSecousse() {
        double angle = Math.toRadians(30);
        secousse.noter(new double[]{Math.sin(angle), 0, Math.cos(angle)});
        assertEquals(0, secousse.relever(), TOLERANCE);
    }

    /** Le choc tient en un échantillon : le relevé doit le garder, même suivi d'un calme. */
    @Test
    void unChocBrefNEstPasEcraseParLEchantillonSuivant() {
        secousse.noter(new double[]{0, 0, 1});
        secousse.noter(new double[]{1.5, 0, 1});
        secousse.noter(new double[]{0, 0, 1});
        assertEquals(Math.sqrt(3.25) - 1, secousse.relever(), TOLERANCE);
    }

    @Test
    void laChuteLibreSeVoitAussi() {
        secousse.noter(new double[]{0, 0, 0.1});
        assertEquals(0.9, secousse.relever(), TOLERANCE);
    }

    @Test
    void chaqueReleveRepartDeZero() {
        secousse.noter(new double[]{0, 0, 2});
        secousse.relever();
        assertNull(secousse.relever());
        secousse.noter(new double[]{0, 0, 1.1});
        assertEquals(0.1, secousse.relever(), TOLERANCE);
    }

    @Test
    void uneValeurDemesureeEstIgnoree() {
        secousse.noter(new double[]{1e300, 1e300, 1e300});
        assertNull(secousse.relever());
    }
}
