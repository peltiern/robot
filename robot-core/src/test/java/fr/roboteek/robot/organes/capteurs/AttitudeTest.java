package fr.roboteek.robot.organes.capteurs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Du quaternion de la centrale aux angles qu'on lit. */
class AttitudeTest {

    private static final double TOLERANCE = 1e-9;

    /** Quaternion {@code [x, y, z, w]} d'une rotation d'angle donné autour d'un axe unitaire. */
    private static double[] rotation(double degres, double ax, double ay, double az) {
        double demi = Math.toRadians(degres) / 2;
        double s = Math.sin(demi);
        return new double[]{ax * s, ay * s, az * s, Math.cos(demi)};
    }

    @Test
    void aPlatTousLesAnglesSontNuls() {
        Attitude attitude = Attitude.depuisQuaternion(new double[]{0, 0, 0, 1});
        assertEquals(0, attitude.roulis(), TOLERANCE);
        assertEquals(0, attitude.tangage(), TOLERANCE);
        assertEquals(0, attitude.lacet(), TOLERANCE);
    }

    @Test
    void chaqueAxeDonneSonAngle() {
        assertEquals(30, Attitude.depuisQuaternion(rotation(30, 1, 0, 0)).roulis(), TOLERANCE);
        assertEquals(-20, Attitude.depuisQuaternion(rotation(-20, 0, 1, 0)).tangage(), TOLERANCE);
        assertEquals(135, Attitude.depuisQuaternion(rotation(135, 0, 0, 1)).lacet(), TOLERANCE);
    }

    /** Un quaternion lu dans le mauvais ordre ({@code w} en tête) se trahirait ici. */
    @Test
    void unRoulisNeFuitPasSurLesAutresAxes() {
        Attitude attitude = Attitude.depuisQuaternion(rotation(45, 1, 0, 0));
        assertEquals(0, attitude.tangage(), TOLERANCE);
        assertEquals(0, attitude.lacet(), TOLERANCE);
    }

    /** Les arrondis de la fusion peuvent dépasser la norme 1 : asin ne doit pas rendre NaN. */
    @Test
    void unQuaternionLegerementHorsNormeNeDonnePasNaN() {
        double r = Math.sqrt(0.5) * 1.0000001;
        Attitude attitude = Attitude.depuisQuaternion(new double[]{0, r, 0, r});
        assertFalse(Double.isNaN(attitude.tangage()));
        assertEquals(90, attitude.tangage(), 1e-3);
    }
}
