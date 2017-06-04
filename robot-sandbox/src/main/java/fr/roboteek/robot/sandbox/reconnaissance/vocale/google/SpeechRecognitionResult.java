package fr.roboteek.robot.sandbox.reconnaissance.vocale.google;


import java.util.List;

public class SpeechRecognitionResult {

	private List<SpeechRecognitionAlternative> alternatives;

	public List<SpeechRecognitionAlternative> getAlternatives() {
		return alternatives;
	}

	public void setAlternatives(List<SpeechRecognitionAlternative> alternatives) {
		this.alternatives = alternatives;
	}

	@Override
	public String toString() {
		return "SpeechRecognitionResult [alternatives=" + alternatives + "]";
	}
	
}
