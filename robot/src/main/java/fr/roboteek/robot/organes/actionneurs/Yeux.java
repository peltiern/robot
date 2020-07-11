package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_ROULIS;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent.MOUVEMENTS_OEIL;
import fr.roboteek.robot.util.phidget.PhidgetServoMotor;
import net.engio.mbassy.listener.Handler;

/**
 * Classe représentant les yeux du robot.
 * @author Java Developer
 */
public class Yeux extends AbstractOrgane {

	/** Index du moteur de l'oeil gauche sur le servo-contrôleur. */
	private static final int IDX_MOTEUR_OEIL_GAUCHE = 2;

	/** Index du moteur de l'oeil droit sur le servo-contrôleur. */
	private static final int IDX_MOTEUR_OEIL_DROIT = 3;

	/** Position "Zéro" du moteur de l'oeil gauche. */
	private static final double POSITION_ZERO_MOTEUR_OEIL_GAUCHE = 92;

	/** Position "Zéro" du moteur de l'oeil droit. */
	private static final double POSITION_ZERO_MOTEUR_OEIL_DROIT = 86;
	
	/** Position minimale relative d'un oeil. */
	public static final double POSITION_MINIMALE_RELATIVE_OEIL = -24;
	
	/** Position maximale relative d'un oeil. */
	public static final double POSITION_MAXIMALE_RELATIVE_OEIL = 20;
	
	/** Position minimale du moteur de l'oeil gauche. */
	private static final double POSITION_MINIMALE_MOTEUR_OEIL_GAUCHE = toPositionAbsolueOeilGauche(POSITION_MAXIMALE_RELATIVE_OEIL);
	
	/** Position maximale du moteur de l'oeil gauche. */
	private static final double POSITION_MAXIMALE_MOTEUR_OEIL_GAUCHE = toPositionAbsolueOeilGauche(POSITION_MINIMALE_RELATIVE_OEIL);

	/** Position minimale du moteur de l'oeil droit. */
	private static final double POSITION_MINIMALE_MOTEUR_OEIL_DROIT = toPositionAbsolueOeilDroit(POSITION_MAXIMALE_RELATIVE_OEIL);
	
	/** Position maximale du moteur de l'oeil droit. */
	private static final double POSITION_MAXIMALE_MOTEUR_OEIL_DROIT = toPositionAbsolueOeilDroit(POSITION_MINIMALE_RELATIVE_OEIL);

	public static final double DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE = 40;

	public static final double DEFAULT_SPEED_MOTEUR_OEIL_DROIT = 40;

	public static final double DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE = 60;

	public static final double DEFAULT_ACCELERATION_MOTEUR_OEIL_DROIT = 60;

	/** Moteur Gauche / Droite. */
	private PhidgetServoMotor moteurOeilGauche;

	/** Moteur Haut / Bas. */
	private PhidgetServoMotor moteurOeilDroit;
	
	/** Flag indiquant que le roulis est en cours. */
	private boolean roulisEnCours= false;
	
	/** Position relative de l'oeil gauche au début du roulis. */
	private double positionRelativeOeilGaucheDebutRoulis = 0;
	
	/** Position relative de l'oeil droit au début du roulis. */
	private double positionRelativeOeilDroitDebutRoulis = 0;

	/** Angle roulis. */
	private double angleRoulis = 0;

	/** Constructeur. */
	public Yeux() {
		super();
		
		System.out.println("YEUX :, Thread = " + Thread.currentThread().getName());

		// Création et initialisation des moteurs
		moteurOeilGauche = new PhidgetServoMotor(IDX_MOTEUR_OEIL_GAUCHE, POSITION_ZERO_MOTEUR_OEIL_GAUCHE, POSITION_MINIMALE_MOTEUR_OEIL_GAUCHE, POSITION_MAXIMALE_MOTEUR_OEIL_GAUCHE,
				DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE, DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE);
		moteurOeilDroit = new PhidgetServoMotor(IDX_MOTEUR_OEIL_DROIT, POSITION_ZERO_MOTEUR_OEIL_DROIT, POSITION_MINIMALE_MOTEUR_OEIL_DROIT, POSITION_MAXIMALE_MOTEUR_OEIL_DROIT,
				DEFAULT_SPEED_MOTEUR_OEIL_DROIT, DEFAULT_ACCELERATION_MOTEUR_OEIL_DROIT);

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

	/** Tourne l'oeil gauche vers le bas sans s'arrêter. */
	public void tournerOeilGaucheVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurOeilGauche.forward(vitesse, acceleration, waitForPosition);
	}

