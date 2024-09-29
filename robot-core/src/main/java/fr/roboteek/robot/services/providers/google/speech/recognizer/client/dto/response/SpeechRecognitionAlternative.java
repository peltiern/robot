package fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response;

public class SpeechRecognitionAlternative {

    private String transcript;
    private float confidence;

    public SpeechRecognitionAlternative(String transcript, float confidence) {
        this.transcript = transcript;
        this.confidence = confidence;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
