package fr.roboteek.robot.spring.server.websocket;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.systemenerveux.event.AudioEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class WebsocketBroadcaster {

    /**
     * Template permmettant l'envoi de messages dans le Websocket.
     */
    private SimpMessagingTemplate simpMessagingTemplate;

    public WebsocketBroadcaster(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        RobotEventBus.getInstance().subscribe(this);
    }

    @Subscribe
    public void handleRobotEvent(RobotEvent robotEvent) {
        if (robotEvent instanceof VideoEvent) {
            simpMessagingTemplate.convertAndSend("/video", robotEvent);
        } else if (robotEvent instanceof AudioEvent) {
            simpMessagingTemplate.convertAndSend("/audio", robotEvent);
        } else {
            simpMessagingTemplate.convertAndSend("/events/" + robotEvent.getEventType(), robotEvent);
        }

    }
}
