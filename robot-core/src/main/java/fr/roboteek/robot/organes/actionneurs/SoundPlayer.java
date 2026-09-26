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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
     * Temps accordé à un {@code play} coupé pour rendre la carte son. Après un {@code kill -9} c'est
     * presque immédiat ; la borne évite seulement d'attendre sans fin un processus récalcitrant.
     */
    private static final long ATTENTE_CARTE_RENDUE_MS = 200;

    /** Les {@code play} que nous avons coupés : leur sortie en erreur est voulue, pas un échec. */
    private final Set<Process> coupes = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
     * Un seul son à la fois : le robot n'a qu'une bouche, et {@code play} joue directement sur la
     * carte, qui n'accepte qu'un programme à la fois. Lancer un son en coupe donc un autre.
     *
     * @return faux si l'organe n'est pas démarré ou si {@code play} n'a pas pu être lancé
     */
    public boolean jouer(String nom, Path fichier) {
        return jouer(nom, fichier, 0);
    }

    /**
     * Joue un fichier son à partir de {@code depuisSecondes} : une animation lancée au milieu fait
     * entendre sa bande-son à partir de là, pas du début.
     */
    public synchronized boolean jouer(String nom, Path fichier, double depuisSecondes) {
        if (!running) {
            return false;
        }
        arreterLecture();
        final ReconnaissanceVocaleControleEvent pause = new ReconnaissanceVocaleControleEvent();
        pause.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE);
        applicationEventPublisher.publishEvent(pause);
        try {
            List<String> commande = new ArrayList<>(List.of("play", "-q", fichier.toString()));
            if (depuisSecondes > 0) {
                // `trim` de sox : ne garde que ce qui suit cet instant.
                commande.addAll(List.of("trim", String.format(Locale.ROOT, "%.3f", depuisSecondes)));
            }
            Process process = new ProcessBuilder(commande).start();
            lecture = process;
            sonEnCours = nom;
            process.onExit().thenRun(() -> {
                signalerUnEchec(nom, process);
                finDeLecture(process);
            });
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
        // Tué net, et non poliment. Poliment (`destroy`, un SIGTERM), `play` joue d'abord ce qui
        // reste dans son tampon : mesuré le 2026-09-25 sur le Jetson, il garde la carte 844 ms. Le
        // son suivant, lancé dans la foulée, ne trouvait pas la carte et échouait en silence — deux
        // sons côte à côte dans une animation, et le second ne jouait jamais.
        coupes.add(process);
        process.destroyForcibly();
        try {
            process.waitFor(ATTENTE_CARTE_RENDUE_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    /**
     * Un {@code play} qui échoue le dit sur sa sortie d'erreur, que personne ne lisait : c'est ainsi
     * que la carte occupée est restée invisible. Ceux que nous avons coupés, eux, sortent en erreur
     * à dessein.
     */
    private void signalerUnEchec(String nom, Process process) {
        if (coupes.remove(process) || process.exitValue() == 0) {
            return;
        }
        String erreur;
        try {
            erreur = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            erreur = "(sortie d'erreur illisible)";
        }
        logger.warn("Son « {} » : play a échoué (code {}) : {}", nom, process.exitValue(), erreur);
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
