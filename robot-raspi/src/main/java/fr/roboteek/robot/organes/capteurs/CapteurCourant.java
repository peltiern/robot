package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.util.phidgets.PhidgetCurrentCaptor;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Capteur regroupant l'ensemble des capteurs de courant (moteurs, ...).
 */
public class CapteurCourant extends AbstractOrgane {

    private static final String nomMoteurGauche = "MOTEUR_GAUCHE";

    private static final String nomMoteurDroit = "MOTEUR_DROIT";

    private PhidgetCurrentCaptor capteurCourantMoteurGauche;

    private PhidgetCurrentCaptor capteurCourantMoteurDroit;

    /**
     * Phidgets configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    public CapteurCourant() {
        super();

        phidgetsConfig = phidgetsConfig();

        // Création et initialisation des capteurs
        capteurCourantMoteurGauche = new PhidgetCurrentCaptor(phidgetsConfig().hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), nomMoteurGauche);
        capteurCourantMoteurDroit = new PhidgetCurrentCaptor(phidgetsConfig().hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), nomMoteurDroit);
    }

    @Override
    public void initialiser() {

    }

    @Override
    public void arreter() {

    }
}
