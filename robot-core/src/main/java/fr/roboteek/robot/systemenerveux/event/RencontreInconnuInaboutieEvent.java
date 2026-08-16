package fr.roboteek.robot.systemenerveux.event;

/**
 * La rencontre annoncée pour un inconnu n'a mené à rien : son tour lui est rendu.
 * <p>
 * Une rencontre d'inconnu est <b>consommée</b> dès qu'elle est annoncée : le registre de présence
 * tient la venue pour tranchée, et une temporisation de deux minutes court avant la suivante.
 * C'est ce qu'il faut quand le robot a effectivement abordé quelqu'un. Mais quand l'annonce n'a
 * abouti à rien, cette temporisation retombe sur le prochain inconnu, qui, lui, n'a jamais rien
 * demandé — et le robot l'ignore.
 * <p>
 * Deux façons d'échouer, constatées sur le robot le 2026-08-15 :
 * <ul>
 *   <li>l'inconnu était en fait quelqu'un de connu, et le robot s'en est excusé ;</li>
 *   <li>le cerveau a refusé la présentation, la précédente étant trop récente. Sans cet
 *       évènement, la demande était perdue : la temporisation expirait, mais plus rien ne
 *       relançait quoi que ce soit tant que la personne restait devant la caméra.</li>
 * </ul>
 */
public class RencontreInconnuInaboutieEvent extends RobotEvent {

    public static final String EVENT_TYPE = "rencontre-inconnu-inaboutie";

    /** Ce qui a fait échouer la rencontre, pour les journaux. */
    private String motif;

    public RencontreInconnuInaboutieEvent() {
        super(EVENT_TYPE);
    }

    public RencontreInconnuInaboutieEvent(String motif) {
        super(EVENT_TYPE);
        this.motif = motif;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
