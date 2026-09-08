package fr.roboteek.robot.web.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@CrossOrigin(origins = "*")
public class WebsocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketController.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Gson dédié au décodage des évènements entrants : {@link RobotEventAdapter} choisit la
     * sous-classe concrète (ParoleEvent, MouvementYeuxEvent, …) d'après le champ {@code eventType}.
     * On décode ici explicitement, plutôt que de compter sur les convertisseurs de messages Spring :
     * sous Spring Boot 4 / Jackson 3, le convertisseur JSON par défaut tente d'instancier la classe
     * abstraite {@code RobotEvent} et échoue (« Cannot construct instance of RobotEvent »).
     */
    private final Gson gson;

    public WebsocketController(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        // Adaptateur java.time : Gson ne sait pas accéder à LocalDateTime par réflexion sous
        // Java >= 9 (module java.base non ouvert) — même contrainte que côté diffusion. On le
        // lit en ISO-8601 (le champ dateEvent de RobotEvent), sinon la construction de l'adaptateur
        // réflexif échoue dès qu'un évènement contient ce champ.
        this.gson = new GsonBuilder()
                .registerTypeAdapter(RobotEvent.class, new RobotEventAdapter())
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                        (json, type, context) -> (json == null || json.isJsonNull())
                                ? null
                                : LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
    }

    @MessageMapping("/robotevents")
    public void processRobotEvent(@Payload String json) {
        RobotEvent robotEvent = gson.fromJson(json, RobotEvent.class);
        if (robotEvent == null) {
            LOGGER.warn("Évènement robot non reconnu reçu sur /robotevents : {}", json);
            return;
        }
        // DEBUG et non INFO : un glissement de curseur en produit douze par seconde et par axe, et
        // le toString d'un MouvementCouEvent fait vingt champs. Sur le Jetson, où le journal part
        // dans le pilote json-file de Docker, c'est de l'écriture disque synchrone sur le thread
        // qui reçoit les ordres.
        LOGGER.debug("Évènement robot reçu du client : {}", robotEvent);
        applicationEventPublisher.publishEvent(robotEvent);
    }
}
