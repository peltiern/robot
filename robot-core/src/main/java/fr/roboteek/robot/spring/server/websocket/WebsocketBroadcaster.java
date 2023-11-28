package fr.roboteek.robot.spring.server.websocket;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
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
    private final Gson gson;

    public WebsocketBroadcaster(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.gson = new Gson();
        RobotEventBus.getInstance().subscribe(this);
    }

    @Subscribe
    public void handleRobotEvent(RobotEvent robotEvent) {
        if (robotEvent instanceof VideoEvent) {
            simpMessagingTemplate.convertAndSend("/video", gson.toJson(robotEvent));
        } else if (robotEvent instanceof AudioEvent) {
            simpMessagingTemplate.convertAndSend("/audio", robotEvent);
        } else {
            simpMessagingTemplate.convertAndSend("/events/" + robotEvent.getEventType(), robotEvent);
        }

    }
}