	/** Tourne l'oeil gauche vers le haut sans s'arrêter. */
	public void tournerOeilGaucheVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurOeilGauche.backward(vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne l'oeil gauche d'un certain angle.
	 * @param angle angle en degrés (négatif : vers le bas, positif : vers le haut)
	 */
	public void tournerOeilGauche(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurOeilGauche.rotate(-angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Positionne l'oeil gauche à une position précise (0 : horizontal, min bas : -45, max haut : 15).
	 * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
	 */
	public void positionnerOeilGauche(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
		if (positionRelative >= POSITION_MINIMALE_RELATIVE_OEIL && positionRelative <= POSITION_MAXIMALE_RELATIVE_OEIL) {
			double positionMoteur = toPositionAbsolueOeilGauche(positionRelative);
			moteurOeilGauche.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
		}
	}

	/**
	 * Stoppe le mouvement de l'oeil gauche.
	 */
	public void stopperOeilGauche() {
		moteurOeilGauche.stop();
	}

	/** Tourne l'oeil droit vers le bas sans s'arrêter. */
	public void tournerOeilDroitVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurOeilDroit.forward(vitesse, acceleration, waitForPosition);
	}

	/** Tourne l'oeil droit vers le haut sans s'arrêter. */
	public void tournerOeilDroitVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurOeilDroit.backward(vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne l'oeil droit d'un certain angle.
	 * @param angle angle en degrés (négatif : en bas, positif : en haut)
	 */
	public void tournerOeilDroit(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurOeilDroit.rotate(-angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Positionne l'oeil droit à une position précise (0 : horizontal, min bas : -45, max haut : 15).
	 * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
	 */
	public void positionnerOeilDroit(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
		if (positionRelative >= POSITION_MINIMALE_RELATIVE_OEIL && positionRelative <= POSITION_MAXIMALE_RELATIVE_OEIL) {
			double positionMoteur = toPositionAbsolueOeilDroit(positionRelative);
			moteurOeilDroit.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
		}
	}

	/**
	 * Stoppe le mouvement de l'oeil droit.
	 */
	public void stopperOeilDroit() {
		moteurOeilDroit.stop();
	}

	/**
	 * Positionne le roulis à l'angle demandé (référence : oeil gauche de face).
	 * @param angleRoulis l'angle de roulis
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
		if (positionRelativeOeilGaucheDebutRoulis + angleRoulisAAffecter > POSITION_MAXIMALE_RELATIVE_OEIL) {
			angleRoulisAAffecter = POSITION_MAXIMALE_RELATIVE_OEIL - positionRelativeOeilGaucheDebutRoulis;
		}
		if (positionRelativeOeilGaucheDebutRoulis + angleRoulisAAffecter < POSITION_MINIMALE_RELATIVE_OEIL) {
			angleRoulisAAffecter = POSITION_MINIMALE_RELATIVE_OEIL - positionRelativeOeilGaucheDebutRoulis;
		}
		if (positionRelativeOeilDroitDebutRoulis - angleRoulisAAffecter > POSITION_MAXIMALE_RELATIVE_OEIL) {
			angleRoulisAAffecter = -(POSITION_MAXIMALE_RELATIVE_OEIL - positionRelativeOeilDroitDebutRoulis);
		}
		if (positionRelativeOeilDroitDebutRoulis - angleRoulisAAffecter < POSITION_MINIMALE_RELATIVE_OEIL) {
			angleRoulisAAffecter = -(POSITION_MINIMALE_RELATIVE_OEIL - positionRelativeOeilDroitDebutRoulis);
		}
		final double nouvellePositionRelativeOeilGauche = positionRelativeOeilGaucheDebutRoulis + angleRoulisAAffecter;
		final double nouvellePositionRelativeOeilDroit = positionRelativeOeilDroitDebutRoulis - angleRoulisAAffecter;
		System.out.println("newAngleRoulis = " + newAngleRoulis + ", angleRoulis = " + angleRoulisAAffecter + ", positionOeilGaucheDebutRoulis = " + positionRelativeOeilGaucheDebutRoulis + ", nouvellePositionOeilGauche = " + nouvellePositionRelativeOeilGauche + ", positionOeilDroitDebutRoulis = " + positionRelativeOeilDroitDebutRoulis + ", nouvellePositionOeilDroit = " + nouvellePositionRelativeOeilDroit);
		positionnerOeilGauche(nouvellePositionRelativeOeilGauche, vitesse, acceleration, false);
		positionnerOeilDroit(nouvellePositionRelativeOeilDroit, vitesse, acceleration, waitForPosition);
		angleRoulis = angleRoulisAAffecter;
	}


	/**
	 * Intercepte les évènements de mouvements.
	 * @param mouvementYeuxEvent évènement de mouvements
	 */
	@Handler
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
	 * @param mouvementYeuxEvent évènement de mouvements de cou
	 */
	@Handler
	public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
		System.out.println("YEUX : Event = " + mouvementCouEvent + ", Thread = " + Thread.currentThread().getName());
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
//	@Handler
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
		while (moteurOeilGauche.getPositionReelle() != POSITION_ZERO_MOTEUR_OEIL_GAUCHE 
				&& moteurOeilDroit.getPositionReelle() != POSITION_ZERO_MOTEUR_OEIL_DROIT)
		{ }
		moteurOeilGauche.stop();
		moteurOeilDroit.stop();
		moteurOeilGauche.setSpeedRampingState(true);
		moteurOeilDroit.setSpeedRampingState(true);
		moteurOeilGauche.setEngaged(false);
		moteurOeilDroit.setEngaged(false);
		moteurOeilGauche.close();
		moteurOeilDroit.close();
	}

	/** Remet les yeux à leur position par défaut. */
	private void reset() {
		moteurOeilDroit.setPositionCible(POSITION_ZERO_MOTEUR_OEIL_DROIT, null, null, false);
		moteurOeilGauche.setPositionCible(POSITION_ZERO_MOTEUR_OEIL_GAUCHE, null, null, false);
		roulisEnCours = false;
		positionRelativeOeilDroitDebutRoulis = 0;
		positionRelativeOeilGaucheDebutRoulis = 0;
		angleRoulis = 0;
	}
	
	/**
	 * Calcule la position absolue de l'oeil gauche à partir d'une position relative.
	 * @param positionRelative la position relative
	 * @param la position absolue de l'oeil gauche
	 */
	private static double toPositionAbsolueOeilGauche(double positionRelative) {
		return POSITION_ZERO_MOTEUR_OEIL_GAUCHE - positionRelative;
	}
	
	/**
	 * Calcule la position relative de l'oeil gauche à partir d'une position absolue.
	 * @param positionAbsolue la position absolue
	 * @param la position relative de l'oeil gauche
	 */
	private static double toPositionRelativeOeilGauche(double positionAbsolue) {
		return POSITION_ZERO_MOTEUR_OEIL_GAUCHE - positionAbsolue;
	}
	
	/**
	 * Calcule la position absolue de l'oeil droit à partir d'une position relative.
	 * @param positionRelative la position relative
	 * @param la position absolue de l'oeil droit
	 */
	private static double toPositionAbsolueOeilDroit(double positionRelative) {
		return POSITION_ZERO_MOTEUR_OEIL_DROIT - positionRelative;
	}
	
	/**
	 * Calcule la position relative de l'oeil droit à partir d'une position absolue.
	 * @param positionAbsolue la position absolue
	 * @param la position relative de l'oeil droit
	 */
	private static double toPositionRelativeOeilDroit(double positionAbsolue) {
		return POSITION_ZERO_MOTEUR_OEIL_DROIT - positionAbsolue;
	}

	/**
	 * Intercepte les évènements d'affichage de position.
	 * @param displayPositionEvent évènement
	 */
	@Handler
	public void handleDisplayPositionEvent(DisplayPositionEvent displayPositionEvent) {
		double positionRelativeOeilGauche = toPositionRelativeOeilGauche(moteurOeilGauche.getPositionReelle());
		double positionRelativeOeilDroit = toPositionRelativeOeilDroit(moteurOeilDroit.getPositionReelle());
		System.out.println("YEUX\tgauche = " + positionRelativeOeilGauche + "\tdroit = " + positionRelativeOeilDroit);
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
