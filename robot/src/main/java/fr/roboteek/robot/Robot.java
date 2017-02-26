package fr.roboteek.robot;

import org.apache.log4j.Logger;

import fr.roboteek.robot.decisionnel.Cerveau;
import fr.roboteek.robot.organes.actionneurs.OrganeParoleEspeak;
import fr.roboteek.robot.organes.actionneurs.Tete;
import fr.roboteek.robot.organes.actionneurs.Visage;
import fr.roboteek.robot.organes.actionneurs.VisageDoubleBuffering;
import fr.roboteek.robot.organes.capteurs.CapteurVision;
import fr.roboteek.robot.organes.capteurs.CapteurVocalGoogle;
import fr.roboteek.robot.server.RobotServer;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import net.engio.mbassy.listener.Handler;

/**
 * Classe principale du robot.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Robot {

    /** Cerveau du robot. */
    private Cerveau cerveau;

    /** Tête du robot. */
//    private Tete tete;
    
    /** Visage. */
    private VisageDoubleBuffering visage;

    /** Organe de la parole. */
//    private OrganeParole organeParole;
    
    /** Organe de la parole. */
//    private OrganeParole3 organeParole;
    
    /** Organe de la parole. */
    private OrganeParoleEspeak organeParole;

    /** Capteur de vision (oeil du robot). Thread ? */
    private CapteurVision capteurVision;

    /** Capteur vocal. */
//    private CapteurVocal capteurVocal;
    
    /** Capteur vocal. */
//    private CapteurVocal2 capteurVocal;
    
    /** Capteur vocal. */
    //private CapteurVocal3 capteurVocal;
    
    /** Capteur vocal. */
    private CapteurVocalGoogle capteurVocal;

    /** Flag d'arrêt du robot. */
    private boolean stopper = false;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(Robot.class);


    public Robot() {
        // Initialisation
        initialiser();
        while(!stopper) {}
        logger.debug("Arrêt complet");
        System.out.println("ARRET");
        System.exit(0);
    }

    /** Initialisation du robot. */
    private void initialiser() {
        logger.debug("Initialisation du robot");
        
        System.setProperty("robot.dir", "/home/npeltier/Robot/Programme");
        
        // Instanciation des différents organes du robot
        cerveau = new Cerveau();
        
        // Actionneurs
//        tete =  new Tete();
        organeParole = new OrganeParoleEspeak();
//        visage = VisageDoubleBuffering.getInstance();
        // Initialisation des actionneurs
//        tete.initialiser();
        organeParole.initialiser();
//        visage.initialiser();
        
        
        // Abonnement aux évènements du système nerveux
        RobotEventBus.getInstance().subscribe(this);
        RobotEventBus.getInstance().subscribe(cerveau);
//        RobotEventBus.getInstance().subscribe(tete);
        RobotEventBus.getInstance().subscribe(organeParole);
//        RobotEventBus.getInstance().subscribe(visage);

        // Capteurs
        capteurVision = new CapteurVision();
        capteurVocal = new CapteurVocalGoogle();
//        capteurVocal = new CapteurVocal2(systemeNerveux);
        
        // Initialisation des capteurs
        capteurVision.initialiser();
        capteurVocal.initialiser();
        
        RobotEventBus.getInstance().subscribe(capteurVocal);
        
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
//                systemeNerveux.unsubscribe(tete);
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
