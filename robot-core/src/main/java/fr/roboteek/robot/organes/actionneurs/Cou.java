package fr.roboteek.robot.organes.actionneurs;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_MONTER_DESCENDRE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_PANORAMIQUE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_INCLINAISON;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.phidgets.PhidgetsServoMotor;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Classe représentant le cou du robot.
 *
 * @author Java Developer
 */
public class Cou extends AbstractOrgane {

    /**
     * Moteur "Panoramique".
     */
    private PhidgetsServoMotor moteurPanoramique;

    /**
     * Moteur "Inclinaison".
     */
    private PhidgetsServoMotor moteurInclinaison;

    /**
     * Moteur "Monter - Descendre".
     */
    private PhidgetsServoMotor moteurMonterDescendre;

    /**
     * Phidgets configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    private MOUVEMENTS_PANORAMIQUE mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
    private MOUVEMENTS_INCLINAISON mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
    private MOUVEMENTS_MONTER_DESCENDRE mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;

    /**
     * Constructeur.
     */
    public Cou() {
        super();

        phidgetsConfig = phidgetsConfig();

        System.out.println("COU :, Thread = " + Thread.currentThread().getName());

        // Création et initialisation des moteurs
        moteurPanoramique = new PhidgetsServoMotor(
                phidgetsConfig.neckLeftRightMotorIndex(),
                phidgetsConfig.neckLeftRightMotorInitialPosition(),
                phidgetsConfig.neckLeftRightMotorMinPosition(),
                phidgetsConfig.neckLeftRightMotorMaxPosition(),
                phidgetsConfig.neckLeftRightMotorSpeed(),
                phidgetsConfig.neckLeftRightMotorAcceleration()
        );
        System.out.println("Cou : fin constructeur moteurPanoramique");
        moteurInclinaison = new PhidgetsServoMotor(
                phidgetsConfig.neckTiltMotorIndex(),
                phidgetsConfig.neckTiltMotorInitialPosition(),
                phidgetsConfig.neckTiltMotorMinPosition(),
                phidgetsConfig.neckTiltMotorMaxPosition(),
                phidgetsConfig.neckTiltMotorSpeed(),
                phidgetsConfig.neckTiltMotorAcceleration()
        );
        System.out.println("Cou : fin constructeur moteurInclinaison");
        moteurMonterDescendre = new PhidgetsServoMotor(
                phidgetsConfig.neckUpDownMotorIndex(),
                phidgetsConfig.neckUpDownMotorInitialPosition(),
                phidgetsConfig.neckUpDownMotorMinPosition(),
                phidgetsConfig.neckUpDownMotorMaxPosition(),
                phidgetsConfig.neckUpDownMotorSpeed(),
                phidgetsConfig.neckUpDownMotorAcceleration()
        );
        System.out.println("Cou : fin constructeur moteurMonterDescendre");

        moteurPanoramique.setEngaged(true);
        moteurPanoramique.setSpeedRampingState(true);

        moteurInclinaison.setEngaged(true);
        moteurInclinaison.setSpeedRampingState(true);

        moteurMonterDescendre.setEngaged(true);
        moteurMonterDescendre.setSpeedRampingState(true);

        System.out.println("Cou : fin Constructeur");
    }

