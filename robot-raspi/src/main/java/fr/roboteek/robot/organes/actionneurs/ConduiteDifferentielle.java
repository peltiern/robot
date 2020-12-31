package fr.roboteek.robot.organes.actionneurs;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.MouvementRoueEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.gamepad.jamepad.RobotJamepadController;
import fr.roboteek.robot.util.phidgets.PhidgetDCMotor;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

public class ConduiteDifferentielle extends AbstractOrgane {

    /**
     * Moteur pour la roue gauche.
     */
    private PhidgetDCMotor moteurGauche;

    /**
     * Moteur pour la roue droite.
     */
    private PhidgetDCMotor moteurDroit;

    /** Phidgets configuration. */
    private PhidgetsConfig phidgetsConfig;

    /**
     * Constructeur.
     */
    public ConduiteDifferentielle() {
        super();

        phidgetsConfig = phidgetsConfig();

        // Création et initialisation des moteurs
        moteurGauche = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());
        moteurDroit = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingRightMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());

    }

    public void avancer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        System.out.println("vitesseFormatee = " + vitesseFormatee + ", accelerationFormatee = " + accelerationFormatee);
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

    public void stop() {
        moteurGauche.stop();
        moteurDroit.stop();
    }

    /**
     * Intercepte les évènements de mouvements.
     * @param mouvementRoueEvent évènement de mouvements
     */
    @Subscribe
    public void handleMouvementRoueEvent(MouvementRoueEvent mouvementRoueEvent) {
        System.out.println(mouvementRoueEvent);
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
            stop();
        }
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

    /** Arrête les moteurs. */
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

    public static void main(String[] args) {
        ConduiteDifferentielle conduiteDifferentielle = new ConduiteDifferentielle();
        conduiteDifferentielle.initialiser();
        RobotEventBus.getInstance().subscribe(conduiteDifferentielle);
        RobotJamepadController robotGamepadController = new RobotJamepadController();
        robotGamepadController.start();
    }
}
