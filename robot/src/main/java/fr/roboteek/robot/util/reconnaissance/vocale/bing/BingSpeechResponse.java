package fr.roboteek.robot.util.reconnaissance.vocale.bing;

/**
 * Réponse du service Web de reconnaissance vocale de Bing.
 * @author Nicolas
 *
 */
public class BingSpeechResponse {
	
	/** Statut. */
	private String RecognitionStatus;
	
	/** Entête. */
	private String DisplayText;
	
	/** Durée. */
	private int Duration;

	public String getRecognitionStatus() {
		return RecognitionStatus;
	}

	public void setRecognitionStatus(String recognitionStatus) {
		this.RecognitionStatus = recognitionStatus;
	}

	public String getDisplayText() {
		return DisplayText;
	}

	public void setDisplayText(String displayText) {
		this.DisplayText = displayText;
	}

	public int getDuration() {
		return Duration;
	}

	public void setDuration(int duration) {
		this.Duration = duration;
	}

	@Override
	public String toString() {
		return "BingSpeechResponse [recognitionStatus=" + RecognitionStatus + ", displayText=" + DisplayText
				+ ", duration=" + Duration + "]";
	}

}
