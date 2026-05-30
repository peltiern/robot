package fr.roboteek.robot.organes.actionneurs.animation;

/**
 * Contraintes physiques d'un axe moteur.
 * Les positions min/max sont exprimées en degrés relatifs (unités de l'animation),
 * pas en positions moteur absolues.
 */
public class MotorConstraints {

    /** Vitesse maximale du moteur en °/s. */
    private final double maxVelocity;

    /** Accélération maximale du moteur en °/s². */
    private final double maxAcceleration;

    /** Position minimale atteignable en degrés relatifs. */
    private final double minPosition;

    /** Position maximale atteignable en degrés relatifs. */
    private final double maxPosition;

    public MotorConstraints(double maxVelocity, double maxAcceleration,
                            double minPosition, double maxPosition) {
        this.maxVelocity = maxVelocity;
        this.maxAcceleration = maxAcceleration;
        this.minPosition = minPosition;
        this.maxPosition = maxPosition;
    }

    public double getMaxVelocity() { return maxVelocity; }
    public double getMaxAcceleration() { return maxAcceleration; }
    public double getMinPosition() { return minPosition; }
    public double getMaxPosition() { return maxPosition; }

    @Override
    public String toString() {
        return "MotorConstraints{vel=" + maxVelocity + ", accel=" + maxAcceleration
                + ", pos=[" + minPosition + ", " + maxPosition + "]}";
    }
}
