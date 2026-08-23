package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.memoire.longterme.personne.Personne;

/**
 * La rencontre annoncée n'a mené à rien : son tour est rendu à celui qu'elle visait.
 * <p>
 * Une rencontre est <b>consommée</b> dès l'annonce — le registre tient la venue pour tranchée et
 * une temporisation court. Quand l'annonce n'aboutit pas, cette temporisation retombe sur
 * quelqu'un qui n'a rien reçu : tant qu'il reste devant la caméra, le robot ne lui dira jamais
 * rien. Trois façons d'en arriver là, toutes constatées sur le robot :
 * <ul>
 *   <li>l'inconnu était en fait quelqu'un de connu, et le robot s'en est excusé ;</li>
 *   <li>l'accueil a été refusé, la présentation précédente étant trop récente ;</li>
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
