package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.memoire.personne.Personne;

/**
 * Quelqu'un est là, et ça vaut la peine d'y réagir.
 * <p>
 * À ne pas confondre avec {@link VisagePercuEvent}, qui n'est que de la perception brute et
 * clignote au rythme des ratés de la détection. Celui-ci est rare et délibéré : il ne part
 * qu'une fois la présence confirmée, et pas avant que la temporisation propre à la personne
 * soit écoulée — sans quoi le robot saluerait la même personne toutes les vingt secondes.
 */
public class RencontreEvent extends RobotEvent {

    public static final String EVENT_TYPE = "rencontre";

    /** Nature de la rencontre. */
    public enum TYPE {
        /** Personne jamais vue, ou non reconnue : il y a un prénom à demander. */
        INCONNU,
        /** Personne connue, revue après une absence : il y a une conversation à reprendre. */
        CONNU_REVU
    }

    private TYPE type;

    /** La personne rencontrée, {@code null} pour un {@link TYPE#INCONNU}. */
    private Personne personne;

    /**
     * Temps écoulé depuis la dernière rencontre, en secondes ; {@code -1} si la personne est
     * inconnue ou n'avait jamais été rencontrée. C'est ce qui permet de saluer autrement
     * quelqu'un vu il y a une heure et quelqu'un vu il y a trois semaines.
     */
    private long secondesDepuisDerniereRencontre;

    public RencontreEvent() {
        super(EVENT_TYPE);
    }

    public RencontreEvent(TYPE type, Personne personne, long secondesDepuisDerniereRencontre) {
        super(EVENT_TYPE);
        this.type = type;
        this.personne = personne;
        this.secondesDepuisDerniereRencontre = secondesDepuisDerniereRencontre;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public Personne getPersonne() {
        return personne;
    }

    public void setPersonne(Personne personne) {
        this.personne = personne;
    }

    public long getSecondesDepuisDerniereRencontre() {
        return secondesDepuisDerniereRencontre;
    }

    public void setSecondesDepuisDerniereRencontre(long secondesDepuisDerniereRencontre) {
        this.secondesDepuisDerniereRencontre = secondesDepuisDerniereRencontre;
    }
}
