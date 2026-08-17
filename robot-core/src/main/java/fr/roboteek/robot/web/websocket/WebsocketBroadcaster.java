package fr.roboteek.robot.web.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import fr.roboteek.robot.systemenerveux.event.AudioEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diffuse les évènements du robot vers les clients Websocket.
 * <p>
 * Écoute les évènements Spring (relayés depuis le bus Guava par le pont
 * tant que la migration n'est pas terminée).
 */
public class WebsocketBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketBroadcaster.class);

    /**
     * Template permmettant l'envoi de messages dans le Websocket.
     */
    private SimpMessagingTemplate simpMessagingTemplate;
    private final Gson gson;

    public WebsocketBroadcaster(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        // Adaptateur java.time : Gson ne sait pas sérialiser LocalDateTime par réflexion
        // sous Java >= 9 (module java.base non ouvert). On le rend en ISO-8601.
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, type, context) ->
                                new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .create();
    }

    @EventListener
    public void handleRobotEvent(RobotEvent robotEvent) {
        // Diffusion best-effort vers l'UI : un échec de sérialisation ou d'envoi ne doit
        // jamais remonter à l'organe émetteur (publication synchrone) et le tuer.
        try {
            if (robotEvent instanceof VideoEvent) {
                simpMessagingTemplate.convertAndSend("/video", gson.toJson(robotEvent));
            } else if (robotEvent instanceof AudioEvent) {
                simpMessagingTemplate.convertAndSend("/audio", robotEvent);
            } else {
                simpMessagingTemplate.convertAndSend("/events/" + robotEvent.getEventType(), robotEvent);
            }
        } catch (RuntimeException e) {
            logger.warn("Diffusion Websocket échouée pour l'évènement {} : {}",
                    robotEvent.getEventType(), e.getMessage());
        }
    }
}
