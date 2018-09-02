package fr.roboteek.robot.organes.actionneurs;

import com.phidgets.AdvancedServoPhidget;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_GAUCHE_DROITE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_HAUT_BAS;
import fr.roboteek.robot.util.phidget.MotorPositionChangeEvent;
import fr.roboteek.robot.util.phidget.MotorPositionChangeListener;
import fr.roboteek.robot.util.phidget.PhidgetMotor;
import fr.roboteek.robot.util.phidget.PhidgetServoController;
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
    private PhidgetMotor moteurGaucheDroite;
    
    /** Moteur Haut / Bas. */
    private PhidgetMotor moteurHautBas;
    
    /** Constructeur. */
    public Cou() {
        super();
        
        // Création et initialisation des moteurs
        moteurGaucheDroite = PhidgetServoController.getMotor(IDX_MOTEUR_GAUCHE_DROITE, AdvancedServoPhidget.PHIDGET_SERVO_HITEC_HS645MG);
        moteurHautBas = PhidgetServoController.getMotor(IDX_MOTEUR_HAUT_BAS, AdvancedServoPhidget.PHIDGET_SERVO_HITEC_HS645MG);
        
        moteurGaucheDroite.setEngaged(true);
        moteurGaucheDroite.setSpeedRampingOn(true);
        moteurGaucheDroite.setVelocityLimit(90);
        moteurGaucheDroite.setAcceleration(2000);
        moteurGaucheDroite.setPositionMin(0);
        moteurGaucheDroite.setPositionMax(180);
        
        moteurHautBas.setEngaged(true);
        moteurHautBas.setSpeedRampingOn(true);
        moteurHautBas.setVelocityLimit(90);
        moteurHautBas.setAcceleration(2000);
        moteurHautBas.setPositionMin(50);
        moteurHautBas.setPositionMax(120);
        
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
    public void positionnerTeteGaucheDroite(double position) {
    	if (position >= moteurGaucheDroite.getPositionMin() && position <= moteurGaucheDroite.getPositionMax()) {
    		moteurGaucheDroite.setPosition(position);
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
        moteurHautBas.backward();
    }
    
    /** Tourne la tête en haut sans s'arrêter. */
    public void tournerEnHaut() {
        moteurHautBas.forward();
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
    public void positionnerTeteHautBas(double position) {
    	if (position >= moteurHautBas.getPositionMin() && position <= moteurHautBas.getPositionMax()) {
    		moteurHautBas.setPosition(position);
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
    	System.out.println("Event = " + mouvementCouEvent);
    	if (mouvementCouEvent.getPositionGaucheDroite() != -1) {
    		positionnerTeteGaucheDroite(mouvementCouEvent.getPositionGaucheDroite());
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
    		positionnerTeteHautBas(mouvementCouEvent.getPositionHautBas());
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
        while (moteurGaucheDroite.getPosition() != POSITION_INITIALE_MOTEUR_GAUCHE_DROITE 
                && moteurHautBas.getPosition() != POSITION_INITIALE_MOTEUR_HAUT_BAS)
        { }
        moteurGaucheDroite.stop();
        moteurHautBas.stop();
        moteurGaucheDroite.setSpeedRampingOn(true);
        moteurHautBas.setSpeedRampingOn(true);
        moteurGaucheDroite.setEngaged(false);
        moteurHautBas.setEngaged(false);
        PhidgetServoController.getInstance().stopMotors();
    }
    
    /** Remet la tête à sa position par défaut. */
    private void reset() {
        moteurHautBas.setPosition(POSITION_INITIALE_MOTEUR_HAUT_BAS);
        moteurGaucheDroite.setPosition(POSITION_INITIALE_MOTEUR_GAUCHE_DROITE);
    }
    
    public void onPositionchanged(MotorPositionChangeEvent event) {
        if (event.getSource() == moteurGaucheDroite) {
            System.out.println("MOTEUR G-D : " + event.getPosition());
        } else if (event.getSource() == moteurHautBas) {
            System.out.println("MOTEUR H-B : " + event.getPosition());
        }
    }
    
    public static void main(String[] args) {
        Cou tete = new Cou();
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
    
    public double getPositionGaucheDroite() {
        return moteurGaucheDroite.getPosition();
    }
    
    public double getPositionHautBas() {
        return moteurHautBas.getPosition();
    }
    
}
