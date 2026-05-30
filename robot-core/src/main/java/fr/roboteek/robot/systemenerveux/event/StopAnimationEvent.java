package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour arrêter l'animation en cours.
 */
public class StopAnimationEvent extends RobotEvent {

    public static final String EVENT_TYPE = "stop-animation";

    public StopAnimationEvent() {
        super(EVENT_TYPE);
    }
}
