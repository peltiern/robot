package fr.roboteek.robot;

import org.apache.log4j.Logger;

import fr.roboteek.robot.decisionnel.Cerveau;
import fr.roboteek.robot.organes.actionneurs.OrganeParole3;
import fr.roboteek.robot.organes.capteurs.CapteurVision;
import fr.roboteek.robot.organes.capteurs.CapteurVocal3;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.StopEvent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;

/**
 * Classe principale du robot.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Robot {

    /** Système nerveux (bus d'évènements). */
    private MBassador<RobotEvent> systemeNerveux;

    /** Cerveau du robot. */
    private Cerveau cerveau;

    /** Tête du robot. */
//    private Tete tete;

    /** Organe de la parole. */
//    private OrganeParole organeParole;
    
    /** Organe de la parole. */
    private OrganeParole3 organeParole;

    /** Capteur de vision (oeil du robot). Thread ? */
    private CapteurVision capteurVision;

    /** Capteur vocal. */
//    private CapteurVocal capteurVocal;
    
    /** Capteur vocal. */
//    private CapteurVocal2 capteurVocal;
    
    /** Capteur vocal. */
    private CapteurVocal3 capteurVocal;

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
        
        // Instanciation du système nerveux
        systemeNerveux = new MBassador<RobotEvent>();
        
        // Instanciation des différents organes du robot
        cerveau = new Cerveau(systemeNerveux);
        
        // Actionneurs
//        tete =  new Tete(systemeNerveux);
        organeParole = new OrganeParole3(systemeNerveux);
        // Initialisation des actionneurs
//        tete.initialiser();
        organeParole.initialiser();
        
        // Abonnement aux évènements du système nerveux
        systemeNerveux.subscribe(this);
        systemeNerveux.subscribe(cerveau);
//        systemeNerveux.subscribe(tete);
        systemeNerveux.subscribe(organeParole);

        // Capteurs
        capteurVision = new CapteurVision(systemeNerveux);
        capteurVocal = new CapteurVocal3(systemeNerveux);
//        capteurVocal = new CapteurVocal2(systemeNerveux);
        
        // Initialisation des capteurs
        capteurVision.initialiser();
        capteurVocal.initialiser();
        
        systemeNerveux.subscribe(capteurVocal);
        
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte("J'ai terminé de m'initialiser");
        systemeNerveux.publish(paroleEvent);
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
                systemeNerveux.unsubscribe(capteurVocal);
                logger.debug("arrêt du robot 1");
                systemeNerveux.unsubscribe(organeParole);
                logger.debug("arrêt du robot 2");
//                systemeNerveux.unsubscribe(tete);
                logger.debug("arrêt du robot 3");
                systemeNerveux.unsubscribe(cerveau);
                logger.debug("arrêt du robot 4");
                systemeNerveux.unsubscribe(Robot.this);
                logger.debug("arrêt du robot 5");
                
                // Arrêt des organes
                capteurVocal.arreter();
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
