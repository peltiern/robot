package fr.roboteek.robot;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.decisionnel.Cerveau;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.organes.actionneurs.*;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationPlayer;
import fr.roboteek.robot.organes.capteurs.CapteurActiviteSon;
import fr.roboteek.robot.organes.capteurs.CapteurVisionWebSocketRest;
import fr.roboteek.robot.organes.capteurs.CapteurVocalAvecReconnaissance;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import fr.roboteek.robot.systemenerveux.event.persistance.RobotEventPersistance;
import fr.roboteek.robot.util.gamepad.jamepad.RobotJamepadController;
import fr.roboteek.robot.util.gamepad.jinput.RobotLogitechController;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principale du robot.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Robot {

    /**
     * Cerveau du robot.
     */
    private Cerveau cerveau;

    /**
     * Organe de la parole.
     */
    private AbstractOrgane organeParole;

    /**
     * Capteur de vision (oeil du robot). Thread ?
     */
    private AbstractOrganeWithThread capteurVision;

    /**
     * Capteur vocal.
     */
    private AbstractOrganeWithThread capteurVocal;

    /**
     * Capteur Activité Son.
     */
    private CapteurActiviteSon capteurActiviteSon;

    /**
     * Cou du robot.
     */
    private Cou cou;

    /**
     * Yeux du robot.
     */
    private Yeux yeux;

    /**
     * Conduite différentielle.
     */
    private ConduiteDifferentielle conduiteDifferentielle;

    /**
     * Controleur de manette.
     */
    private RobotGamepadController robotGamepadController;

    /**
     * Lecteur de sons.
     */
    private SoundPlayer soundPlayer;

    /**
     * Lecteur d'animations.
     */
    private AnimationPlayer animationPlayer;

    /**
     * Robot events interceptor to persist events.
     */
    private RobotEventPersistance robotEventPersistance;

    /**
     * Flag d'arrêt du robot.
     */
    private boolean stopper = false;

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(Robot.class);


    public Robot() {
        // Initialisation
        initialiser();
        while (!stopper) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug("Arrêt complet test");
        System.out.println("ARRET");
        System.exit(0);
    }

    /**
     * Initialisation du robot.
     */
    private void initialiser() {
        logger.debug("Initialisation du robot");

        // Instanciation des différents organes du robot
        cerveau = new Cerveau();
        cerveau.initialiser();

        // Actionneurs
        organeParole = new OrganeParoleGoogle();

        // Initialisation des actionneurs
        organeParole.initialiser();


        // Abonnement aux évènements du système nerveux
        RobotEventBus.getInstance().subscribe(this);
        RobotEventBus.getInstance().subscribe(cerveau);
        RobotEventBus.getInstance().subscribe(organeParole);

        System.out.println("#####      1      ########");

        // Capteurs
        capteurVision = new CapteurVisionWebSocketRest();
        capteurVocal = new CapteurVocalAvecReconnaissance();

        // Initialisation des capteurs
        capteurVision.initialiser();
        capteurVision.start();
        System.out.println("#####      2      ########");
        capteurVocal.initialiser();
        capteurVocal.start();
//
        RobotEventBus.getInstance().subscribe(capteurVocal);
        System.out.println("#####      3      ########");
//
        // Lecteur de sons
        soundPlayer = new SoundPlayer();
        soundPlayer.initialiser();
        RobotEventBus.getInstance().subscribe(soundPlayer);
        System.out.println("#####      4      ########");

        // Robot events persistance
        robotEventPersistance = new RobotEventPersistance();
        RobotEventBus.getInstance().subscribe(robotEventPersistance);
        System.out.println("#####      5      ########");

        // Lecteur d'animations
        animationPlayer = new AnimationPlayer();
        animationPlayer.initialiser();
        animationPlayer.start();
        RobotEventBus.getInstance().subscribe(animationPlayer);
        System.out.println("#####      6      ########");

        // Manette
        robotGamepadController = new RobotLogitechController();
//        robotGamepadController = new RobotJamepadController();
        robotGamepadController.start();
        System.out.println("#####      7      ########");

        yeux = new Yeux();
        System.out.println("#####      8      ########");
        cou =  new Cou();
        System.out.println("#####      9      ########");
        conduiteDifferentielle = new ConduiteDifferentielle();
        System.out.println("#####      10      ########");
        cou.initialiser();
        System.out.println("#####      11      ########");
        yeux.initialiser();
        System.out.println("#####      12      ########");
        conduiteDifferentielle.initialiser();
        RobotEventBus.getInstance().subscribe(yeux);
        RobotEventBus.getInstance().subscribe(cou);
        RobotEventBus.getInstance().subscribe(conduiteDifferentielle);
        System.out.println("#####      13      ########");

        cerveau.start();
        System.out.println("#####      14      ########");

        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte("J'ai terminé de m'initialiser");
        RobotEventBus.getInstance().publishAsync(paroleEvent);
        logger.debug("Fin de l'initialisation");
        System.out.println("#####      15      ########");
    }

    /**
     * Arrête le robot.
     */
    private void arreter() {
        // TODO NP : Revoir l'arrêt correct de l'ensemble des threads
        // Lancement d'un Thread pour l'arrêt du robot
        final Thread threadArret = new Thread(() -> {
            logger.debug("Début de l'arrêt du robot");

            // Désabonnement des organes au système nerveux
            RobotEventBus.getInstance().unsubscribe(yeux);
            RobotEventBus.getInstance().unsubscribe(cou);
            RobotEventBus.getInstance().unsubscribe(conduiteDifferentielle);
            RobotEventBus.getInstance().unsubscribe(animationPlayer);
            RobotEventBus.getInstance().unsubscribe(soundPlayer);
            RobotEventBus.getInstance().unsubscribe(capteurVocal);
            RobotEventBus.getInstance().unsubscribe(capteurVision);
            RobotEventBus.getInstance().unsubscribe(organeParole);
            RobotEventBus.getInstance().unsubscribe(cerveau);
            RobotEventBus.getInstance().unsubscribe(Robot.this);

            // Arrêt des organes
            capteurVision.arreter();
            organeParole.arreter();

            logger.debug("Fin de l'arrêt du robot");

            stopper = true;
        });
        threadArret.start();
    }

    /**
     * Intercepte les évènements pour stopper le robot.
     *
     * @param stopEvent évènement pour stopper le robot
     */
    @Subscribe
    public void handleStopEvent(StopEvent stopEvent) {
        arreter();
    }

    public static void main(String args[]) {
        new Robot();
        System.exit(0);
    }

}
