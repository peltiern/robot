package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_HAUT_BAS;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.phidget.MotorPositionChangeEvent;
import fr.roboteek.robot.util.phidget.MotorPositionChangeListener;
import fr.roboteek.robot.util.phidget.PhidgetMotor2;
import net.engio.mbassy.listener.Handler;

/**
 * Classe représentant le cou du robot.
 * @author Java Developer
 */
public class Cou extends AbstractOrgane implements MotorPositionChangeListener {

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
		moteurGaucheDroite = new PhidgetMotor2(IDX_MOTEUR_GAUCHE_DROITE, POSITION_INITIALE_MOTEUR_GAUCHE_DROITE, 30, 150);
		moteurHautBas = new PhidgetMotor2(IDX_MOTEUR_HAUT_BAS, POSITION_INITIALE_MOTEUR_HAUT_BAS, 50, 130);

		moteurGaucheDroite.setEngaged(true);
		moteurGaucheDroite.setSpeedRampingState(true);
		moteurGaucheDroite.setVelocityLimit(180);
		moteurGaucheDroite.setAcceleration(150);

		moteurHautBas.setEngaged(true);
		moteurHautBas.setSpeedRampingState(true);
		moteurHautBas.setVelocityLimit(180);
		moteurHautBas.setAcceleration(150);

