package fr.roboteek.robot.organes.capteurs;

/**
 * Attitude du robot en degrés : roulis (penché sur le côté), tangage (penché en avant ou en
 * arrière), lacet (cap).
 * <p>
 * Le quaternion fourni par la centrale est la seule forme sans ambiguïté, mais personne ne lit un
 * quaternion : l'interface et les seuils de basculement raisonnent en angles. La conversion suit
 * la convention aéronautique Z-Y-X ; le tangage est donc borné à ±90°, et près de cette limite le
 * roulis et le lacet perdent leur sens (blocage de cardan). Un robot sur chenilles qui en arrive
 * là est de toute façon tombé.
 */
public record Attitude(double roulis, double tangage, double lacet) {

    /**
     * Convertit un quaternion unitaire dans l'ordre que livre Phidget22 : {@code [x, y, z, w]}.
     * L'ordre compte — le prendre pour {@code [w, x, y, z]} donne des angles plausibles mais faux.
     */
    public static Attitude depuisQuaternion(double[] q) {
        double x = q[0];
        double y = q[1];
        double z = q[2];
        double w = q[3];

        double roulis = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
        // Bornage avant asin : les arrondis de l'algorithme poussent parfois l'argument à
        // 1,0000001, et asin rend alors NaN.
        double sinTangage = Math.max(-1, Math.min(1, 2 * (w * y - z * x)));
        double tangage = Math.asin(sinTangage);
        double lacet = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

        return new Attitude(Math.toDegrees(roulis), Math.toDegrees(tangage), Math.toDegrees(lacet));
    }
}
