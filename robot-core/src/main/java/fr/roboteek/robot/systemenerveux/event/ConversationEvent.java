package fr.roboteek.robot.systemenerveux.event;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Evènement de conversation.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Entity
@DiscriminatorValue(ConversationEvent.EVENT_TYPE)
public class ConversationEvent extends RobotEvent {

    public static final String EVENT_TYPE = "conversation";

    /**
     * Texte.
     */
    @Column(name = "text_1")
    private String texte;

    /**
     * Locuteur.
     * -1 : robot
     * 0 : inconnu
     * > 0 : identifiant de la personne
     */
    @Column(name = "integer_1")
    private int idLocuteur = -1;

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

    public int getIdLocuteur() {
        return idLocuteur;
    }

    public void setIdLocuteur(int idLocuteur) {
        this.idLocuteur = idLocuteur;
    }

}
