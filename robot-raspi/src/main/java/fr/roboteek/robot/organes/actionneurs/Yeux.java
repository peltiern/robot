package fr.roboteek.robot.organes.actionneurs;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_ROULIS;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent.MOUVEMENTS_OEIL;
import fr.roboteek.robot.util.phidgets.PhidgetsServoMotor;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Classe représentant les yeux du robot.
 *
 * @author Java Developer
 */
public class Yeux extends AbstractOrgane {

    /**
     * Moteur Gauche / Droite.
     */
    private PhidgetsServoMotor moteurOeilGauche;

    /**
     * Moteur Haut / Bas.
     */
    private PhidgetsServoMotor moteurOeilDroit;

    /**
     * Flag indiquant que le roulis est en cours.
     */
    private boolean roulisEnCours = false;

    /**
     * Position relative de l'oeil gauche au début du roulis.
     */
    private double positionRelativeOeilGaucheDebutRoulis = 0;

    /**
     * Position relative de l'oeil droit au début du roulis.
     */
    private double positionRelativeOeilDroitDebutRoulis = 0;

    /**
     * Angle roulis.
     */
    private double angleRoulis = 0;

    /**
     * Phidgets Configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    private MOUVEMENTS_OEIL mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.STOPPER;
    private MOUVEMENTS_OEIL mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.STOPPER;

    /**
     * Constructeur.
     */
    public Yeux() {
        super();

        phidgetsConfig = phidgetsConfig();

        System.out.println("YEUX :, Thread = " + Thread.currentThread().getName());

        // Création et initialisation des moteurs
        moteurOeilGauche = new PhidgetsServoMotor(
                phidgetsConfig.eyeLeftMotorIndex(),
                phidgetsConfig.eyeLeftMotorPositionZero(),
                toPositionAbsolueOeilGauche(phidgetsConfig.eyeMotorRelativePositionMax()),
                toPositionAbsolueOeilGauche(phidgetsConfig.eyeMotorRelativePositionMin()),
                phidgetsConfig.eyeLeftMotorSpeed(),
                phidgetsConfig.eyeLeftMotorAcceleration()
        );
        moteurOeilDroit = new PhidgetsServoMotor(
                phidgetsConfig.eyeRightMotorIndex(),
                phidgetsConfig.eyeRightMotorPositionZero(),
                toPositionAbsolueOeilDroit(phidgetsConfig.eyeMotorRelativePositionMax()),
                toPositionAbsolueOeilDroit(phidgetsConfig.eyeMotorRelativePositionMin()),
                phidgetsConfig.eyeRightMotorSpeed(),
                phidgetsConfig.eyeRightMotorAcceleration()
        );

        moteurOeilGauche.setSpeedRampingState(true);

        moteurOeilDroit.setSpeedRampingState(true);

    }

