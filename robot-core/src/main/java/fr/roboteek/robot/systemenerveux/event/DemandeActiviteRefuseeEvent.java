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
    private String idActivite;

    /** Le motif du refus, nommé par {@code ArbitrageActivites.Decision}. */
    private String motif;

    public DemandeActiviteRefuseeEvent() {
        super(EVENT_TYPE);
    }

    public DemandeActiviteRefuseeEvent(String idActivite, String motif) {
        super(EVENT_TYPE);
        this.idActivite = idActivite;
        this.motif = motif;
    }

    public String getIdActivite() {
        return idActivite;
    }

    public void setIdActivite(String idActivite) {
        this.idActivite = idActivite;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
