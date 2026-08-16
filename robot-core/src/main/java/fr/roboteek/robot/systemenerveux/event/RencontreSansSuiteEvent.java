package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.memoire.longterme.personne.Personne;

/**
 * La rencontre annoncée n'a mené à rien : son tour est rendu à celui qu'elle visait.
 * <p>
 * Une rencontre est <b>consommée</b> dès qu'elle est annoncée : le registre de présence tient la
 * venue pour tranchée, et une temporisation court avant la suivante. C'est ce qu'il faut quand le
 * robot a effectivement parlé à quelqu'un. Mais quand l'annonce n'a abouti à rien, cette
 * temporisation retombe sur une personne qui, elle, n'a rien reçu — et tant qu'elle reste devant
 * la caméra, le robot ne lui dira jamais rien : il faudrait qu'elle disparaisse plus de quatre
 * secondes <b>et</b> que la temporisation expire.
 * <p>
 * Trois façons d'échouer, toutes constatées sur le robot le 2026-08-15 et le 2026-08-16 :
 * <ul>
 *   <li>l'inconnu était en fait quelqu'un de connu, et le robot s'en est excusé ;</li>
 *   <li>l'accueil a été refusé par le cerveau, la présentation précédente étant trop récente ;</li>
 *   <li>les retrouvailles ont été refusées, une présentation plus prioritaire étant en cours.</li>
 * </ul>
 */
public class RencontreSansSuiteEvent extends RobotEvent {

    public static final String EVENT_TYPE = "rencontre-sans-suite";

    /** La personne dont la venue est à rendre ; {@code null} désigne l'inconnu. */
    private Personne personne;

    /** Ce qui a fait échouer la rencontre, pour les journaux. */
    private String motif;

    public RencontreSansSuiteEvent() {
        super(EVENT_TYPE);
    }

    public RencontreSansSuiteEvent(Personne personne, String motif) {
        super(EVENT_TYPE);
        this.personne = personne;
        this.motif = motif;
    }

    public Personne getPersonne() {
        return personne;
    }

    public void setPersonne(Personne personne) {
        this.personne = personne;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
