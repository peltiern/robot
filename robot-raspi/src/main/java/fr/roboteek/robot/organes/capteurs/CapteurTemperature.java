package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.util.phidgets.PhidgetTemperatureCaptor;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Capteur regroupant l'ensemble des capteurs de température (moteurs, ...).
 */
public class CapteurTemperature extends AbstractOrgane {

    private static final String nomMoteurGauche = "MOTEUR_GAUCHE";

    private static final String nomMoteurDroit = "MOTEUR_DROIT";

    private PhidgetTemperatureCaptor capteurTemperatureMoteurGauche;

    private PhidgetTemperatureCaptor capteurTemperatureMoteurDroit;

    /**
     * Phidgets configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    public CapteurTemperature() {
        super();

        phidgetsConfig = phidgetsConfig();

        // Création et initialisation des capteurs
        capteurTemperatureMoteurGauche = new PhidgetTemperatureCaptor(phidgetsConfig().hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), nomMoteurGauche);
        capteurTemperatureMoteurDroit = new PhidgetTemperatureCaptor(phidgetsConfig().hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), nomMoteurDroit);
    }

    @Override
    public void initialiser() {

    }

    @Override
    public void arreter() {

    }
}
