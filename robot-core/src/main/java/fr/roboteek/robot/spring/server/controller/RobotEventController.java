package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.spring.server.repository.RobotEventRepository;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("robot-events")
public class RobotEventController {

    @Autowired
    private RobotEventRepository robotEventRepository;

    public RobotEventController(RobotEventRepository robotEventRepository) {
        this.robotEventRepository = robotEventRepository;
    }

    @GetMapping
    public List<RobotEvent> findAll() {
        return robotEventRepository.findByEventTypeAndPositionGaucheDroiteLessThan(-5D);
    }
}
