package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.memoire.longterme.personne.Personne;

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

    private final TYPE type;

    /** La personne rencontrée, {@code null} pour un {@link TYPE#INCONNU}. */
    private final Personne personne;

    /**
     * <b>Durée pendant laquelle la personne n'a pas été vue</b> avant de réapparaître, en
     * secondes ; {@code -1} si elle est inconnue ou si c'est sa première apparition. C'est ce qui
     * permet de saluer autrement quelqu'un revenu après trois semaines et quelqu'un qui s'est
     * absenté trente secondes.
     * <p>
     * L'absence réelle, et non le temps écoulé depuis la dernière rencontre annoncée : ces deux
     * durées n'ont rien à voir. Quelqu'un peut parler sans discontinuer pendant dix minutes sans
     * qu'aucune rencontre ne soit annoncée — les avoir confondues a fait dire au robot « on
     * parlait de la population française » douze secondes après en avoir parlé.
     */
    private final long secondesDAbsence;

    public RencontreEvent(TYPE type, Personne personne, long secondesDAbsence) {
        super(EVENT_TYPE);
        this.type = type;
        this.personne = personne;
        this.secondesDAbsence = secondesDAbsence;
    }

    public TYPE getType() {
        return type;
    }

    public Personne getPersonne() {
        return personne;
    }

    public long getSecondesDAbsence() {
        return secondesDAbsence;
    }

}