    @Override
    public void initialiser() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        reset();
        System.out.println("Cou : fin initialisation");
    }

    /**
     * Tourne la tête à gauche sans s'arrêter.
     */
    public void tournerAGauche(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE) {
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE;
            moteurPanoramique.backward(vitesse, acceleration, waitForPosition);
        } else {
            moteurPanoramique.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête à droite sans s'arrêter.
     */
    public void tournerADroite(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE) {
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE;
            moteurPanoramique.forward(vitesse, acceleration, waitForPosition);
        } else {
            moteurPanoramique.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête sur le plan "Gauche / Droite" d'un certain angle.
     *
     * @param angle angle en degrés (négatif : à droite, positif : à gauche)
     */
    public void tournerTeteGaucheDroite(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurPanoramique.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Gauche / Droite" à une position précise (0 : à gauche, 180 : à droite).
     *
     * @param position position en degrés (0 : à gauche, 180 : à droite)
     */
    public void positionnerTeteGaucheDroite(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = phidgetsConfig.neckLeftRightMotorInitialPosition() - position;
        if (positionMoteur >= moteurPanoramique.getPositionMin() && positionMoteur <= moteurPanoramique.getPositionMax()) {
            System.out.println("POS_GD = " + positionMoteur);
            moteurPanoramique.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de la tête sur le plan "Gauche / Droite".
     */
    public void stopperTeteGaucheDroite() {
        moteurPanoramique.stop();
        mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
    }

    /**
     * Tourne la tête en bas sans s'arrêter.
     */
    public void tournerEnBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsInclinaisonEnCours != MOUVEMENTS_INCLINAISON.TOURNER_BAS) {
            System.out.println("BAS");
            mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.TOURNER_BAS;
            moteurInclinaison.forward(vitesse, acceleration, waitForPosition);
        } else {
            System.out.println("BAS VITESSE");
            moteurInclinaison.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête en haut sans s'arrêter.
     */
    public void tournerEnHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsInclinaisonEnCours != MOUVEMENTS_INCLINAISON.TOURNER_HAUT) {
            System.out.println("HAUT");
            mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.TOURNER_HAUT;
            moteurInclinaison.backward(vitesse, acceleration, waitForPosition);
        } else {
            System.out.println("HAUT VITESSE");
            moteurInclinaison.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête sur le plan "Haut / Bas" d'un certain angle.
     *
     * @param angle angle en degrés (négatif : en bas, positif : en haut)
     */
    public void tournerTeteHautBas(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurInclinaison.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Haut / Bas" à une position précise (0 : en bas, 180 : en haut).
     *
     * @param position position en degrés (0 : en bas, 180 : en haut)
     */
    public void positionnerTeteHautBas(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = phidgetsConfig.neckTiltMotorInitialPosition() - position;
        if (positionMoteur >= moteurInclinaison.getPositionMin() && positionMoteur <= moteurInclinaison.getPositionMax()) {
            System.out.println("POS_HB = " + positionMoteur);
            moteurInclinaison.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de la tête sur le plan "Haut / Bas".
     */
    public void stopperTeteHautBas() {
        System.out.println("STOP HAUT BAS");
        moteurInclinaison.stop();
        mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
    }

    /**
     * Descend la tête sans s'arrêter.
     */
    public void descendre(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsMonterDescendreEnCours != MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE) {
            System.out.println("DESCENDRE");
            mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE;
            moteurMonterDescendre.forward(vitesse, acceleration, waitForPosition);
        } else {
            System.out.println("DESCENDRE VITESSE");
            moteurMonterDescendre.setVitesse(vitesse);
        }
    }

    /**
     * Monte la tête sans s'arrêter.
     */
    public void monter(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsMonterDescendreEnCours != MOUVEMENTS_MONTER_DESCENDRE.MONTER) {
            System.out.println("MONTER");
            mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.MONTER;
            moteurMonterDescendre.backward(vitesse, acceleration, waitForPosition);
        } else {
            System.out.println("MONTER VITESSE");
            moteurMonterDescendre.setVitesse(vitesse);
        }
    }

    /**
     * Monte ou descend la tête d'un certain angle.
     *
     * @param angle angle en degrés (négatif : descend, positif : monte)
     */
    public void monterDescendreTete(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurMonterDescendre.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Monte ou descend la tête à une position précise (0 : en bas, 180 : en haut).
     *
     * @param position position en degrés (0 : en bas, 180 : en haut)
     */
    public void positionnerTeteMonterDescendre(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = phidgetsConfig.neckUpDownMotorInitialPosition() - position;
        if (positionMoteur >= moteurMonterDescendre.getPositionMin() && positionMoteur <= moteurMonterDescendre.getPositionMax()) {
            System.out.println("POS_MD = " + positionMoteur);
            moteurMonterDescendre.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de la tête sur le plan "Monter - Descendre".
     */
    public void stopperTeteMonterDescendre() {
        System.out.println("STOP MonterDescendre");
        moteurMonterDescendre.stop();
        mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;
    }

    /**
     * Intercepte les évènements de mouvements.
     *
     * @param mouvementCouEvent évènement de mouvements
     */
    @Subscribe
    public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        System.out.println("COU : Event = " + mouvementCouEvent + ", Thread = " + Thread.currentThread().getName());
        if (mouvementCouEvent.getPositionPanoramique() != MouvementCouEvent.POSITION_NEUTRE) {
            // TODO ne pas mettre en synchrone si Haut/Bas en synchrone ==> A corriger
            positionnerTeteGaucheDroite(mouvementCouEvent.getPositionPanoramique(), mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getAnglePanoramique() != MouvementCouEvent.ANGLE_NEUTRE) {
            // TODO ne pas mettre en synchrone si Haut/Bas en synchrone ==> A corriger
            tournerTeteGaucheDroite(mouvementCouEvent.getAnglePanoramique(), mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementPanoramique() != null) {
            if (mouvementCouEvent.getMouvementPanoramique() == MOUVEMENTS_PANORAMIQUE.STOPPER) {
                stopperTeteGaucheDroite();
            } else if (mouvementCouEvent.getMouvementPanoramique() == MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE) {
                tournerAGauche(mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
            } else if (mouvementCouEvent.getMouvementPanoramique() == MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE) {
                tournerADroite(mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
            }
        }
        if (mouvementCouEvent.getPositionInclinaison() != MouvementCouEvent.POSITION_NEUTRE) {
            positionnerTeteHautBas(mouvementCouEvent.getPositionInclinaison(), mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getAngleInclinaison() != MouvementCouEvent.ANGLE_NEUTRE) {
            tournerTeteHautBas(mouvementCouEvent.getPositionInclinaison(), mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementInclinaison() != null) {
            if (mouvementCouEvent.getMouvementInclinaison() == MOUVEMENTS_INCLINAISON.STOPPER) {
                stopperTeteHautBas();
            } else if (mouvementCouEvent.getMouvementInclinaison() == MOUVEMENTS_INCLINAISON.TOURNER_HAUT) {
                tournerEnHaut(mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
            } else if (mouvementCouEvent.getMouvementInclinaison() == MOUVEMENTS_INCLINAISON.TOURNER_BAS) {
                tournerEnBas(mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
            }
        }
        if (mouvementCouEvent.getPositionMonterDescendre() != MouvementCouEvent.POSITION_NEUTRE) {
            positionnerTeteMonterDescendre(mouvementCouEvent.getPositionMonterDescendre(), mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getAngleMonterDescendre() != MouvementCouEvent.ANGLE_NEUTRE) {
            monterDescendreTete(mouvementCouEvent.getPositionMonterDescendre(), mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementMonterDescendre() != null) {
            if (mouvementCouEvent.getMouvementMonterDescendre() == MOUVEMENTS_MONTER_DESCENDRE.STOPPER) {
                stopperTeteMonterDescendre();
            } else if (mouvementCouEvent.getMouvementMonterDescendre() == MOUVEMENTS_MONTER_DESCENDRE.MONTER) {
                monter(mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
            } else if (mouvementCouEvent.getMouvementMonterDescendre() == MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE) {
                descendre(mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
            }
        }
    }

    @Override
    public void arreter() {
        reset();
        // Attente du retour à la position initiale
        while (moteurPanoramique.getPositionReelle() != phidgetsConfig.neckLeftRightMotorInitialPosition()
                && moteurInclinaison.getPositionReelle() != phidgetsConfig.neckTiltMotorInitialPosition()
                && moteurMonterDescendre.getPositionReelle() != phidgetsConfig.neckUpDownMotorInitialPosition()) {
        }
        moteurPanoramique.stop();
        moteurInclinaison.stop();
        moteurMonterDescendre.stop();
        moteurPanoramique.setSpeedRampingState(true);
        moteurInclinaison.setSpeedRampingState(true);
        moteurMonterDescendre.setSpeedRampingState(true);
        moteurPanoramique.setEngaged(false);
        moteurInclinaison.setEngaged(false);
        moteurMonterDescendre.setEngaged(false);
        moteurPanoramique.close();
        moteurInclinaison.close();
        moteurMonterDescendre.close();
    }

    /**
     * Remet la tête à sa position par défaut.
     */
    private void reset() {
        moteurInclinaison.setPositionCible(phidgetsConfig.neckTiltMotorInitialPosition(), null, null, false);
        moteurPanoramique.setPositionCible(phidgetsConfig.neckLeftRightMotorInitialPosition(), null, null, false);
        moteurMonterDescendre.setPositionCible(phidgetsConfig.neckUpDownMotorInitialPosition(), null, null, false);
    }

    /**
     * Intercepte les évènements d'affichage de position.
     *
     * @param displayPositionEvent évènement
     */
    @Subscribe
    public void handleDisplayPositionEvent(DisplayPositionEvent displayPositionEvent) {
        double positionGaucheDroite = phidgetsConfig.neckLeftRightMotorInitialPosition() - moteurPanoramique.getPositionReelle();
        double positionHautBas = phidgetsConfig.neckTiltMotorInitialPosition() - moteurInclinaison.getPositionReelle();
        double positionMonterDescendre = phidgetsConfig.neckUpDownMotorInitialPosition() - moteurMonterDescendre.getPositionReelle();
        //System.out.println("COU\tGD = " + positionGaucheDroite + "\tHB = " + positionHautBas);
    }

    public void testMouvement() {
        int pas = 30;
        System.out.println("====== PAS 30");
        for (int angle = -60; angle <= 60; angle += pas) {
            positionnerTeteGaucheDroite(angle, 180D, 150D, true);
        }
        pas = 5;
        System.out.println("====== PAS 5");
        for (int angle = -60; angle <= 60; angle += pas) {
            positionnerTeteGaucheDroite(angle, 180D, 150D, true);
        }
        pas = 1;
        System.out.println("====== PAS 1");
        for (int angle = -60; angle <= 60; angle += pas) {
            positionnerTeteGaucheDroite(angle, 180D, 150D, true);
        }
//		System.out.println("====== PAS 0");
//		positionnerTeteGaucheDroite(-60, true);
//		positionnerTeteGaucheDroite(60, true);

    }

    public static void main(String[] args) {
//		try {
        int temps = 500;
        Cou cou = new Cou();
        cou.initialiser();
        RobotEventBus.getInstance().subscribe(cou);
        //cou.testMouvement();
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(0);
        mouvementCouEvent.setPositionInclinaison(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(-30);
        mouvementCouEvent.setVitessePanoramique(300D);
        mouvementCouEvent.setPositionInclinaison(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(30);
        mouvementCouEvent.setVitessePanoramique(300D);
        mouvementCouEvent.setPositionInclinaison(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(-30);
        mouvementCouEvent.setVitessePanoramique(800D);
        mouvementCouEvent.setAccelerationPanoramique(400D);
        mouvementCouEvent.setPositionInclinaison(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(30);
        mouvementCouEvent.setVitessePanoramique(800D);
        mouvementCouEvent.setAccelerationPanoramique(400D);
        mouvementCouEvent.setPositionInclinaison(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(0);
        mouvementCouEvent.setPositionInclinaison(30);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(30);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionInclinaison(-30);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(-30);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionInclinaison(30);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionPanoramique(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
        mouvementCouEvent = new MouvementCouEvent();
        mouvementCouEvent.setPositionInclinaison(0);
        mouvementCouEvent.setSynchrone(true);
        RobotEventBus.getInstance().publish(mouvementCouEvent);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE);
//			mouvementCouEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_BAS);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE);
//			mouvementCouEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_BAS);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE);
//			mouvementCouEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_HAUT);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE);
//			mouvementCouEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.TOURNER_HAUT);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setMouvementGaucheDroite(MOUVEMENTS_GAUCHE_DROITE.STOPPER);
//			mouvementCouEvent.setMouvementHauBas(MOUVEMENTS_HAUT_BAS.STOPPER);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
    }

}
