package fr.roboteek.robot.systemenerveux.event;

/**
 * Évènement d'arrêt d'urgence des moteurs.
 * <p>
 * Deux états, portés par le même évènement pour que tout le monde suive la même bascule :
 * <ul>
 *     <li>{@code actif = true} : <b>déclenchement</b>. Chaque organe à moteur coupe ses moteurs
 *     immédiatement, puis refuse tout ordre de mouvement.</li>
 *     <li>{@code actif = false} : <b>réarmement</b>. Rien ne bouge pour autant — les ordres sont
 *     simplement acceptés à nouveau.</li>
 * </ul>
 * À ne pas confondre avec {@link StopEvent}, qui éteint l'application entière.
 * <p>
 * Diffusé aux clients Websocket sur {@code /events/arret-urgence} comme tout
 * {@link RobotEvent} : l'interface suit l'état sans avoir à interroger le robot.
 *
 * @see fr.roboteek.robot.securite.ArretUrgence
 */
public class ArretUrgenceEvent extends RobotEvent {

    public static final String EVENT_TYPE = "arret-urgence";

    /** Vrai : arrêt d'urgence déclenché. Faux : réarmement. */
    private boolean actif;

    /**
     * Qui a déclenché (« manette », « interface », nom d'un futur chien de garde…). Sert aux
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
