package fr.roboteek.robot.systemenerveux.event;

/**
 * Le robot a fini de dire une phrase.
 * <p>
 * {@link ParoleEvent} est un tir sans retour : il est traité de façon asynchrone, et celui qui
 * l'a émis ne sait pas quand la phrase a été prononcée. C'est sans conséquence pour un robot qui
 * commente, mais pas pour un robot qui <b>pose une question et attend la réponse</b> : sans ce
 * signal, le délai d'attente commencerait à courir dès l'émission, et les quelques secondes de
 * synthèse et de lecture seraient décomptées du temps laissé à la personne pour répondre.
 * <p>
 * Émis dans tous les cas, y compris quand la synthèse échoue : ce qui attend derrière doit
 * reprendre la main, pas rester suspendu.
 */
public class ParoleTermineeEvent extends RobotEvent {

    public static final String EVENT_TYPE = "parole-terminee";

    /** Le texte qui vient d'être dit, pour que l'attente sache de quelle phrase il s'agit. */
    private final String texte;

    public ParoleTermineeEvent(String texte) {
        super(EVENT_TYPE);
        this.texte = texte;
    }

    public String getTexte() {
        return texte;
    }

}
