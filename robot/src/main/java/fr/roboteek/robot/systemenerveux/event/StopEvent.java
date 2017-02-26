package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour stopper le robot.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class StopEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "Stop";
	
	public StopEvent() {
		super(EVENT_TYPE);
	}
	
}
