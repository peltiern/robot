package fr.roboteek.robot.systemenerveux.event;

/**
 * Demande d'apprendre le visage qu'on a en face, et de le rattacher à une personne.
 * <p>
 * Passe par le système nerveux plutôt que par un appel direct, parce que l'empreinte
 * biométrique se calcule sur l'<b>image</b> : seul l'organe de vision l'a sous la main. La faire
 * voyager dans l'autre sens — publier les empreintes pour que d'autres les enregistrent —
 * mettrait 128 nombres flottants sur le bus dix fois par seconde, pour un besoin qui se présente
 * une fois par rencontre.
 * <p>
 * La réponse arrive par un {@link EnrolementTermineEvent}.
 */
public class DemandeEnrolementEvent extends RobotEvent {

    public static final String EVENT_TYPE = "demande-enrolement";

    /** Identifiant de la personne à qui rattacher les empreintes relevées. */
    private final String idPersonne;

    public DemandeEnrolementEvent(String idPersonne) {
        super(EVENT_TYPE);
        this.idPersonne = idPersonne;
    }

    public String getIdPersonne() {
        return idPersonne;
    }

}
