package fr.roboteek.robot.organes;

import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import net.engio.mbassy.bus.MBassador;

/**
 * Classe abstraite représentant un organe.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class AbstractOrgane {
    
    /** Système nerveux (bus d'évènements). */
    protected MBassador<RobotEvent> systemeNerveux;
    
    /**
     * Constructeur.
     * @param systemeNerveux le système nerveux
     */
    public AbstractOrgane(MBassador<RobotEvent> systemeNerveux) {
        this.systemeNerveux = systemeNerveux;
    }
    
    /**
     * Initialise l'organe avant abonnement aux évènements du système nerveux.
     * Permet, par exemple, de charger des données, initialiser des valeurs, ...
     */
    public abstract void initialiser();
    
    /**
     * Arrête l'organe.
     * Permet au robot d'arrêter proprement l'actionneur après désabonnement aux évènements du système nerveux.
     */
    public abstract void arreter();

}
