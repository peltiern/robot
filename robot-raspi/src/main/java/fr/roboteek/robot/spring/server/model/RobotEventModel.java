package fr.roboteek.robot.spring.server.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Model to persit robot events.
 */
//@Entity
//@Table(name = "events")
public class RobotEventModel {

//    @Id
//    @Column(name = "id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "event_type")
    private String eventType;

//    @Column(name = "datetime")
    private LocalDateTime dateTime;

//    @Column(name = "processed_by_brain")
    private boolean processedByBrain;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isProcessedByBrain() {
        return processedByBrain;
    }

    public void setProcessedByBrain(boolean processedByBrain) {
        this.processedByBrain = processedByBrain;
    }
}
