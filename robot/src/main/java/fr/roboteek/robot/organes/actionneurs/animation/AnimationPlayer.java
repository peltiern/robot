package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.RobotSound;
import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.commons.RandomUtils;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AnimationPlayer extends AbstractOrgane {

    /** Flag indiquant de stopper le thread. */
    private boolean stopperThread = false;

    /** Liste des étapes d'animation à jouer. */
    private ConcurrentLinkedQueue<AnimationStep> animationSteps = new ConcurrentLinkedQueue<>();

    @Override
    public void initialiser() {
        final Thread threadPlayer = new Thread("AnimationPlayer") {
            @Override
            public void run() {
                loop();
            }
        };
        threadPlayer.start();
    }

    @Override
    public void arreter() {
        stopperThread = true;
    }

    /**
     * Intercepte les évènements pour jouer une animation.
     * @param playAnimationEvent évènement pour jouer une animation
     */
    @Handler
    public void handlePlayAnimationEvent(PlayAnimationEvent playAnimationEvent) {
        if (playAnimationEvent != null && playAnimationEvent.getAnimation() != null
                && CollectionUtils.isNotEmpty(playAnimationEvent.getAnimation().getSteps())) {
            // Nettoyage de la liste des animations en cours
            animationSteps.addAll(playAnimationEvent.getAnimation().getSteps());
        }
    }

    private void loop() {
        boolean automaticMode = false;
        long nextAnimationEventTimer = System.currentTimeMillis();
        AnimationStep nextAnimationStep = null;
        while (!stopperThread) {
            //System.out.println("LOOP : " + System.currentTimeMillis());
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
                e.printStackTrace();
            }
        }
    }

    private void playAnimationStep(AnimationStep animationStep) {
        System.out.println("playAnimationStep = " + animationStep);
        // Transformation de l'étape en évènement
        MouvementYeuxEvent mouvementYeuxEvent = animationStep.buildMouvementYeuxEvent();
        MouvementCouEvent mouvementCouEvent = animationStep.buildMouvementCouEvent();
        PlaySoundEvent playSoundEvent = animationStep.buildPlaySoundEvent();

        // Envoi des évènements dans le bus
        RobotEventBus.getInstance().publishAsync(mouvementYeuxEvent);
        RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
       if (playSoundEvent != null) {
           RobotEventBus.getInstance().publishAsync(playSoundEvent);
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
            double positionOeil = RandomUtils.nextDouble(Yeux.POSITION_MINIMALE_RELATIVE_OEIL, 0);
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
        int playSound = RandomUtils.nextInt(0, 5);
        if (playSound % 5 == 0) {
            int soundIndex = RandomUtils.nextInt(0, 4);
            sound = RobotSound.values()[soundIndex];
        }

        AnimationStep animationStep = new AnimationStep(delay, positionOeilGauche, positionOeilDroit, positionCouGaucheDroite, positionCouHautBas, sound);
        return animationStep;
    }

    public static void main(String[] args) throws InterruptedException {
        Yeux yeux =  new Yeux();
        Cou cou =  new Cou();

        yeux.initialiser();
        cou.initialiser();

        RobotEventBus.getInstance().subscribe(yeux);
        RobotEventBus.getInstance().subscribe(cou);

        // Lecteur de sons
        SoundPlayer soundPlayer = new SoundPlayer();
        soundPlayer.initialiser();
        RobotEventBus.getInstance().subscribe(soundPlayer);

        // Lecteur d'animations
        AnimationPlayer animationPlayer = new AnimationPlayer();
        animationPlayer.initialiser();
        RobotEventBus.getInstance().subscribe(animationPlayer);

        Thread.sleep(2000);
        PlayAnimationEvent playAnimationEvent = new PlayAnimationEvent();
        playAnimationEvent.setAnimation(Animation.SAD);
        RobotEventBus.getInstance().publishAsync(playAnimationEvent);

        Thread.sleep(2000);

        playAnimationEvent = new PlayAnimationEvent();
        playAnimationEvent.setAnimation(Animation.AMAZED);
        RobotEventBus.getInstance().publishAsync(playAnimationEvent);

        Thread.sleep(2000);

        playAnimationEvent = new PlayAnimationEvent();
        playAnimationEvent.setAnimation(Animation.SURPRISED);
        RobotEventBus.getInstance().publishAsync(playAnimationEvent);

        Thread.sleep(2000);

//        playAnimationEvent = new PlayAnimationEvent();
//        playAnimationEvent.setAnimation(Animation.TEST);
//        RobotEventBus.getInstance().publishAsync(playAnimationEvent);
//
//        Thread.sleep(2000);

        playAnimationEvent = new PlayAnimationEvent();
        playAnimationEvent.setAnimation(Animation.TEST_2);
        RobotEventBus.getInstance().publishAsync(playAnimationEvent);

        yeux.arreter();
        cou.arreter();
        soundPlayer.arreter();
        animationPlayer.arreter();

        System.exit(0);
    }
}
