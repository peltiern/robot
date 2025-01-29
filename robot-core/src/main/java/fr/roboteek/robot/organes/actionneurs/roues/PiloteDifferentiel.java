package fr.roboteek.robot.organes.actionneurs.roues;

import com.google.common.eventbus.Subscribe;
import com.phidget22.EncoderPositionChangeEvent;
import com.phidget22.EncoderPositionChangeListener;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.EncodeurRoueEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementRoueEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.gamepad.jinput.RobotLogitechController;
import fr.roboteek.robot.util.phidgets.PhidgetDCMotor;
import lombok.Getter;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;
import static fr.roboteek.robot.configuration.Configurations.robotConfig;

public class PiloteDifferentiel extends AbstractOrgane implements EncoderPositionChangeListener {

    /** Chassis à piloter. */
    @Getter
    private final Chassis chassis;

    private final PhidgetsConfig phidgetsConfig;

    /**
     * Constructeur.
     */
    public PiloteDifferentiel() {
        super();

        phidgetsConfig = phidgetsConfig();
        RobotConfig robotConfig = robotConfig();

        // Création et initialisation des moteurs
        PhidgetDCMotor moteurGauche = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());
        PhidgetDCMotor moteurDroit = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingRightMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());

        chassis = Chassis.builder()
                .moteurGauche(moteurGauche)
                .moteurDroit(moteurDroit)
                .largeurRoues(robotConfig().distanceRoues())
                .diametreRoue(robotConfig.diametreRoue())
                .rapportTransmission(robotConfig.rapportTransmission())
                .ticksParRotation(robotConfig().ticksParRotation())
                .build();

        moteurDroit.getEncodeur().addPositionChangeListener(this);

    }

    public void avancer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        System.out.println("vitesseFormatee = " + vitesseFormatee + ", accelerationFormatee = " + accelerationFormatee);
        chassis.getMoteurGauche().forward(vitesseFormatee, accelerationFormatee);
        chassis.getMoteurDroit().forward(vitesseFormatee, accelerationFormatee);
    }

    public void reculer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        chassis.getMoteurGauche().forward(-vitesseFormatee, accelerationFormatee);
        chassis.getMoteurDroit().forward(-vitesseFormatee, accelerationFormatee);
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
        chassis.getMoteurGauche().forward(vitesseFormatee, accelerationFormatee);
    }

    public void tournerRoueDroite(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        chassis.getMoteurDroit().forward(vitesseFormatee, accelerationFormatee);
    }

    public void stop() {
        chassis.getMoteurGauche().stop();
        chassis.getMoteurDroit().stop();
    }

    /**
     * Intercepte les évènements de mouvements.
     *
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
        chassis.getMoteurGauche().close();
        chassis.getMoteurDroit().close();
    }

    @Override
    public void onPositionChange(EncoderPositionChangeEvent encoderPositionChangeEvent) {
        if (encoderPositionChangeEvent.getSource() == chassis.getMoteurDroit().getEncodeur()) {
            EncodeurRoueEvent event = new EncodeurRoueEvent(-chassis.getMoteurGauche().getPositionEncodeur(), chassis.getMoteurDroit().getPositionEncodeur());
            RobotEventBus.getInstance().publishAsync(event);
        }
    }

    /**
     * Arrête les moteurs.
     */
    private void reset() {
        chassis.getMoteurGauche().stop();
        chassis.getMoteurDroit().stop();
    }

    private double toVitesse(Double vitesse) {
        return (vitesse == null ? 1 : Math.abs(vitesse) > 1 ? (int) vitesse.intValue() : vitesse) * phidgetsConfig.differentialDrivingMotorMaxSpeed();
    }

    private double toAcceleration(Double acceleration) {
        return acceleration == null || acceleration < 0.1 || acceleration > 100 ? phidgetsConfig.differentialDrivingMotorAcceleration() : acceleration;
    }

    public static void main(String[] args) {
        PiloteDifferentiel piloteDifferentiel = new PiloteDifferentiel();
        piloteDifferentiel.initialiser();
        RobotEventBus.getInstance().subscribe(piloteDifferentiel);
        RobotLogitechController robotLogitechController = new RobotLogitechController();
        robotLogitechController.start();
    }
}
