package fr.roboteek.robot.organes.actionneurs;

import com.phidgets.AdvancedServoPhidget;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent.MOUVEMENTS_OEIL;
import fr.roboteek.robot.util.phidget.MotorPositionChangeEvent;
import fr.roboteek.robot.util.phidget.MotorPositionChangeListener;
import fr.roboteek.robot.util.phidget.PhidgetMotor;
import fr.roboteek.robot.util.phidget.PhidgetServoController;
import net.engio.mbassy.listener.Handler;

/**
 * Classe représentant les yeux du robot.
 * @author Java Developer
 */
public class Yeux extends AbstractOrgane implements MotorPositionChangeListener {
    
    /** Index du moteur de l'oeil gauche sur le servo-contrôleur. */
    private static final int IDX_MOTEUR_OEIL_GAUCHE = 2;
    
    /** Index du moteur de l'oeil droit sur le servo-contrôleur. */
    private static final int IDX_MOTEUR_OEIL_DROIT = 3;
    
    /** Position initiale du moteur de l'oeil gauche. */
    private static final double POSITION_INITIALE_MOTEUR_OEIL_GAUCHE = 110;
    
    /** Position initiale du moteur de l'oeil droit. */
    private static final double POSITION_INITIALE_MOTEUR_OEIL_DROIT = 75;

    /** Moteur Gauche / Droite. */
    private PhidgetMotor moteurOeilGauche;
    
    /** Moteur Haut / Bas. */
    private PhidgetMotor moteurOeilDroit;
    
    /** Constructeur. */
    public Yeux() {
        super();
        
        // Création et initialisation des moteurs
        moteurOeilGauche = PhidgetServoController.getMotor(IDX_MOTEUR_OEIL_GAUCHE, AdvancedServoPhidget.PHIDGET_SERVO_HITEC_HS422);
        moteurOeilDroit = PhidgetServoController.getMotor(IDX_MOTEUR_OEIL_DROIT, AdvancedServoPhidget.PHIDGET_SERVO_HITEC_HS422);
        
        moteurOeilGauche.setEngaged(true);
        moteurOeilGauche.setSpeedRampingOn(true);
        moteurOeilGauche.setVelocityLimit(200);
        moteurOeilGauche.setAcceleration(2000);
        moteurOeilGauche.setPositionMin(65);
        moteurOeilGauche.setPositionMax(125);
        
        moteurOeilDroit.setEngaged(true);
        moteurOeilDroit.setSpeedRampingOn(true);
        moteurOeilDroit.setVelocityLimit(200);
        moteurOeilDroit.setAcceleration(2000);
        moteurOeilDroit.setPositionMin(60);
        moteurOeilDroit.setPositionMax(120);
        
        // Ajout des listeners sur les moteurs
        moteurOeilGauche.addMotorPositionChangeListener(this);
        moteurOeilDroit.addMotorPositionChangeListener(this);
    }
    
    @Override
    public void initialiser() {
        reset();
    }
    
    /** Tourne l'oeil gauche vers le bas sans s'arrêter. */
    public void tournerOeilGaucheVersBas() {
        moteurOeilGauche.backward();
    }
    
    /** Tourne l'oeil gauche vers le haut sans s'arrêter. */
    public void tournerOeilGaucheVersHaut() {
        moteurOeilGauche.forward();
    }
    
    /**
     * Tourne l'oeil gauche d'un certain angle.
     * @param angle angle en degrés (négatif : à droite, positif : à gauche)
     */
    public void tournerOeilGauche(double angle) {
        moteurOeilGauche.rotate(angle);
    }
    
    /**
     * Positionne l'oeil gauche à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     * @param position position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilGauche(double position) {
    	double positionMoteur = POSITION_INITIALE_MOTEUR_OEIL_GAUCHE + position;
    	if (positionMoteur >= moteurOeilGauche.getPositionMin() && positionMoteur <= moteurOeilGauche.getPositionMax()) {
    		moteurOeilGauche.setPosition(positionMoteur);
    	}
    }
    
    /**
     * Stoppe le mouvement de l'oeil gauche.
     */
    public void stopperOeilGauche() {
        moteurOeilGauche.stop();
    }
    
