package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_HAUT_BAS;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.phidget.MotorPositionChangeEvent;
import fr.roboteek.robot.util.phidget.PhidgetMotor2;
import net.engio.mbassy.listener.Handler;

/**
 * Classe représentant le cou du robot.
 * @author Java Developer
 */
public class Cou extends AbstractOrgane {

	/** Index du moteur Gauche / Droite sur le servo-contrôleur. */
	private static final int IDX_MOTEUR_GAUCHE_DROITE = 0;

	/** Index du moteur Haut / Bas sur le servo-contrôleur. */
	private static final int IDX_MOTEUR_HAUT_BAS = 1;

	/** Position initiale du moteur Gauche / Droite. */
	private static final double POSITION_INITIALE_MOTEUR_GAUCHE_DROITE = 90;

	/** Position initiale du moteur Haut / Bas. */
	private static final double POSITION_INITIALE_MOTEUR_HAUT_BAS = 90;

	/** Moteur Gauche / Droite. */
	private PhidgetMotor2 moteurGaucheDroite;

	/** Moteur Haut / Bas. */
	private PhidgetMotor2 moteurHautBas;

	/** Constructeur. */
	public Cou() {
		super();

		System.out.println("COU :, Thread = " + Thread.currentThread().getName());

		// Création et initialisation des moteurs
		moteurGaucheDroite = new PhidgetMotor2(IDX_MOTEUR_GAUCHE_DROITE, POSITION_INITIALE_MOTEUR_GAUCHE_DROITE, 30, 150, 180, 150);
		moteurHautBas = new PhidgetMotor2(IDX_MOTEUR_HAUT_BAS, POSITION_INITIALE_MOTEUR_HAUT_BAS, 50, 130, 180, 150);

		moteurGaucheDroite.setEngaged(true);
		moteurGaucheDroite.setSpeedRampingState(true);

		moteurHautBas.setEngaged(true);
		moteurHautBas.setSpeedRampingState(true);
	}

	@Override
	public void initialiser() {
		reset();
	}

	/** Tourne la tête à gauche sans s'arrêter. */
	public void tournerAGauche(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurGaucheDroite.backward(vitesse, acceleration, waitForPosition);
	}

