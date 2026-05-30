package fr.roboteek.robot.organes.actionneurs.animation;

/**
 * Keyframe d'un axe moteur à un instant précis.
 * La vitesse et l'accélération sont optionnelles : si null, les valeurs par défaut du track sont utilisées.
 */
public class Keyframe {

    /** Instant absolu depuis le début de l'animation (ms). */
    private long time;

    /** Position cible en degrés. */
    private double value;

    /** Vitesse en °/s. Null = utilise Track.defaultVelocity. */
    private Double velocity;

    /** Accélération en °/s². Null = utilise Track.defaultAcceleration. */
    private Double acceleration;

    public Keyframe() {}

    public Keyframe(long time, double value) {
        this(time, value, null, null);
    }

    public Keyframe(long time, double value, Double velocity, Double acceleration) {
        this.time = time;
        this.value = value;
        this.velocity = velocity;
        this.acceleration = acceleration;
    }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public Double getVelocity() { return velocity; }
    public void setVelocity(Double velocity) { this.velocity = velocity; }

    public Double getAcceleration() { return acceleration; }
    public void setAcceleration(Double acceleration) { this.acceleration = acceleration; }

    @Override
    public String toString() {
        return "Keyframe{t=" + time + ", v=" + value + ", vel=" + velocity + ", accel=" + acceleration + "}";
    }
}
