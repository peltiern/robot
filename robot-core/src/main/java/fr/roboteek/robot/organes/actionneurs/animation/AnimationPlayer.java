package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.organes.actionneurs.RobotSound;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.commons.RandomUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lecteur d'animations : déroule les étapes d'une animation dans son propre thread
 * en publiant les évènements de mouvements et de sons correspondants.
 * <p>
 * Migré en bean Spring : cycle de vie (thread de la boucle) géré par {@link SmartLifecycle},
 * évènements reçus via {@link EventListener}.
 */
@Component
public class AnimationPlayer extends AbstractOrganeWithThread implements SmartLifecycle {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(AnimationPlayer.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Mode automatique : génère des animations aléatoires (lu par le thread de la boucle,
     * modifié par les threads des listeners).
     */
    private volatile boolean automaticMode = false;

    /**
     * Liste des étapes d'animation à jouer.
     */
    private ConcurrentLinkedQueue<AnimationStep> animationSteps = new ConcurrentLinkedQueue<>();

    public AnimationPlayer() {
        super("AnimationPlayer");
    }

    @Override
    public void initialiser() {

    }

    @Override
    public void loop() {
        long nextAnimationEventTimer = System.currentTimeMillis();
        AnimationStep nextAnimationStep = null;
        while (!Thread.interrupted()) {
            // Récupération de la prochaine étape d'animation s'il n'y en a pas
            if (nextAnimationStep == null) {
                nextAnimationStep = animationSteps.poll();
                if (nextAnimationStep != null) {
                    // Calcul du timer de la prochaine étape d'animation
                    nextAnimationEventTimer = System.currentTimeMillis() + nextAnimationStep.getDelay();
                } else if (automaticMode) {
                    // En mode automatique, s'il n'y a plus d'étape d'animation en attente, on en génère une aléatoirement
                    nextAnimationStep = generateRandomAnimationStep();
                    // Calcul du timer de la prochaine étape d'animation
                    nextAnimationEventTimer = System.currentTimeMillis() + nextAnimationStep.getDelay();
                }
            }

            if (nextAnimationStep != null && nextAnimationEventTimer <= System.currentTimeMillis()) {
                // L'étape d'animation en attente doit être jouée
                playAnimationStep(nextAnimationStep);
                nextAnimationStep = null;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Restitution du flag d'interruption pour que la condition de la boucle le voie
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Intercepte les évènements pour jouer une animation.
     *
     * @param playAnimationEvent évènement pour jouer une animation
     */
    @EventListener
    public void handlePlayAnimationEvent(PlayAnimationEvent playAnimationEvent) {
        if (running && playAnimationEvent != null) {
            Animation animation = playAnimationEvent.getAnimation() != null ?
                    playAnimationEvent.getAnimation() : Animation.getAnimationByName(playAnimationEvent.getAnimationName());
            if (animation != null) {
                automaticMode = false;
                if (animation == Animation.RANDOM) {
                    automaticMode = true;
                } else if (CollectionUtils.isNotEmpty(animation.getSteps())) {
                    // Nettoyage de la liste des animations en cours
                    animationSteps.addAll(animation.getSteps());
                }
            }
        }
    }

    private void playAnimationStep(AnimationStep animationStep) {
        // Transformation de l'étape en évènement
        MouvementYeuxEvent mouvementYeuxEvent = animationStep.buildMouvementYeuxEvent();
        MouvementCouEvent mouvementCouEvent = animationStep.buildMouvementCouEvent();
        PlaySoundEvent playSoundEvent = animationStep.buildPlaySoundEvent();

        // Envoi des évènements dans le bus
        applicationEventPublisher.publishEvent(mouvementYeuxEvent);
        applicationEventPublisher.publishEvent(mouvementCouEvent);
        if (playSoundEvent != null) {
            applicationEventPublisher.publishEvent(playSoundEvent);
        }
    }

    private AnimationStep generateRandomAnimationStep() {
        // TODO
        long delay = RandomUtils.nextLong(500, 3000);

        // Mouvements des yeux
        // Mouvements des yeux qu'une fois sur 3
        int moveYeux = RandomUtils.nextInt(0, 3);
        double positionOeilGauche = MouvementYeuxEvent.POSITION_NEUTRE;
        double positionOeilDroit = MouvementYeuxEvent.POSITION_NEUTRE;
        if (moveYeux % 3 == 0) {
            double positionOeil = RandomUtils.nextDouble(Configurations.phidgetsConfig().eyeMotorRelativePositionMin(), 0);
            boolean moveYeuxSymetrique = RandomUtils.nextBoolean();
            if (moveYeuxSymetrique) {
                positionOeilGauche = positionOeil;
                positionOeilDroit = positionOeil;
            } else {
                if (RandomUtils.nextBoolean()) {
                    positionOeilGauche = 0;
                    positionOeilDroit = positionOeil;
                } else {
                    positionOeilGauche = positionOeil;
                    positionOeilDroit = 0;
                }
            }
        }

        // Mouvements du cou
        // Mouvements du cou qu'une fois sur 2
        int moveCouGaucheDroite = RandomUtils.nextInt(0, 2);
        int moveCouHautBas = RandomUtils.nextInt(0, 2);

        double positionCouGaucheDroite = MouvementCouEvent.POSITION_NEUTRE;
        double positionCouHautBas = MouvementCouEvent.POSITION_NEUTRE;

        // TODO : utiliser les MIN et MAX de chaque moteur
        double minCouGaucheDroite = -40;
        double maxCouGaucheDroite = 40;
        if (moveCouGaucheDroite % 2 == 0) {
            positionCouGaucheDroite = RandomUtils.nextDouble(minCouGaucheDroite, maxCouGaucheDroite);
        }
        double minCouHautBas = -30;
        double maxCouHautBas = 30;
        if (moveCouHautBas % 2 == 0) {
            positionCouHautBas = RandomUtils.nextDouble(minCouHautBas, maxCouHautBas);
        }

        // Lecture d'un son
        RobotSound sound = null;
        // TODO Gérer le son en animation aléatoire
        // TODO PB : le capteur vocal est mis en pause lorsqu'un son est joué ce qui altère la reconnaissance
//        int playSound = RandomUtils.nextInt(0, 5);
//        if (playSound % 5 == 0) {
//            int soundIndex = RandomUtils.nextInt(0, 4);
//            sound = RobotSound.values()[soundIndex];
//        }

        AnimationStep animationStep = new AnimationStep(delay, positionOeilGauche, positionOeilDroit, positionCouGaucheDroite, positionCouHautBas, sound);
        return animationStep;
    }

    @Override
    public void start() {
        initialiser();
        super.start();
        running = true;
        logger.info("AnimationPlayer démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("AnimationPlayer arrêté");
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