	/** Tourne la tête à droite sans s'arrêter. */
	public void tournerADroite(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurGaucheDroite.forward(vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne la tête sur le plan "Gauche / Droite" d'un certain angle.
	 * @param angle angle en degrés (négatif : à droite, positif : à gauche)
	 */
	public void tournerTeteGaucheDroite(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurGaucheDroite.rotate(angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne la tête à gauche d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerAGauche(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		tournerTeteGaucheDroite(-angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne la tête à droite d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerADroite(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		tournerTeteGaucheDroite(angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Positionne la tête sur le plan "Gauche / Droite" à une position précise (0 : à gauche, 180 : à droite).
	 * @param position position en degrés (0 : à gauche, 180 : à droite)
	 */
	public void positionnerTeteGaucheDroite(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
		double positionMoteur = POSITION_INITIALE_MOTEUR_GAUCHE_DROITE - position;
		if (positionMoteur >= moteurGaucheDroite.getPositionMin() && positionMoteur <= moteurGaucheDroite.getPositionMax()) {
			System.out.println("POS_GD = " + positionMoteur);
			moteurGaucheDroite.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
		}
	}

	/**
	 * Stoppe le mouvement de la tête sur le plan "Gauche / Droite".
	 */
	public void stopperTeteGaucheDroite() {
		moteurGaucheDroite.stop();
	}

	/** Tourne la tête en bas sans s'arrêter. */
	public void tournerEnBas(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurHautBas.forward(vitesse, acceleration, waitForPosition);
	}

	/** Tourne la tête en haut sans s'arrêter. */
	public void tournerEnHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurHautBas.backward(vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne la tête sur le plan "Haut / Bas" d'un certain angle.
	 * @param angle angle en degrés (négatif : en bas, positif : en haut)
	 */
	public void tournerTeteHautBas(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		moteurHautBas.rotate(angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne la tête en bas d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerEnBas(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		tournerTeteHautBas(-angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Tourne la tête en haut d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerEnHaut(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
		tournerTeteHautBas(angle, vitesse, acceleration, waitForPosition);
	}

	/**
	 * Positionne la tête sur le plan "Haut / Bas" à une position précise (0 : en bas, 180 : en haut).
	 * @param position position en degrés (0 : en bas, 180 : en haut)
	 */
	public void positionnerTeteHautBas(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
		double positionMoteur = POSITION_INITIALE_MOTEUR_HAUT_BAS - position;
		if (positionMoteur >= moteurHautBas.getPositionMin() && positionMoteur <= moteurHautBas.getPositionMax()) {
			System.out.println("POS_HB = " + positionMoteur);
			moteurHautBas.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
		}
	}

	/**
	 * Stoppe le mouvement de la tête sur le plan "Haut / Bas".
	 */
	public void stopperTeteHautBas() {
		moteurHautBas.stop();
	}

	/**
	 * Intercepte les évènements de mouvements.
	 * @param mouvementCouEvent évènement de mouvements
	 */
	@Handler
	public void handleMouvementTeteEvent(MouvementCouEvent mouvementCouEvent) {
		System.out.println("COU : Event = " + mouvementCouEvent + ", Thread = " + Thread.currentThread().getName());
		if (mouvementCouEvent.getPositionGaucheDroite() != MouvementCouEvent.POSITION_NEUTRE) {
			positionnerTeteGaucheDroite(mouvementCouEvent.getPositionGaucheDroite(), mouvementCouEvent.getVitesseGaucheDroite(), mouvementCouEvent.getAccelerationGaucheDroite(), mouvementCouEvent.isSynchrone());
		} else if (mouvementCouEvent.getMouvementGaucheDroite() != null) {
			if (mouvementCouEvent.getMouvementGaucheDroite() == MOUVEMENTS_GAUCHE_DROITE.STOPPER) {
				stopperTeteGaucheDroite();
			} else if (mouvementCouEvent.getMouvementGaucheDroite() == MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE) {
				tournerAGauche(mouvementCouEvent.getVitesseGaucheDroite(), mouvementCouEvent.getAccelerationGaucheDroite(), mouvementCouEvent.isSynchrone());
			} else if (mouvementCouEvent.getMouvementGaucheDroite() == MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE) {
				tournerADroite(mouvementCouEvent.getVitesseGaucheDroite(), mouvementCouEvent.getAccelerationGaucheDroite(), mouvementCouEvent.isSynchrone());
			}
		}
		if (mouvementCouEvent.getPositionHautBas() != MouvementCouEvent.POSITION_NEUTRE) {
			positionnerTeteHautBas(mouvementCouEvent.getPositionHautBas(), mouvementCouEvent.getVitesseHautBas(), mouvementCouEvent.getAccelerationHautBas(), mouvementCouEvent.isSynchrone());
		} else if (mouvementCouEvent.getMouvementHauBas() != null) {
			if (mouvementCouEvent.getMouvementHauBas() == MOUVEMENTS_HAUT_BAS.STOPPER) {
				stopperTeteHautBas();
			} else if (mouvementCouEvent.getMouvementHauBas() == MOUVEMENTS_HAUT_BAS.TOURNER_HAUT) {
				tournerEnHaut(mouvementCouEvent.getVitesseHautBas(), mouvementCouEvent.getAccelerationHautBas(), mouvementCouEvent.isSynchrone());
			} else if (mouvementCouEvent.getMouvementHauBas() == MOUVEMENTS_HAUT_BAS.TOURNER_BAS) {
				tournerEnBas(mouvementCouEvent.getVitesseHautBas(), mouvementCouEvent.getAccelerationHautBas(), mouvementCouEvent.isSynchrone());
			}
		}
	}

	@Override
	public void arreter() {
		reset();
		// Attente du retour à la position initiale
		while (moteurGaucheDroite.getPositionReelle() != POSITION_INITIALE_MOTEUR_GAUCHE_DROITE 
				&& moteurHautBas.getPositionReelle() != POSITION_INITIALE_MOTEUR_HAUT_BAS)
		{ }
		moteurGaucheDroite.stop();
		moteurHautBas.stop();
		moteurGaucheDroite.setSpeedRampingState(true);
		moteurHautBas.setSpeedRampingState(true);
		moteurGaucheDroite.setEngaged(false);
		moteurHautBas.setEngaged(false);
	}

	/** Remet la tête à sa position par défaut. */
	private void reset() {
		moteurHautBas.setPositionCible(POSITION_INITIALE_MOTEUR_HAUT_BAS, null, null, false);
		moteurGaucheDroite.setPositionCible(POSITION_INITIALE_MOTEUR_GAUCHE_DROITE, null, null, false);
	}

	public void onPositionchanged(MotorPositionChangeEvent event) {
		if (event.getSource() == moteurGaucheDroite) {
			System.out.println("MOTEUR G-D : " + event.getPosition() + ", PR = " + moteurGaucheDroite.getPositionReelle() + ", temps = " + System.currentTimeMillis());
		} else if (event.getSource() == moteurHautBas) {
			System.out.println("MOTEUR H-B : " + event.getPosition() + ", PR = " + moteurHautBas.getPositionReelle() + ", temps = " + System.currentTimeMillis());
		}
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
			mouvementCouEvent.setPositionGaucheDroite(0);
			mouvementCouEvent.setPositionHautBas(0);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(-30);
			mouvementCouEvent.setVitesseGaucheDroite(300D);
			mouvementCouEvent.setPositionHautBas(0);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(30);
			mouvementCouEvent.setVitesseGaucheDroite(300D);
			mouvementCouEvent.setPositionHautBas(0);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(-30);
			mouvementCouEvent.setVitesseGaucheDroite(800D);
			mouvementCouEvent.setAccelerationGaucheDroite(400D);
			mouvementCouEvent.setPositionHautBas(0);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(30);
			mouvementCouEvent.setVitesseGaucheDroite(800D);
			mouvementCouEvent.setAccelerationGaucheDroite(400D);
			mouvementCouEvent.setPositionHautBas(0);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(0);
			mouvementCouEvent.setPositionHautBas(30);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(30);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionHautBas(-30);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(-30);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionHautBas(30);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionGaucheDroite(0);
			mouvementCouEvent.setSynchrone(true);
			RobotEventBus.getInstance().publish(mouvementCouEvent);
			mouvementCouEvent = new MouvementCouEvent();
			mouvementCouEvent.setPositionHautBas(0);
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
