package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement de conversation.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ConversationEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "Conversation";
    
    /** Texte. */
    private String texte;
    
    /**
     * Locuteur.
     * -1 : robot
     * 0 : inconnu
     * > 0 : identifiant de la personne
     */
    private int idLocuteur = -1;
    
	public ConversationEvent() {
		super(EVENT_TYPE);
	}

    /**
     * Récupère la valeur de texte.
     * @return la valeur de texte
     */
    public String getTexte() {
        return texte;
    }

    /**
     * Définit la valeur de texte.
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
