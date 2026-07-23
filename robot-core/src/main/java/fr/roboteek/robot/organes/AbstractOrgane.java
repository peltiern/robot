package fr.roboteek.robot.organes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Classe abstraite représentant un organe.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class AbstractOrgane {

    /**
     * Publication des évènements du système nerveux (injecté par Spring —
     * n'est renseigné que si l'organe est un bean Spring).
     */
    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    /**
     * Constructeur.
     */
    public AbstractOrgane() {
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
