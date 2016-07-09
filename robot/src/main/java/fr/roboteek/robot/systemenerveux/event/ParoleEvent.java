package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour lire du texte.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ParoleEvent extends RobotEvent {
    
    /** Texte à dire. */
    private String texte;

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

}
