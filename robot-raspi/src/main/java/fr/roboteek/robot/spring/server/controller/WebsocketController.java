package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@CrossOrigin(origins = "*")
public class WebsocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketController.class);

    @MessageMapping("/robotevents")
    public void processRobotEvent(@Payload RobotEvent robotEvent) {
        LOGGER.info(robotEvent.toString());
        System.out.println(robotEvent);
        RobotEventBus.getInstance().publishAsync(robotEvent);
    }
}
