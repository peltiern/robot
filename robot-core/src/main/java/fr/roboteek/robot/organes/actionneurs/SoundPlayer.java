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
import java.nio.file.Path;
import java.util.Optional;

/**
 * Lecteur de sons du robot.
 * <p>
 * Cycle de vie géré par {@link SmartLifecycle},
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

    /** Le {@code play} en cours pour un son du Studio, ou {@code null} si le robot se tait. */
    private volatile Process lecture;

    private volatile String sonEnCours;

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
     * Joue un fichier son du Studio, sans attendre la fin.
     * <p>
     * À la différence de {@link #play(RobotSound)}, qui bloque son appelant, la lecture est rendue
     * tout de suite : la demande vient d'un appel HTTP, et tenir la requête ouverte pendant la
     * seconde que dure le son ferait passer le Studio pour lent alors qu'il ne l'est pas.
     * <p>
     * Un seul son à la fois : le robot n'a qu'une bouche, et deux {@code play} en parallèle se
     * mélangeraient sur la même carte son. Lancer un son en coupe donc un autre.
     *
     * @return faux si l'organe n'est pas démarré ou si {@code play} n'a pas pu être lancé
     */
    public synchronized boolean jouer(String nom, Path fichier) {
        if (!running) {
            return false;
        }
        arreterLecture();
        final ReconnaissanceVocaleControleEvent pause = new ReconnaissanceVocaleControleEvent();
        pause.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE);
        applicationEventPublisher.publishEvent(pause);
        try {
            Process process = new ProcessBuilder("play", "-q", fichier.toString()).start();
            lecture = process;
            sonEnCours = nom;
            process.onExit().thenRun(() -> finDeLecture(process));
            return true;
        } catch (IOException e) {
            logger.error("Son « {} » non joué", nom, e);
            finDeLecture(null);
            return false;
        }
    }

    /** Le son du Studio en cours de lecture, s'il y en a un. */
    public Optional<String> sonEnCours() {
        return Optional.ofNullable(sonEnCours);
    }

    /**
     * Coupe le son en cours ; rend faux si le robot ne jouait rien. La reconnaissance vocale est
     * relancée par {@link #finDeLecture}, que la fin soit voulue ou naturelle.
     */
    public synchronized boolean arreterLecture() {
        Process process = lecture;
        if (process == null) {
            return false;
        }
        process.destroy();
        return true;
    }

    private synchronized void finDeLecture(Process process) {
        if (process != null && lecture != process) {
            // Un son plus récent a pris la place : c'est lui qui rendra l'écoute, pas celui-ci.
            return;
        }
        lecture = null;
        sonEnCours = null;
        final ReconnaissanceVocaleControleEvent redemarrage = new ReconnaissanceVocaleControleEvent();
        redemarrage.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.DEMARRER);
        applicationEventPublisher.publishEvent(redemarrage);
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
        // Un `play` laissé derrière soi continuerait de parler après l'arrêt de l'organe, et la
        // reconnaissance vocale resterait en pause sans que personne ne la relance.
        arreterLecture();
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
