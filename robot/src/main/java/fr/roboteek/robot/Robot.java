package fr.roboteek.robot;

import fr.roboteek.robot.organes.actionneurs.ConduiteDifferentielle;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleGoogle;
import fr.roboteek.robot.organes.capteurs.CapteurActiviteSon;
import fr.roboteek.robot.organes.capteurs.CapteurVocalSimple;
import fr.roboteek.robot.util.gamepad.jamepad.RobotJamepadController;
import fr.roboteek.robot.util.gamepad.jinput.RobotJinputController;
import fr.roboteek.robot.util.gamepad.shared.RobotGamepadController;
import org.apache.log4j.Logger;

import fr.roboteek.robot.decisionnel.Cerveau;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleEspeak;
import fr.roboteek.robot.organes.actionneurs.VisageDoubleBuffering;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import fr.roboteek.robot.organes.capteurs.CapteurVocalWebService;
import fr.roboteek.robot.server.RobotServer;
import fr.roboteek.robot.server.test.CapteurVisionWebSocket;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import fr.roboteek.robot.util.reconnaissance.vocale.bing.BingSpeechRecognizerRest;
import net.engio.mbassy.listener.Handler;

/**
 * Classe principale du robot.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Robot {

    /** Cerveau du robot. */
    private Cerveau cerveau;

    /** Tête du robot. */
    //private Tete tete;
    
    /** Cou du robot. */
    private Cou cou;
    
    /** Yeux du robot. */
    private Yeux yeux;

    /** Conduite différentielle. */
    private ConduiteDifferentielle conduiteDifferentielle;
    
    /** Visage. */
    private VisageDoubleBuffering visage;
    
    /** Organe de la parole. */
//    private OrganeParoleEspeak organeParole;

    /** Organe de la parole. */
    private OrganeParoleGoogle organeParole;

    /** Capteur de vision (oeil du robot). Thread ? */
    private AbstractOrgane capteurVision;
    
    /** Capteur vocal. */
    private AbstractOrgane capteurVocal;

    /** Capteur Activité Son. */
    private CapteurActiviteSon capteurActiviteSon;

    /** Controleur de manette. */
    private RobotGamepadController robotGamepadController;

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
        System.out.println("LIBRARY PATH = " + System.getProperty("java.library.path"));
        
        // Instanciation des différents organes du robot
        cerveau = new Cerveau();
        
        // Actionneurs
        //tete =  new Tete();
//        yeux = new Yeux();
//        cou =  new Cou();
//        conduiteDifferentielle = new ConduiteDifferentielle();
//        organeParole = new OrganeParoleEspeak();
        organeParole = new OrganeParoleGoogle();
//        visage = VisageDoubleBuffering.getInstance();
        // Initialisation des actionneurs
//        tete.initialiser();
//        cou.initialiser();
//        yeux.initialiser();
//        conduiteDifferentielle.initialiser();
        organeParole.initialiser();
//        visage.initialiser();
        
        
        // Abonnement aux évènements du système nerveux
        RobotEventBus.getInstance().subscribe(this);
        RobotEventBus.getInstance().subscribe(cerveau);
//        RobotEventBus.getInstance().subscribe(tete);
//        RobotEventBus.getInstance().subscribe(yeux);
//        RobotEventBus.getInstance().subscribe(cou);
//        RobotEventBus.getInstance().subscribe(conduiteDifferentielle);
        RobotEventBus.getInstance().subscribe(organeParole);
//        RobotEventBus.getInstance().subscribe(visage);

        // Capteurs
        capteurVision = new CapteurVisionWebSocket();
//        capteurVocal = new CapteurVocalSimple();
        capteurVocal = new CapteurVocalWebService(BingSpeechRecognizerRest.getInstance());
//        capteurVocal = new CapteurVocal2(systemeNerveux);
//        capteurActiviteSon = new CapteurActiviteSon();
        
        // Initialisation des capteurs
        capteurVision.initialiser();
        capteurVocal.initialiser();
//        capteurActiviteSon.initialiser();
        
        RobotEventBus.getInstance().subscribe(capteurVocal);

        // Manette
        //String currentLibraryPath = System.getProperty("java.library.path");
        //System.setProperty("java.library.path", currentLibraryPath + ":/home/npeltier/Developpement/robot/robot-sandbox/target/natives");
        //robotGamepadController = new RobotJinputController();
//        robotGamepadController = new RobotJamepadController();
//        robotGamepadController.start();
        
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
        final Thread threadArret = new Thread() {
            @Override
            public void run() {
                logger.debug("Début de l'arrêt du robot");
                
                // Désabonnement des organes au système nerveux
                RobotEventBus.getInstance().unsubscribe(visage);
                RobotEventBus.getInstance().unsubscribe(capteurVocal);
                logger.debug("arrêt du robot 1");
                RobotEventBus.getInstance().unsubscribe(organeParole);
                logger.debug("arrêt du robot 2");
//                RobotEventBus.getInstance().unsubscribe(tete);
                RobotEventBus.getInstance().unsubscribe(cou);
                RobotEventBus.getInstance().unsubscribe(yeux);
                RobotEventBus.getInstance().unsubscribe(conduiteDifferentielle);
                logger.debug("arrêt du robot 3");
                RobotEventBus.getInstance().unsubscribe(cerveau);
                logger.debug("arrêt du robot 4");
                RobotEventBus.getInstance().unsubscribe(Robot.this);
                logger.debug("arrêt du robot 5");
                
                // Arrêt des organes
//                capteurVocal.arreter();
                logger.debug("arrêt du robot 6");
                capteurVision.arreter();
                logger.debug("arrêt du robot 7");
                organeParole.arreter();
                logger.debug("arrêt du robot 8");
//                tete.arreter();
                cou.arreter();
                logger.debug("arrêt du robot 9");
                cerveau.arreter();
                
                logger.debug("Fin de l'arrêt du robot");

                stopper = true;
            }
        };
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
