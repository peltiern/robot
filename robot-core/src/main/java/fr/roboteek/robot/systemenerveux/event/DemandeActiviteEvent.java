package fr.roboteek.robot.systemenerveux.event;

/**
 * Demande de changement d'activité adressée au cerveau. C'est par là que passe tout ce qui peut
 * faire changer le robot d'occupation : la voix, un visage qui apparaît, un bouton de l'interface.
 * <p>
 * L'activité est désignée par son identifiant ({@code AbstractActivity.identifiant()}, soit le nom
 * simple de sa classe) et non par une référence : l'évènement est rediffusé sur le WebSocket, où
 * un bean n'a rien à faire.
 */
public class DemandeActiviteEvent extends RobotEvent {

    public static final String EVENT_TYPE = "demande-activite";

    /** Identifiant de l'activité demandée. */
    private String idActivite;

    /**
     * Constructeur vide exigé par la désérialisation : cet évènement arrive aussi de l'interface,
     * sur {@code /app/robotevents}. Le supprimer laisse Gson allouer l'objet sans passer par aucun
     * constructeur — l'évènement entre alors <b>sans date</b>, l'interface n'en envoyant pas, et
     * rien ne compile en rouge. Voir {@code EvenementsEntrantsTest}.
     */
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

}