    /** Tourne l'oeil droit vers le bas sans s'arrêter. */
    public void tournerOeilDroitVersBas() {
        moteurOeilDroit.forward();
    }
    
    /** Tourne l'oeil droit vers le haut sans s'arrêter. */
    public void tournerOeilDroitVersHaut() {
        moteurOeilDroit.backward();
    }
    
    /**
     * Tourne l'oeil droit d'un certain angle.
     * @param angle angle en degrés (négatif : en bas, positif : en haut)
     */
    public void tournerOeilDroit(double angle) {
        moteurOeilDroit.rotate(-angle);
    }
    
    /**
     * Positionne l'oeil droit à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     * @param position position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilDroit(double position) {
    	double positionMoteur = POSITION_INITIALE_MOTEUR_OEIL_DROIT - position;
    	if (positionMoteur >= moteurOeilDroit.getPositionMin() && positionMoteur <= moteurOeilDroit.getPositionMax()) {
    		moteurOeilDroit.setPosition(positionMoteur);
    	}
    }
    
    /**
     * Stoppe le mouvement de l'oeil droit.
     */
    public void stopperOeilDroit() {
        moteurOeilDroit.stop();
    }
    
    
    /**
     * Intercepte les évènements de mouvements.
     * @param mouvementYeuxEvent évènement de mouvements
     */
    @Handler
    public void handleMouvementYeuxEvent(MouvementYeuxEvent mouvementYeuxEvent) {
    	System.out.println("Event = " + mouvementYeuxEvent);
    	if (mouvementYeuxEvent.getPositionOeilGauche() != -1) {
    		positionnerOeilGauche(mouvementYeuxEvent.getPositionOeilGauche());
    	} else if (mouvementYeuxEvent.getMouvementOeilGauche() != null) {
            if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.STOPPER) {
                stopperOeilGauche();
            } else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_BAS) {
                tournerOeilGaucheVersBas();
            } else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
                tournerOeilGaucheVersHaut();
            }
        }
    	if (mouvementYeuxEvent.getPositionOeilDroit() != -1) {
    		positionnerOeilDroit(mouvementYeuxEvent.getPositionOeilDroit());
    	} else if (mouvementYeuxEvent.getMouvementOeilDroit() != null) {
            if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.STOPPER) {
                stopperOeilDroit();
            } else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_BAS) {
                tournerOeilDroitVersBas();
            } else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
                tournerOeilDroitVersHaut();
            }
        }
    }

    @Override
    public void arreter() {
        reset();
        // Attente du retour à la position initiale
        while (moteurOeilGauche.getPosition() != POSITION_INITIALE_MOTEUR_OEIL_GAUCHE 
                && moteurOeilDroit.getPosition() != POSITION_INITIALE_MOTEUR_OEIL_DROIT)
        { }
        moteurOeilGauche.stop();
        moteurOeilDroit.stop();
        moteurOeilGauche.setSpeedRampingOn(true);
        moteurOeilDroit.setSpeedRampingOn(true);
        moteurOeilGauche.setEngaged(false);
        moteurOeilDroit.setEngaged(false);
        PhidgetServoController.getInstance().stopMotors();
    }
    
    /** Remet les yeux à leur position par défaut. */
    private void reset() {
        moteurOeilDroit.setPosition(POSITION_INITIALE_MOTEUR_OEIL_DROIT);
        moteurOeilGauche.setPosition(POSITION_INITIALE_MOTEUR_OEIL_GAUCHE);
    }
    
    public void onPositionchanged(MotorPositionChangeEvent event) {
        if (event.getSource() == moteurOeilGauche) {
            System.out.println("MOTEUR O G : " + event.getPosition());
        } else if (event.getSource() == moteurOeilDroit) {
            System.out.println("MOTEUR O D : " + event.getPosition());
        }
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
//        tete.arreter();
        while(true) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
