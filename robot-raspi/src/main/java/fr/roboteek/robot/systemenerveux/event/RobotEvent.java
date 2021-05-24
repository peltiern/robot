package fr.roboteek.robot.systemenerveux.event;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="event_type", discriminatorType = DiscriminatorType.STRING)
public abstract class RobotEvent {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="event_type", insertable = false, updatable = false)
    private String eventType;

    @Column(name = "datetime")
    private LocalDateTime dateTime;

    @Column(name = "processed_by_brain")
    private boolean processedByBrain;

    public RobotEvent() {
    }

    public RobotEvent(String eventType) {
        this.eventType = eventType;
        this.dateTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
