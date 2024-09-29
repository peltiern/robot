package fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request;

public class RecognitionAudio {

    private String content;

    public RecognitionAudio(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
