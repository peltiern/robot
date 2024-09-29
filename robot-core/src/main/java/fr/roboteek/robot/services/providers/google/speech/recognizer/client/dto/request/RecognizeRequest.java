package fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request;

public class RecognizeRequest {

    private RecognitionConfig config;
    private RecognitionAudio audio;

    public RecognizeRequest(RecognitionConfig config, RecognitionAudio audio) {
        this.config = config;
        this.audio = audio;
    }

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
