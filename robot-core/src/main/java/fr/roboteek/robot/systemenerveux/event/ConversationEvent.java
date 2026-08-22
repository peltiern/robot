package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement de conversation : un tour de dialogue, et qui l'a prononcé.
 * <p>
 * Le locuteur était jusqu'ici un {@code int} hérité (-1 robot, 0 inconnu, > 0 identifiant), tenu
 * de l'époque où les personnes portaient un numéro. Elles portent un UUID depuis, et ce champ ne
 * désignait donc plus personne : il ne valait jamais que -1 ou 0. Il est remplacé par ce qu'on
 * veut réellement savoir — est-ce le robot qui parle, et sinon à qui a-t-on affaire.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ConversationEvent extends RobotEvent {

    public static final String EVENT_TYPE = "conversation";

    /**
     * Texte.
     */
    private String texte;

    /**
     * Vrai si c'est le robot qui parle, faux si c'est quelqu'un devant lui.
     */
    private boolean duRobot;

    /**
     * Identifiant de la personne qui parle, ou {@code null} si le robot ne sait pas à qui il
     * parle — personne d'identifié devant lui, ou reconnaissance qui cligne au mauvais moment.
     * Toujours {@code null} quand c'est le robot qui parle.
     */
    private String idPersonne;

    /**
     * Prénom de la personne qui parle, transporté avec l'identifiant pour que l'interface ait de
     * quoi afficher sans rien demander de plus.
     */
    private String prenom;

    public ConversationEvent() {
        super(EVENT_TYPE);
    }

    /**
     * Récupère la valeur de texte.
     *
     * @return la valeur de texte
     */
    public String getTexte() {
        return texte;
    }

    /**
     * Définit la valeur de texte.
     *
     * @param texte la nouvelle valeur de texte
     */
    public void setTexte(String texte) {
        this.texte = texte;
    }

    public boolean isDuRobot() {
        return duRobot;
    }

    public void setDuRobot(boolean duRobot) {
        this.duRobot = duRobot;
    }

    public String getIdPersonne() {
        return idPersonne;
    }

    public void setIdPersonne(String idPersonne) {
        this.idPersonne = idPersonne;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

}