    @Override
    public void initialiser() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        reset();
    }

    /**
     * Tourne l'oeil gauche vers le bas sans s'arrêter.
     */
    public void tournerOeilGaucheVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.TOURNER_BAS) {
            mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.TOURNER_BAS;
            moteurOeilGauche.forward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil gauche vers le haut sans s'arrêter.
     */
    public void tournerOeilGaucheVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.TOURNER_HAUT) {
            mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.TOURNER_HAUT;
            moteurOeilGauche.backward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil gauche d'un certain angle.
     *
     * @param angle angle en degrés (négatif : vers le bas, positif : vers le haut)
     */
    public void tournerOeilGauche(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurOeilGauche.rotate(-angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne l'oeil gauche à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     *
     * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilGauche(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
        if (positionRelative >= phidgetsConfig.eyeMotorRelativePositionMin() && positionRelative <= phidgetsConfig.eyeMotorRelativePositionMax()) {
            double positionMoteur = toPositionAbsolueOeilGauche(positionRelative);
            moteurOeilGauche.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de l'oeil gauche.
     */
    public void stopperOeilGauche() {
        moteurOeilGauche.stop();
        mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.STOPPER;
    }

    /**
     * Tourne l'oeil droit vers le bas sans s'arrêter.
     */
    public void tournerOeilDroitVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.TOURNER_BAS) {
            mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.TOURNER_BAS;
            moteurOeilDroit.forward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil droit vers le haut sans s'arrêter.
     */
    public void tournerOeilDroitVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.TOURNER_HAUT) {
            mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.TOURNER_HAUT;
            moteurOeilDroit.backward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil droit d'un certain angle.
     *
     * @param angle angle en degrés (négatif : en bas, positif : en haut)
     */
    public void tournerOeilDroit(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurOeilDroit.rotate(-angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne l'oeil droit à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     *
     * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilDroit(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
        if (positionRelative >= phidgetsConfig.eyeMotorRelativePositionMin() && positionRelative <= phidgetsConfig.eyeMotorRelativePositionMax()) {
            double positionMoteur = toPositionAbsolueOeilDroit(positionRelative);
            moteurOeilDroit.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de l'oeil droit.
     */
    public void stopperOeilDroit() {
        moteurOeilDroit.stop();
        mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.STOPPER;
    }

    /**
     * Positionne le roulis à l'angle demandé (référence : oeil gauche de face).
     *
     * @param newAngleRoulis l'angle de roulis
     */
    public void setPositionRoulis(double newAngleRoulis, Double vitesse, Double acceleration, boolean waitForPosition) {
        // Récupération des positions de chaque oeil si c'est le début du roulis
        if (!roulisEnCours) {
            positionRelativeOeilGaucheDebutRoulis = toPositionRelativeOeilGauche(moteurOeilGauche.getPositionReelle());
            positionRelativeOeilDroitDebutRoulis = toPositionRelativeOeilDroit(moteurOeilDroit.getPositionReelle());
        }
        roulisEnCours = true;

        // Calcul de l'angle max (référence oeil gauche)
        double angleRoulisAAffecter = newAngleRoulis;
        if (positionRelativeOeilGaucheDebutRoulis + angleRoulisAAffecter > phidgetsConfig.eyeMotorRelativePositionMax()) {
            angleRoulisAAffecter = phidgetsConfig.eyeMotorRelativePositionMax() - positionRelativeOeilGaucheDebutRoulis;
        }
        if (positionRelativeOeilGaucheDebutRoulis + angleRoulisAAffecter < phidgetsConfig.eyeMotorRelativePositionMin()) {
            angleRoulisAAffecter = phidgetsConfig.eyeMotorRelativePositionMin() - positionRelativeOeilGaucheDebutRoulis;
        }
        if (positionRelativeOeilDroitDebutRoulis - angleRoulisAAffecter > phidgetsConfig.eyeMotorRelativePositionMax()) {
            angleRoulisAAffecter = -(phidgetsConfig.eyeMotorRelativePositionMax() - positionRelativeOeilDroitDebutRoulis);
        }
        if (positionRelativeOeilDroitDebutRoulis - angleRoulisAAffecter < phidgetsConfig.eyeMotorRelativePositionMin()) {
            angleRoulisAAffecter = -(phidgetsConfig.eyeMotorRelativePositionMin() - positionRelativeOeilDroitDebutRoulis);
        }
        final double nouvellePositionRelativeOeilGauche = positionRelativeOeilGaucheDebutRoulis + angleRoulisAAffecter;
        final double nouvellePositionRelativeOeilDroit = positionRelativeOeilDroitDebutRoulis - angleRoulisAAffecter;
        //System.out.println("newAngleRoulis = " + newAngleRoulis + ", angleRoulis = " + angleRoulisAAffecter + ", positionOeilGaucheDebutRoulis = " + positionRelativeOeilGaucheDebutRoulis + ", nouvellePositionOeilGauche = " + nouvellePositionRelativeOeilGauche + ", positionOeilDroitDebutRoulis = " + positionRelativeOeilDroitDebutRoulis + ", nouvellePositionOeilDroit = " + nouvellePositionRelativeOeilDroit);
        positionnerOeilGauche(nouvellePositionRelativeOeilGauche, vitesse, acceleration, false);
        positionnerOeilDroit(nouvellePositionRelativeOeilDroit, vitesse, acceleration, waitForPosition);
        angleRoulis = angleRoulisAAffecter;
    }


    /**
     * Intercepte les évènements de mouvements.
     *
     * @param mouvementYeuxEvent évènement de mouvements
     */
    @Subscribe
    public void handleMouvementYeuxEvent(MouvementYeuxEvent mouvementYeuxEvent) {
        System.out.println("YEUX : Event = " + mouvementYeuxEvent + ", Thread = " + Thread.currentThread().getName());
        if (mouvementYeuxEvent.getPositionOeilGauche() != MouvementYeuxEvent.POSITION_NEUTRE) {
            // TODO ne pas mettre en synchrone si oeil droit en synchrone ==> A corriger
            roulisEnCours = false;
            positionnerOeilGauche(mouvementYeuxEvent.getPositionOeilGauche(), mouvementYeuxEvent.getVitesseOeilGauche(), mouvementYeuxEvent.getAccelerationOeilGauche(), false/*mouvementYeuxEvent.isSynchrone()*/);
        } else if (mouvementYeuxEvent.getMouvementOeilGauche() != null) {
            if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.STOPPER) {
                stopperOeilGauche();
            } else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_BAS) {
                roulisEnCours = false;
                tournerOeilGaucheVersBas(mouvementYeuxEvent.getVitesseOeilGauche(), mouvementYeuxEvent.getAccelerationOeilGauche(), mouvementYeuxEvent.isSynchrone());
            } else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
                roulisEnCours = false;
                tournerOeilGaucheVersHaut(mouvementYeuxEvent.getVitesseOeilGauche(), mouvementYeuxEvent.getAccelerationOeilGauche(), mouvementYeuxEvent.isSynchrone());
            }
        }
        if (mouvementYeuxEvent.getPositionOeilDroit() != MouvementYeuxEvent.POSITION_NEUTRE) {
            roulisEnCours = false;
            positionnerOeilDroit(mouvementYeuxEvent.getPositionOeilDroit(), mouvementYeuxEvent.getVitesseOeilDroit(), mouvementYeuxEvent.getAccelerationOeilDroit(), mouvementYeuxEvent.isSynchrone());
        } else if (mouvementYeuxEvent.getMouvementOeilDroit() != null) {
            if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.STOPPER) {
                stopperOeilDroit();
            } else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_BAS) {
                roulisEnCours = false;
                tournerOeilDroitVersBas(mouvementYeuxEvent.getVitesseOeilDroit(), mouvementYeuxEvent.getAccelerationOeilDroit(), mouvementYeuxEvent.isSynchrone());
            } else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
                roulisEnCours = false;
                tournerOeilDroitVersHaut(mouvementYeuxEvent.getVitesseOeilDroit(), mouvementYeuxEvent.getAccelerationOeilDroit(), mouvementYeuxEvent.isSynchrone());
            }
        }
        roulisEnCours = false;
    }

    /**
     * Intercepte les évènements de mouvements de cou.
     *
     * @param mouvementCouEvent évènement de mouvements de cou
     */
    @Subscribe
    public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        //System.out.println("YEUX : Event = " + mouvementCouEvent + ", Thread = " + Thread.currentThread().getName());
        if (mouvementCouEvent.getMouvementRoulis() == MOUVEMENTS_ROULIS.HORAIRE) {
            setPositionRoulis(mouvementCouEvent.getPositionRoulis(), mouvementCouEvent.getVitesseRoulis(), mouvementCouEvent.getAccelerationRoulis(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementRoulis() == MOUVEMENTS_ROULIS.ANTI_HORAIRE) {
            setPositionRoulis(-mouvementCouEvent.getPositionRoulis(), mouvementCouEvent.getVitesseRoulis(), mouvementCouEvent.getAccelerationRoulis(), mouvementCouEvent.isSynchrone());
        } else {
            roulisEnCours = false;
        }
    }

//	/**
//	 * Intercepte les évènements de mouvements.
//	 * @param mouvementYeuxEvent évènement de mouvements
//	 */
//	@Subscribe
//	public void handleRobotEvent(RobotEvent robotEvent) {
//		System.out.println("YEUX : Event = " + robotEvent + ", Thread = " + Thread.currentThread().getName());
//		if (robotEvent instanceof MouvementYeuxEvent) {
//			final MouvementYeuxEvent mouvementYeuxEvent = (MouvementYeuxEvent) robotEvent;
//			if (mouvementYeuxEvent.getPositionOeilGauche() != -1) {
//				positionnerOeilGauche(mouvementYeuxEvent.getPositionOeilGauche());
//			} else if (mouvementYeuxEvent.getMouvementOeilGauche() != null) {
//				if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.STOPPER) {
//					stopperOeilGauche();
//				} else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_BAS) {
//					tournerOeilGaucheVersBas();
//				} else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
//					tournerOeilGaucheVersHaut();
//				}
//			}
//			if (mouvementYeuxEvent.getPositionOeilDroit() != -1) {
//				positionnerOeilDroit(mouvementYeuxEvent.getPositionOeilDroit());
//			} else if (mouvementYeuxEvent.getMouvementOeilDroit() != null) {
//				if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.STOPPER) {
//					stopperOeilDroit();
//				} else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_BAS) {
//					tournerOeilDroitVersBas();
//				} else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
//					tournerOeilDroitVersHaut();
//				}
//			}
//			roulisEnCours = false;
//			
//		} else if (robotEvent instanceof MouvementCouEvent) {
//			final MouvementCouEvent mouvementCouEvent = (MouvementCouEvent) robotEvent;
//			if (mouvementCouEvent.getMouvementRoulis() == MOUVEMENTS_ROULIS.HORAIRE) {
//				setPositionRoulis(mouvementCouEvent.getPositionRoulis());
//			} else if (mouvementCouEvent.getMouvementRoulis() == MOUVEMENTS_ROULIS.ANTI_HORAIRE) {
//				setPositionRoulis(-mouvementCouEvent.getPositionRoulis());
//			} else {
//				roulisEnCours = false;
//				stopperOeilGauche();
//				stopperOeilDroit();
//			}
//		}
//	}

    @Override
    public void arreter() {
        reset();
        // Attente du retour à la position initiale
        while (moteurOeilGauche.getPositionReelle() != phidgetsConfig.eyeLeftMotorPositionZero()
                && moteurOeilDroit.getPositionReelle() != phidgetsConfig.eyeRightMotorPositionZero()) {
        }
        moteurOeilGauche.stop();
        moteurOeilDroit.stop();
        moteurOeilGauche.setSpeedRampingState(true);
        moteurOeilDroit.setSpeedRampingState(true);
        moteurOeilGauche.setEngaged(false);
        moteurOeilDroit.setEngaged(false);
        moteurOeilGauche.close();
        moteurOeilDroit.close();
    }

    /**
     * Remet les yeux à leur position par défaut.
     */
    private void reset() {
        moteurOeilDroit.setPositionCible(phidgetsConfig.eyeRightMotorPositionZero(), null, null, false);
        moteurOeilGauche.setPositionCible(phidgetsConfig.eyeLeftMotorPositionZero(), null, null, false);
        roulisEnCours = false;
        positionRelativeOeilDroitDebutRoulis = 0;
        positionRelativeOeilGaucheDebutRoulis = 0;
        angleRoulis = 0;
    }

    /**
     * Calcule la position absolue de l'oeil gauche à partir d'une position relative.
     *
     * @param positionRelative la position relative
     */
    private double toPositionAbsolueOeilGauche(double positionRelative) {
        return phidgetsConfig.eyeLeftMotorPositionZero() - positionRelative;
    }

    /**
     * Calcule la position relative de l'oeil gauche à partir d'une position absolue.
     *
     * @param positionAbsolue la position absolue
     */
    private double toPositionRelativeOeilGauche(double positionAbsolue) {
        return phidgetsConfig.eyeLeftMotorPositionZero() - positionAbsolue;
    }

    /**
     * Calcule la position absolue de l'oeil droit à partir d'une position relative.
     *
     * @param positionRelative la position relative
     */
    private double toPositionAbsolueOeilDroit(double positionRelative) {
        return phidgetsConfig.eyeRightMotorPositionZero() - positionRelative;
    }

    /**
     * Calcule la position relative de l'oeil droit à partir d'une position absolue.
     *
     * @param positionAbsolue la position absolue
     */
    private double toPositionRelativeOeilDroit(double positionAbsolue) {
        return phidgetsConfig.eyeRightMotorPositionZero() - positionAbsolue;
    }

    /**
     * Intercepte les évènements d'affichage de position.
     *
     * @param displayPositionEvent évènement
     */
    @Subscribe
    public void handleDisplayPositionEvent(DisplayPositionEvent displayPositionEvent) {
        double positionRelativeOeilGauche = toPositionRelativeOeilGauche(moteurOeilGauche.getPositionReelle());
        double positionRelativeOeilDroit = toPositionRelativeOeilDroit(moteurOeilDroit.getPositionReelle());
        //System.out.println("YEUX\tgauche = " + positionRelativeOeilGauche + "\tdroit = " + positionRelativeOeilDroit);
    }

    public static void main(String[] args) {
        Yeux tete = new Yeux();
        tete.initialiser();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        tete.positionnerOeilGauche(-15, null, null, true);
        tete.positionnerOeilDroit(5, null, null, true);
    }
}
