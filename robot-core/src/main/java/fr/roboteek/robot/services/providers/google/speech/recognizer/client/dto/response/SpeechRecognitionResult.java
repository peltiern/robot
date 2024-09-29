package fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response;

import java.util.List;

public class SpeechRecognitionResult {

    private List<SpeechRecognitionAlternative> alternatives;

    public SpeechRecognitionResult(List<SpeechRecognitionAlternative> alternatives) {
        this.alternatives = alternatives;
    }

    public List<SpeechRecognitionAlternative> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<SpeechRecognitionAlternative> alternatives) {
        this.alternatives = alternatives;
    }
}
