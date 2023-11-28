package fr.roboteek.robot.spring.server.repository;

import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RobotEventRepository extends JpaRepository<RobotEvent, Long> {

    List<RobotEvent> findByOrderByDateTimeDesc();

    @Query("SELECT e FROM MouvementCouEvent e WHERE e.positionGaucheDroite < ?1")
    List<RobotEvent> findByEventTypeAndPositionGaucheDroiteLessThan(Double position);
}
