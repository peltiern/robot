package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementRoueEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.phidgets.PhidgetDCMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Conduite différentielle (chenilles) du robot.
 * <p>
 * Migré en bean Spring : cycle de vie géré par {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#ACTIONNEURS_AVEC_MOTEUR}. Les moteurs ne sont créés
 * et engagés qu'au {@code start()} — pas à la construction du bean — pour respecter
 * l'ordre des phases (moteurs derniers démarrés, premiers arrêtés).
 */
@Component
public class ConduiteDifferentielle extends AbstractOrgane implements SmartLifecycle {

    /**
     * Moteur pour la roue gauche.
     */
    private PhidgetDCMotor moteurGauche;

    /**
     * Moteur pour la roue droite.
     */
    private PhidgetDCMotor moteurDroit;

    /**
     * Phidgets configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ConduiteDifferentielle.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Arrêt d'urgence en cours : tout ordre de mouvement est refusé jusqu'au réarmement
     * (cf. {@link fr.roboteek.robot.securite.ArretUrgence}).
     */
    private volatile boolean arretUrgence = false;

    /**
     * Constructeur.
     */
    public ConduiteDifferentielle() {
        super();
        phidgetsConfig = phidgetsConfig();
    }

    public void avancer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        logger.debug("vitesseFormatee = {}, accelerationFormatee = {}", vitesseFormatee, accelerationFormatee);
        moteurGauche.forward(vitesseFormatee, accelerationFormatee);
        moteurDroit.forward(vitesseFormatee, accelerationFormatee);
    }

    public void reculer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        moteurGauche.forward(-vitesseFormatee, accelerationFormatee);
        moteurDroit.forward(-vitesseFormatee, accelerationFormatee);
    }

    public void pivoterAGauche(Double vitesse, Double acceleration) {
        tournerRoueGauche(-vitesse, acceleration);
        tournerRoueDroite(vitesse, acceleration);
    }

    public void pivoterADroite(Double vitesse, Double acceleration) {
        tournerRoueGauche(vitesse, acceleration);
        tournerRoueDroite(-vitesse, acceleration);
    }

    public void differentiel(Double vitesseRoueGauche, Double accelerationRoueGauche, Double vitesseRoueDroite, Double accelerationRoueDroite) {
        tournerRoueGauche(vitesseRoueGauche, accelerationRoueGauche);
        tournerRoueDroite(vitesseRoueDroite, accelerationRoueDroite);
    }

    public void tournerRoueGauche(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        moteurGauche.forward(vitesseFormatee, accelerationFormatee);
    }

    public void tournerRoueDroite(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        moteurDroit.forward(vitesseFormatee, accelerationFormatee);
    }

    public void stopperRoues() {
        moteurGauche.stop();
        moteurDroit.stop();
    }

    /**
     * Intercepte les évènements de mouvements.
     *
     * @param mouvementRoueEvent évènement de mouvements
     */
    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleMouvementRoueEvent(MouvementRoueEvent mouvementRoueEvent) {
        if (!running || arretUrgence) {
            return;
        }
        logger.debug("{}", mouvementRoueEvent);
        if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.AVANCER) {
            avancer(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.RECULER) {
            reculer(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_GAUCHE) {
            pivoterAGauche(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_DROIT) {
            pivoterADroite(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.DIFFERENTIEL) {
            differentiel(mouvementRoueEvent.getVitesseRoueGauche(), mouvementRoueEvent.getAccelerationRoueGauche(),
                    mouvementRoueEvent.getVitesseRoueDroite(), mouvementRoueEvent.getAccelerationRoueDroite());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER) {
            stopperRoues();
        }
    }

    /**
     * Arrêt d'urgence : coupe les deux moteurs de chenilles sur-le-champ. Voir
     * {@code Cou.handleArretUrgenceEvent} pour le pourquoi du traitement synchrone.
     * <p>
     * C'est l'organe le plus concerné : un robot qui roule est la seule chose ici qui puisse
     * partir loin toute seule.
     */
    @EventListener
    public void handleArretUrgenceEvent(ArretUrgenceEvent evenement) {
        arretUrgence = evenement.isActif();
        if (!running || !evenement.isActif()) {
            return;
        }
        logger.warn("ConduiteDifferentielle : arrêt d'urgence, coupure des moteurs");
        stopperRoues();
    }

    @Override
    public void initialiser() {
        reset();
    }

    @Override
    public void arreter() {
        reset();
        moteurGauche.close();
        moteurDroit.close();
    }

    /**
     * Arrête les moteurs.
     */
    private void reset() {
        moteurGauche.stop();
        moteurDroit.stop();
    }

    private double toVitesse(Double vitesse) {
        return (vitesse == null ? 1 : Math.abs(vitesse) > 1 ? (int) vitesse.intValue() : vitesse) * phidgetsConfig.differentialDrivingMotorMaxSpeed();
    }

    private double toAcceleration(Double acceleration) {
        return acceleration == null || acceleration < 0.1 || acceleration > 100 ? phidgetsConfig.differentialDrivingMotorAcceleration() : acceleration;
    }

    @Override
    public void start() {
        // Création des moteurs au démarrage de la phase (et non à la construction du bean)
        moteurGauche = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());
        moteurDroit = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingRightMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());
        initialiser();
        running = true;
        logger.info("ConduiteDifferentielle démarrée");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("ConduiteDifferentielle arrêtée");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.ACTIONNEURS_AVEC_MOTEUR;
    }
}
