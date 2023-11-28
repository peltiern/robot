package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour afficher la position.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class DisplayPositionEvent extends RobotEvent {

    public static final String EVENT_TYPE = "display-position";

    public DisplayPositionEvent() {
        super(EVENT_TYPE);
    }
}
