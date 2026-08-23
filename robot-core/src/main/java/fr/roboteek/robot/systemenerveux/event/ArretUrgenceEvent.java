package fr.roboteek.robot.systemenerveux.event;

/**
 * Évènement d'arrêt d'urgence des moteurs, porteur des deux bascules : {@code actif = true} coupe
 * les moteurs et fait refuser tout ordre de mouvement, {@code actif = false} réarme — sans rien
 * remettre en marche, les ordres sont simplement acceptés à nouveau.
 * <p>
 * À ne pas confondre avec {@link StopEvent}, qui éteint l'application entière.
 *
 * @see fr.roboteek.robot.securite.ArretUrgence
 */
public class ArretUrgenceEvent extends RobotEvent {

    public static final String EVENT_TYPE = "arret-urgence";

    /** Vrai : arrêt d'urgence déclenché. Faux : réarmement. */
    private boolean actif;

    /**
     * Qui a déclenché (« manette », « interface », nom d'un futur watchdog…). Sert aux
     * journaux et à l'affichage : quand le robot se fige, la première question est « qui l'a
     * arrêté ? ».
     */
    private String origine;

    public ArretUrgenceEvent() {
        super(EVENT_TYPE);
    }

    public ArretUrgenceEvent(boolean actif, String origine) {
        super(EVENT_TYPE);
        this.actif = actif;
        this.origine = origine;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    @Override
    public String toString() {
        return "ArretUrgenceEvent{actif=" + actif + ", origine='" + origine + "'}";
    }
}
