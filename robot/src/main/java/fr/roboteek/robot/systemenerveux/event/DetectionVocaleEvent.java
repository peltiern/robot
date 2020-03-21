package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement de détection vocale (un flux de parole a été détecté et stocké dans un fichier).
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class DetectionVocaleEvent extends RobotEvent {

	public static final String EVENT_TYPE = "DetectionVocaleEvent";

    /** Fichier WAV contenant le flux de parole détecté. */
    private String cheminFichier;


	public DetectionVocaleEvent() {
		super(EVENT_TYPE);
	}

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

}
