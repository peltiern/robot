package fr.roboteek.robot.systemenerveux.event;

/**
 * Relevé des encodeurs des deux chenilles, publié par la conduite tant qu'elles bougent.
 * <p>
 * Les compteurs sont <b>cumulés</b> et non des écarts : un relevé perdu ne fait pas perdre de
 * chemin, le suivant le rattrape. Ils sont déjà redressés, positifs en marche avant des deux
 * côtés, l'organe étant seul à savoir comment ses encodeurs sont câblés.
 */
public class EncodeursChenillesEvent extends RobotEvent {

    public static final String EVENT_TYPE = "encodeurs-chenilles";

    private final long topsGauche;

    private final long topsDroit;

    /**
     * Dernier relevé d'un mouvement : les chenilles viennent de s'immobiliser. C'est le moment de
     * faire le point, une fois par mouvement plutôt que vingt fois par seconde.
     */
    private final boolean arret;

    public EncodeursChenillesEvent(long topsGauche, long topsDroit, boolean arret) {
        super(EVENT_TYPE);
        this.topsGauche = topsGauche;
        this.topsDroit = topsDroit;
        this.arret = arret;
    }

    public long getTopsGauche() {
        return topsGauche;
    }

    public long getTopsDroit() {
        return topsDroit;
    }

    public boolean isArret() {
        return arret;
    }

    @Override
    public String toString() {
        return "EncodeursChenillesEvent{gauche=" + topsGauche + ", droit=" + topsDroit + ", arret=" + arret + '}';
    }
}
