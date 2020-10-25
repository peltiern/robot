package fr.roboteek.robot;

import fr.roboteek.robot.decisionnel.Cerveau;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.ConduiteDifferentielle;
import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleGoogle;
import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import fr.roboteek.robot.organes.actionneurs.animation.AnimationPlayer;
import fr.roboteek.robot.organes.capteurs.CapteurActiviteSon;
import fr.roboteek.robot.organes.capteurs.CapteurVisionWebSocket;
import fr.roboteek.robot.organes.capteurs.CapteurVocalSimple;
import fr.roboteek.robot.server.RobotServer;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import fr.roboteek.robot.util.gamepad.jamepad.RobotJamepadController;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;
import net.engio.mbassy.listener.Handler;
import org.apache.log4j.Logger;

/**
 * Classe principale du robot.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Robot {

    /** Cerveau du robot. */
    private Cerveau cerveau;

    /** Organe de la parole. */
    private AbstractOrgane organeParole;

    /** Capteur de vision (oeil du robot). Thread ? */
    private AbstractOrgane capteurVision;

    /** Capteur vocal. */
    private AbstractOrgane capteurVocal;

    /** Capteur Activité Son. */
    private CapteurActiviteSon capteurActiviteSon;

    /** Cou du robot. */
    private Cou cou;

    /** Yeux du robot. */
    private Yeux yeux;

    /** Conduite différentielle. */
    private ConduiteDifferentielle conduiteDifferentielle;

    /** Controleur de manette. */
    private RobotGamepadController robotGamepadController;

    /** Lecteur de sons. */
    private SoundPlayer soundPlayer;

    /** Lecteur d'animations. */
    private AnimationPlayer animationPlayer;

    /** Flag d'arrêt du robot. */
    private boolean stopper = false;

    /** Logger. */
    private Logger logger = Logger.getLogger(Robot.class);


    public Robot() {
        // Initialisation
        initialiser();
        while(!stopper) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug("Arrêt complet");
        System.out.println("ARRET");
        System.exit(0);
    }

    /** Initialisation du robot. */
    private void initialiser() {
        logger.debug("Initialisation du robot");

        // Instanciation des différents organes du robot
        cerveau = new Cerveau();

        // Actionneurs
        organeParole = new OrganeParoleGoogle();

        // Initialisation des actionneurs
        organeParole.initialiser();


        // Abonnement aux évènements du système nerveux
        RobotEventBus.getInstance().subscribe(this);
        RobotEventBus.getInstance().subscribe(cerveau);
        RobotEventBus.getInstance().subscribe(organeParole);

        // Capteurs
        capteurVision = new CapteurVisionWebSocket();
        capteurVocal = new CapteurVocalSimple();

        // Initialisation des capteurs
        capteurVision.initialiser();
        capteurVocal.initialiser();

        RobotEventBus.getInstance().subscribe(capteurVocal);

        // Lecteur de sons
        soundPlayer = new SoundPlayer();
        soundPlayer.initialiser();
        RobotEventBus.getInstance().subscribe(soundPlayer);

        // Lecteur d'animations
        animationPlayer = new AnimationPlayer();
        animationPlayer.initialiser();
        RobotEventBus.getInstance().subscribe(animationPlayer);

        // Manette
        robotGamepadController = new RobotJamepadController();
        robotGamepadController.start();

        yeux = new Yeux();
        cou =  new Cou();
        conduiteDifferentielle = new ConduiteDifferentielle();
        cou.initialiser();
        yeux.initialiser();
        conduiteDifferentielle.initialiser();
        RobotEventBus.getInstance().subscribe(yeux);
        RobotEventBus.getInstance().subscribe(cou);
        RobotEventBus.getInstance().subscribe(conduiteDifferentielle);

        // Démarrage du serveur
        RobotServer.getInstance().run();

        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte("J'ai terminé de m'initialiser");
        RobotEventBus.getInstance().publishAsync(paroleEvent);
        logger.debug("Fin de l'initialisation");
    }

    /** Arrête le robot. */
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
     * @param stopEvent évènement pour stopper le robot
     */
    @Handler
    public void handleStopEvent(StopEvent stopEvent) {
        arreter();
    }

    public static void main(String args[]) {
        new Robot();
        System.exit(0);
    }

}
