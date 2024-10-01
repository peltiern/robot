package fr.roboteek.robot.systemenerveux.event;

import java.time.LocalDateTime;

/**
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class RobotEvent {

    private String eventType;

    private LocalDateTime dateTime;

    private boolean processedByBrain;

    public RobotEvent() {
    }

    public RobotEvent(String eventType) {
        this.eventType = eventType;
        this.dateTime = LocalDateTime.now();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public boolean isProcessedByBrain() {
        return processedByBrain;
    }

    public void setProcessedByBrain(boolean processedByBrain) {
        this.processedByBrain = processedByBrain;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public String toString() {
        return "RobotEvent{" +
                "eventType='" + eventType + '\'' +
                ", dateTime=" + dateTime +
                ", processedByBrain=" + processedByBrain +
                '}';
    }
}
