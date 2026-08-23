package fr.roboteek.robot.systemenerveux.event;

/**
 * Résultat d'un {@link DemandeEnrolementEvent} : le visage est appris, ou il ne l'est pas.
 * <p>
 * L'échec est un cas ordinaire, pas une anomalie : la personne peut s'être détournée, être
 * repartie, ou la lumière peut ne pas suffire. Celui qui a demandé l'enrôlement doit pouvoir le
 * dire à voix haute plutôt que d'affirmer se souvenir de quelqu'un qu'il ne reconnaîtra pas.
 */
public class EnrolementTermineEvent extends RobotEvent {

    public static final String EVENT_TYPE = "enrolement-termine";

    /** Identifiant de la personne concernée, tel que demandé. */
    private final String idPersonne;

    /** Nombre d'empreintes effectivement enregistrées. */
    private final int nombreEmpreintes;

    /** Vrai si le visage est désormais reconnaissable. */
    private final boolean reussi;

    public EnrolementTermineEvent(String idPersonne, int nombreEmpreintes, boolean reussi) {
        super(EVENT_TYPE);
        this.idPersonne = idPersonne;
        this.nombreEmpreintes = nombreEmpreintes;
        this.reussi = reussi;
    }

    public String getIdPersonne() {
        return idPersonne;
    }

    public int getNombreEmpreintes() {
        return nombreEmpreintes;
    }

    public boolean isReussi() {
        return reussi;
    }

}
