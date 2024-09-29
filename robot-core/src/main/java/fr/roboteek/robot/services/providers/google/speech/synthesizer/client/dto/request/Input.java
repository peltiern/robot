package fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.request;

public class Input {

    private String text;

    public Input(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
