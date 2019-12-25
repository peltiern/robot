package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.MouvementRoueEvent;
import fr.roboteek.robot.util.phidget.PhidgetDCMotor;
import net.engio.mbassy.listener.Handler;

public class ConduiteDifferentielle extends AbstractOrgane {

    /**
     * Numéro de série du Hub VINT.
     */
    private static final int SERIAL_NUMBER_VINT_HUB = 561050;

    /**
     * Port du hub pour le moteur gauche.
     */
    private static final int PORT_MOTEUR_GAUCHE = 0;

    /**
     * Port du hub pour le moteur droit.
     */
    private static final int PORT_MOTEUR_DROIT = 1;

    /**
     * Vitesse maximale des moteurs.
     */
    private static final double VITESSE_MAX = 0.5;

    /**
     * Vitesse par défaut.
     */
    public static final double VITESSE_PAR_DEFAUT = 1;

    /**
     * Accélération par défaut.
     */
    public static final double ACCELERATION_PAR_DEFAUT = 1;

    /**
     * Moteur pour la roue gauche.
     */
    private PhidgetDCMotor moteurGauche;

    /**
     * Moteur pour la roue droite.
     */
    private PhidgetDCMotor moteurDroit;

    /**
     * Constructeur.
     */
    public ConduiteDifferentielle() {
        super();

        // Création et initialisation des moteurs
        moteurGauche = new PhidgetDCMotor(SERIAL_NUMBER_VINT_HUB, PORT_MOTEUR_GAUCHE, ACCELERATION_PAR_DEFAUT);
        moteurDroit = new PhidgetDCMotor(SERIAL_NUMBER_VINT_HUB, PORT_MOTEUR_DROIT, ACCELERATION_PAR_DEFAUT);

    }

    public void avancer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
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
    @Handler
    public void handleMouvementRoueEvent(MouvementRoueEvent mouvementRoueEvent) {
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
        return (vitesse == null ? 1 : Math.abs(vitesse) > 1 ? (int) vitesse.intValue() : vitesse) * VITESSE_MAX;
    }

    private double toAcceleration(Double acceleration) {
        return acceleration == null || acceleration < 0.1 || acceleration > 100 ? ACCELERATION_PAR_DEFAUT : acceleration;
    }
}
