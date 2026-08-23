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
     * Horodatage du dernier signe de vie de l'organe (voir {@link #battement()}).
     * Volatile : écrit par le thread de l'organe, lu par celui du watchdog.
     */
    private volatile long dernierBattement = 0L;

    /** Constructeur. */
    public AbstractOrgane() {
    }

    /**
     * Déclare un signe de vie. À appeler depuis le <b>travail réel</b> de l'organe — un tour de
     * boucle abouti, une lecture de position qui a répondu — et jamais depuis son cycle de vie :
     * tout l'intérêt est d'attester que l'organe fonctionne encore, là où {@code isRunning()} reste
     * à {@code true} même quand la boucle est morte sur une exception.
     * <p>
     * Une seule écriture de {@code long} : assez léger pour être appelé à chaque bloc audio ou à
     * chaque tour de scrutation. Lu par {@code RegistreSante}, voir
     * {@code fr.roboteek.robot.securite.OrganeSurveille}.
     */
    protected void battement() {
        dernierBattement = System.currentTimeMillis();
    }

    /**
     * Horodatage du dernier battement, ou {@code 0} si l'organe n'a encore jamais donné signe de
     * vie. Implémente {@code OrganeSurveille.dernierBattement()} pour tous les organes qui héritent
     * de cette classe.
     */
    public long dernierBattement() {
        return dernierBattement;
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
