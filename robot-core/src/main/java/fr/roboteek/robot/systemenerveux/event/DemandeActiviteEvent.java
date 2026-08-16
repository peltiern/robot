package fr.roboteek.robot.systemenerveux.event;

/**
 * Demande de changement d'activité adressée au cerveau.
 * <p>
 * Jusqu'ici, le seul déclencheur d'activité était la voix, câblé en dur dans le cerveau :
 * rien d'autre — un visage qui apparaît, un bouton de l'interface — ne pouvait faire changer
 * le robot d'occupation. Cet évènement ouvre cette porte.
 * <p>
 * L'activité est désignée par son identifiant ({@code AbstractActivity.identifiant()}, soit
 * le nom simple de sa classe) et non par une référence : l'évènement est aussi rediffusé sur
 * le WebSocket, où un bean n'a rien à faire.
 */
public class DemandeActiviteEvent extends RobotEvent {

    public static final String EVENT_TYPE = "demande-activite";

    /** Identifiant de l'activité demandée. */
    private String idActivite;

    public DemandeActiviteEvent() {
        super(EVENT_TYPE);
    }

    public DemandeActiviteEvent(String idActivite) {
        super(EVENT_TYPE);
        this.idActivite = idActivite;
    }

    public String getIdActivite() {
        return idActivite;
    }

    public void setIdActivite(String idActivite) {
        this.idActivite = idActivite;
    }
}
