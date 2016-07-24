package fr.roboteek.robot.util.reconnaissance.vocale.google;

public class RecognizeRequest {

	private RecognitionConfig config;
	
	private RecognitionAudio audio;

	public RecognitionConfig getConfig() {
		return config;
	}

	public void setConfig(RecognitionConfig config) {
		this.config = config;
	}

	public RecognitionAudio getAudio() {
		return audio;
	}

	public void setAudio(RecognitionAudio audio) {
		this.audio = audio;
	}
}
