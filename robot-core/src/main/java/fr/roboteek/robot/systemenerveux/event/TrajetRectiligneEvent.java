package fr.roboteek.robot.systemenerveux.event;

/**
 * Ordre d'avancer (ou de reculer, distance négative) en ligne droite d'une distance donnée, arrêté
 * sur les encodeurs des chenilles. Tout ordre de chenilles venu d'ailleurs l'interrompt.
 */
public class TrajetRectiligneEvent extends RobotEvent {

    public static final String EVENT_TYPE = "trajet-rectiligne";

    private double distanceMm;

    /** Pour la désérialisation de ce qui arrive par le websocket. */
    public TrajetRectiligneEvent() {
        super(EVENT_TYPE);
    }

    public TrajetRectiligneEvent(double distanceMm) {
        super(EVENT_TYPE);
        this.distanceMm = distanceMm;
    }

    public double getDistanceMm() {
        return distanceMm;
    }

    @Override
    public String toString() {
        return "TrajetRectiligneEvent{distanceMm=" + distanceMm + '}';
    }
}
