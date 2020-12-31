package fr.roboteek.robot.systemenerveux.event;

/**
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class RobotEvent {

	private String eventType;
	
	private long timestamp;

	private boolean processedByBrain;
	
	public RobotEvent(String eventType) {
		this.eventType = eventType;
		this.timestamp = System.currentTimeMillis();
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isProcessedByBrain() {
		return processedByBrain;
	}

	public void setProcessedByBrain(boolean processedByBrain) {
		this.processedByBrain = processedByBrain;
	}
}
