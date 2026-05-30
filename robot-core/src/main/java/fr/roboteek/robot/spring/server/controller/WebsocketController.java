package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.organes.actionneurs.animation.AnimationInterpolator;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopAnimationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.concurrent.atomic.AtomicReference;

@Controller
@CrossOrigin(origins = "*")
public class WebsocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketController.class);

    private final AnimationInterpolator interpolator = new AnimationInterpolator();

    /**
     * Dernière commande de scrub reçue.
     * Permet de ne traiter que la plus récente si plusieurs arrivent en rafale.
     */
    private final AtomicReference<AnimationScrubCommand> latestScrub = new AtomicReference<>();

    /** Réception d'événements génériques depuis le client. */
    @MessageMapping("/robotevents")
    public void processRobotEvent(@Payload RobotEvent robotEvent) {
        logger.debug("RobotEvent reçu : {}", robotEvent);
        RobotEventBus.getInstance().publishAsync(robotEvent);
    }

    /**
     * Joue une animation depuis l'éditeur (live preview).
     * Reçoit l'animation complète dans le body, sans la sauvegarder.
     */
    @MessageMapping("/animation/play")
    public void playAnimation(@Payload PlayAnimationEvent event) {
        logger.debug("Lecture animation via WebSocket : {}", event.getAnimationName());
        RobotEventBus.getInstance().publishAsync(event);
    }

    /** Arrête l'animation en cours. */
    @MessageMapping("/animation/stop")
    public void stopAnimation() {
        logger.debug("Arrêt animation via WebSocket");
        RobotEventBus.getInstance().publishAsync(new StopAnimationEvent());
    }

    /**
     * Scrubbing : déplace le robot à la position interpolée correspondant au playhead.
     * Seule la commande la plus récente est traitée en cas de rafale.
     */
    @MessageMapping("/animation/scrub")
    public void scrub(@Payload AnimationScrubCommand command) {
        if (command.getAnimation() == null) return;

        // Mémorise la commande et traite uniquement si elle est toujours la plus récente
        latestScrub.set(command);
        AnimationScrubCommand latest = latestScrub.get();
        if (latest != command) return;

        MouvementYeuxEvent yeuxEvent = interpolator.buildYeuxEvent(command.getAnimation(), command.getTime());
        MouvementCouEvent couEvent = interpolator.buildCouEvent(command.getAnimation(), command.getTime());

        if (yeuxEvent != null) RobotEventBus.getInstance().publishAsync(yeuxEvent);
        if (couEvent != null) RobotEventBus.getInstance().publishAsync(couEvent);

        logger.debug("Scrub à {}ms → yeux={}, cou={}", command.getTime(), yeuxEvent != null, couEvent != null);
    }
}
