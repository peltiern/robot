package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * Lecteur de sons du robot.
 * <p>
 * Premier organe migré en bean Spring : cycle de vie géré par {@link SmartLifecycle},
 * évènements reçus via {@link EventListener} (relayés depuis le bus Guava par le pont
 * tant que la migration n'est pas terminée).
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Component
public class SoundPlayer extends AbstractOrgane implements SmartLifecycle {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SoundPlayer.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Joue un son.
     *
     * @param sound le son à jouer
     */
    public void play(RobotSound sound) {
        if (sound != null) {

            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE);
            applicationEventPublisher.publishEvent(eventPause);

            logger.debug("Lecture son :\t{}", sound);

            try {
                Process p = new ProcessBuilder("play", Constantes.DOSSIER_SONS + File.separator + sound.getFileName())
                        .start();
                p.waitFor();
            } catch (IOException e) {
                logger.error("Erreur lors de la lecture du son {}", sound, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Lecture du son {} interrompue", sound);
            }

            logger.debug("Fin lecture :\t{}", sound);

            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
            eventRedemarrage.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.DEMARRER);
            applicationEventPublisher.publishEvent(eventRedemarrage);
        }
    }

    /**
     * Intercepte les évènements pour jouer un son.
     *
     * @param playSoundEvent évènement pour jouer un son
     */
    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handlePlaySoundEvent(PlaySoundEvent playSoundEvent) {
        if (running && playSoundEvent.getSound() != null) {
            play(playSoundEvent.getSound());
        }
    }

    @Override
    public void initialiser() {

    }

    @Override
    public void arreter() {

    }

    @Override
    public void start() {
        initialiser();
        running = true;
        logger.info("SoundPlayer démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("SoundPlayer arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.ACTIONNEURS;
    }
}