		// Ajout des listeners sur les moteurs
		moteurGaucheDroite.addMotorPositionChangeListener(this);
	}

	@Override
	public void initialiser() {
		reset();
	}

	/** Tourne la tête à gauche sans s'arrêter. */
	public void tournerAGauche() {
		moteurGaucheDroite.backward();
	}

	/** Tourne la tête à droite sans s'arrêter. */
	public void tournerADroite() {
		moteurGaucheDroite.forward();
	}

	/**
	 * Tourne la tête sur le plan "Gauche / Droite" d'un certain angle.
	 * @param angle angle en degrés (négatif : à droite, positif : à gauche)
	 */
	public void tournerTeteGaucheDroite(double angle) {
		moteurGaucheDroite.rotate(angle);
	}

	/**
	 * Tourne la tête à gauche d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerAGauche(double angle) {
		tournerTeteGaucheDroite(-angle);
	}

	/**
	 * Tourne la tête à droite d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerADroite(double angle) {
		tournerTeteGaucheDroite(angle);
	}

	/**
	 * Positionne la tête sur le plan "Gauche / Droite" à une position précise (0 : à gauche, 180 : à droite).
	 * @param position position en degrés (0 : à gauche, 180 : à droite)
	 */
	public void positionnerTeteGaucheDroite(double position, boolean waitForPosition) {
		double positionMoteur = POSITION_INITIALE_MOTEUR_GAUCHE_DROITE - position;
		if (positionMoteur >= moteurGaucheDroite.getPositionMin() && positionMoteur <= moteurGaucheDroite.getPositionMax()) {
			System.out.println("POS_GD = " + positionMoteur);
			moteurGaucheDroite.setPositionCible(positionMoteur);
		}
	}

	/**
	 * Stoppe le mouvement de la tête sur le plan "Gauche / Droite".
	 */
	public void stopperTeteGaucheDroite() {
		moteurGaucheDroite.stop();
	}

	/** Tourne la tête en bas sans s'arrêter. */
	public void tournerEnBas() {
		moteurHautBas.forward();
	}

	/** Tourne la tête en haut sans s'arrêter. */
	public void tournerEnHaut() {
		moteurHautBas.backward();
	}

	/**
	 * Tourne la tête sur le plan "Haut / Bas" d'un certain angle.
	 * @param angle angle en degrés (négatif : en bas, positif : en haut)
	 */
	public void tournerTeteHautBas(double angle) {
		moteurHautBas.rotate(angle);
	}

	/**
	 * Tourne la tête en bas d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerEnBas(double angle) {
		tournerTeteHautBas(-angle);
	}

	/**
	 * Tourne la tête en haut d'un certain angle.
	 * @param angle angle en degrés
	 */
	public void tournerEnHaut(double angle) {
		tournerTeteHautBas(angle);
	}

	/**
	 * Positionne la tête sur le plan "Haut / Bas" à une position précise (0 : en bas, 180 : en haut).
	 * @param position position en degrés (0 : en bas, 180 : en haut)
	 */
	public void positionnerTeteHautBas(double position, boolean waitForPosition) {
		double positionMoteur = POSITION_INITIALE_MOTEUR_HAUT_BAS - position;
		if (positionMoteur >= moteurHautBas.getPositionMin() && positionMoteur <= moteurHautBas.getPositionMax()) {
			System.out.println("POS_HB = " + positionMoteur);
			moteurHautBas.setPositionCible(positionMoteur);
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
		if (mouvementCouEvent.getPositionGaucheDroite() != -1) {
			positionnerTeteGaucheDroite(mouvementCouEvent.getPositionGaucheDroite(), false);
		} else if (mouvementCouEvent.getMouvementGaucheDroite() != null) {
			if (mouvementCouEvent.getMouvementGaucheDroite() == MOUVEMENTS_GAUCHE_DROITE.STOPPER) {
				stopperTeteGaucheDroite();
			} else if (mouvementCouEvent.getMouvementGaucheDroite() == MOUVEMENTS_GAUCHE_DROITE.TOURNER_GAUCHE) {
				tournerAGauche();
			} else if (mouvementCouEvent.getMouvementGaucheDroite() == MOUVEMENTS_GAUCHE_DROITE.TOURNER_DROITE) {
				tournerADroite();
			}
		}
		if (mouvementCouEvent.getPositionHautBas() != -1) {
			positionnerTeteHautBas(mouvementCouEvent.getPositionHautBas(), false);
		} else if (mouvementCouEvent.getMouvementHauBas() != null) {
			if (mouvementCouEvent.getMouvementHauBas() == MOUVEMENTS_HAUT_BAS.STOPPER) {
				stopperTeteHautBas();
			} else if (mouvementCouEvent.getMouvementHauBas() == MOUVEMENTS_HAUT_BAS.TOURNER_HAUT) {
				tournerEnHaut();
			} else if (mouvementCouEvent.getMouvementHauBas() == MOUVEMENTS_HAUT_BAS.TOURNER_BAS) {
				tournerEnBas();
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
		moteurHautBas.setPositionCible(POSITION_INITIALE_MOTEUR_HAUT_BAS);
		moteurGaucheDroite.setPositionCible(POSITION_INITIALE_MOTEUR_GAUCHE_DROITE);
	}

	public void onPositionchanged(MotorPositionChangeEvent event) {
		if (event.getSource() == moteurGaucheDroite) {
			//System.out.println("MOTEUR G-D : " + event.getPosition());
		} else if (event.getSource() == moteurHautBas) {
			//System.out.println("MOTEUR H-B : " + event.getPosition());
		}
	}
	
	public void testMouvement() {
		int pas = 30;
		System.out.println("====== PAS 30");
		for (int angle = -60; angle <= 60; angle += pas) {
			positionnerTeteGaucheDroite(angle, true);
		}
		pas = 5;
		System.out.println("====== PAS 5");
		for (int angle = -60; angle <= 60; angle += pas) {
			positionnerTeteGaucheDroite(angle, true);
		}
		pas = 1;
		System.out.println("====== PAS 1");
		for (int angle = -60; angle <= 60; angle += pas) {
			positionnerTeteGaucheDroite(angle, true);
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
			cou.testMouvement();
//			MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(0);
//			mouvementCouEvent.setPositionHautBas(30);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(15);
//			mouvementCouEvent.setPositionHautBas(15);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(30);
//			mouvementCouEvent.setPositionHautBas(0);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(15);
//			mouvementCouEvent.setPositionHautBas(-15);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(0);
//			mouvementCouEvent.setPositionHautBas(-30);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(-15);
//			mouvementCouEvent.setPositionHautBas(-15);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(-30);
//			mouvementCouEvent.setPositionHautBas(0);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(-15);
//			mouvementCouEvent.setPositionHautBas(15);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
//			mouvementCouEvent = new MouvementCouEvent();
//			mouvementCouEvent.setPositionGaucheDroite(0);
//			mouvementCouEvent.setPositionHautBas(30);
//			RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//			Thread.sleep(temps);
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
