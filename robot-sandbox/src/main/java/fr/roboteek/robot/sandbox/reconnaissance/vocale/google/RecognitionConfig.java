package fr.roboteek.robot.sandbox.reconnaissance.vocale.google;


public class RecognitionConfig {

	public static enum AudioEncoding {ENCODING_UNSPECIFIED, LINEAR16, FLAC, MULAW, AMR, AMR_WB};
	
	private AudioEncoding encoding;
	private int sampleRate;
	private String languageCode;
	private int maxAlternatives;
	private boolean profanityFilter;
	private SpeechContext speechContext;
	public AudioEncoding getEncoding() {
		return encoding;
	}
	public void setEncoding(AudioEncoding encoding) {
		this.encoding = encoding;
	}
	public int getSampleRate() {
		return sampleRate;
	}
	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	public String getLanguageCode() {
		return languageCode;
	}
	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}
	public int getMaxAlternatives() {
		return maxAlternatives;
	}
	public void setMaxAlternatives(int maxAlternatives) {
		this.maxAlternatives = maxAlternatives;
	}
	public boolean isProfanityFilter() {
		return profanityFilter;
	}
	public void setProfanityFilter(boolean profanityFilter) {
		this.profanityFilter = profanityFilter;
	}
	public SpeechContext getSpeechContext() {
		return speechContext;
	}
	public void setSpeechContext(SpeechContext speechContext) {
		this.speechContext = speechContext;
	}
	
	
}
