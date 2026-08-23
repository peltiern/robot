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

    /**
     * Constructeur vide exigé par la désérialisation : cet évènement arrive aussi de l'interface,
     * sur {@code /app/robotevents}. Le supprimer laisse Gson allouer l'objet sans passer par aucun
     * constructeur — l'évènement entre alors <b>sans date</b>, l'interface n'en envoyant pas, et
     * rien ne compile en rouge. Voir {@code EvenementsEntrantsTest}.
     */
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

    public String getOrigine() {
        return origine;
    }

    @Override
    public String toString() {
        return "ArretUrgenceEvent{actif=" + actif + ", origine='" + origine + "'}";
    }
}
