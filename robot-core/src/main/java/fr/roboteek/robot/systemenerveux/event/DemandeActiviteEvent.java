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
     * Qui la demande vise, {@code null} quand elle ne vise personne.
     * <p>
     * Un identifiant et non une {@code Personne} : l'évènement est rediffusé sur le WebSocket, où
     * un bean n'a rien à faire. Il suffit à l'activité pour retrouver ce qu'on lui a confié.
     */
    private String idPersonne;

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
        this(idActivite, null);
    }

    public DemandeActiviteEvent(String idActivite, String idPersonne) {
        super(EVENT_TYPE);
        this.idActivite = idActivite;
        this.idPersonne = idPersonne;
    }

    public String getIdActivite() {
        return idActivite;
    }

    public String getIdPersonne() {
        return idPersonne;
    }

}
