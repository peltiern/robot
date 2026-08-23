package fr.roboteek.robot.systemenerveux.event;

/**
 * Une demande d'activité a été refusée par l'arbitrage.
 * <p>
 * Sans cette réponse, une demande refusée disparaissait sans laisser de trace : celui qui l'avait
 * faite n'en savait rien et ne la rejouait jamais, même une fois la cause du refus levée.
 */
public class DemandeActiviteRefuseeEvent extends RobotEvent {

    public static final String EVENT_TYPE = "demande-activite-refusee";

    /** Identifiant de l'activité réclamée, tel qu'il figurait dans la demande. */
    private final String idActivite;

    /** Le motif du refus, nommé par {@code ArbitrageActivites.Decision}. */
    private final String motif;

    /** Qui la demande refusée visait, repris tel quel de celle-ci ; {@code null} si personne. */
    private final String idPersonne;

    public DemandeActiviteRefuseeEvent(String idActivite, String motif, String idPersonne) {
        super(EVENT_TYPE);
        this.idActivite = idActivite;
        this.motif = motif;
        this.idPersonne = idPersonne;
    }

    public String getIdActivite() {
        return idActivite;
    }

    public String getMotif() {
        return motif;
    }

    public String getIdPersonne() {
        return idPersonne;
    }

}
