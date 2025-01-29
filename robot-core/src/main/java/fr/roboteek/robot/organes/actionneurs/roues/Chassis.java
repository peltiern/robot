package fr.roboteek.robot.organes.actionneurs.roues;

import fr.roboteek.robot.util.phidgets.PhidgetDCMotor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Chassis {

    /**
     * Moteur pour la roue gauche.
     */
    private PhidgetDCMotor moteurGauche;

    /**
     * Moteur pour la roue droite.
     */
    private PhidgetDCMotor moteurDroit;

    /**
     * Distance entre les roues.
     */
    private int largeurRoues;

    /** Diamètre des roues (en mm). */
    private int diametreRoue;

    /** Rapport de transmission. */
    private float rapportTransmission;

    /** Nombre de ticks par rotation de roue. */
    private int ticksParRotation;

    /**
     * Convertitun nombre de ticks en distance (en mm)
     * @param deltaTicks le nombre de ticks à convertir
     * @return la distance (en mm)
     */
    public long ticksEnDistance(long deltaTicks) {
        // Calcul de la distance parcourue par la roue (ajout du rapport de transmission)
        double distanceParcourue = (deltaTicks / (double) ticksParRotation) * (Math.PI * diametreRoue);
        return (long) (distanceParcourue * rapportTransmission);
    }

    public double calculerAngle(long deltaTicksGauche, long deltaTicksDroite) {
        // Calcul de l'angle tourné en fonction des ticks des roues
        double distanceGauche = ticksEnDistance(deltaTicksGauche);
        double distanceDroite = ticksEnDistance(deltaTicksDroite);
        return (distanceDroite - distanceGauche) / largeurRoues;
    }
}
