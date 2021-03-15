package fr.roboteek.robot.sandbox.reconnaissance.vocale.google;


public class SpeechRecognitionAlternative {

    private String transcript;

    private float confidence;

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

    @Override
    public String toString() {
        return "SpeechRecognitionAlternative [transcript=" + transcript + ", confidence=" + confidence + "]";
    }
}
