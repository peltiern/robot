package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement de reconnaissance vocale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ReconnaissanceVocaleEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "ReconnaissanceVocale";
    
    /** Texte reconnu. */
    private String texteReconnu;
    
    /** Nom de la règle correspondant au texte reconnu. */
    private String nomRegle;

    /**
     * Récupère la valeur de texteReconnu.
     * @return la valeur de texteReconnu
     */
    public String getTexteReconnu() {
        return texteReconnu;
    }

    /**
     * Définit la valeur de texteReconnu.
     * @param texteReconnu la nouvelle valeur de texteReconnu
     */
    public void setTexteReconnu(String texteReconnu) {
        this.texteReconnu = texteReconnu;
    }

    /**
     * Récupère la valeur de nomRegle.
     * @return la valeur de nomRegle
     */
    public String getNomRegle() {
        return nomRegle;
    }

    /**
     * Définit la valeur de nomRegle.
     * @param nomRegle la nouvelle valeur de nomRegle
     */
    public void setNomRegle(String nomRegle) {
        this.nomRegle = nomRegle;
    }

}
