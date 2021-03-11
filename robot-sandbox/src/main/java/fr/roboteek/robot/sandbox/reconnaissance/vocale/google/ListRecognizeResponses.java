package fr.roboteek.robot.sandbox.reconnaissance.vocale.google;


import java.util.List;

public class ListRecognizeResponses {

    private List<RecognizeResponse> responses;

    public List<RecognizeResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<RecognizeResponse> responses) {
        this.responses = responses;
    }

    @Override
    public String toString() {
        return "ListRecognizeResponses [responses=" + responses + "]";
    }
}
