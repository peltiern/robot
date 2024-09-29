package fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response;

import java.util.List;

public class RecognizeResponse {

    private List<SpeechRecognitionResult> results;

    public RecognizeResponse(List<SpeechRecognitionResult> results) {
        this.results = results;
    }

    public List<SpeechRecognitionResult> getResults() {
        return results;
    }

    public void setResults(List<SpeechRecognitionResult> results) {
        this.results = results;
    }
}
