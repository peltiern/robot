package fr.roboteek.robot.util.reconnaissance.vocale.google;


import java.util.List;

public class RecognizeResponse {

	private List<SpeechRecognitionResult> results;
	
	private int resultIndex;

	public List<SpeechRecognitionResult> getResults() {
		return results;
	}

	public void setResults(List<SpeechRecognitionResult> results) {
		this.results = results;
	}

	public int getResultIndex() {
		return resultIndex;
	}

	public void setResultIndex(int resultIndex) {
		this.resultIndex = resultIndex;
	}

	@Override
	public String toString() {
		return "RecognizeResponse [results=" + results + ", resultIndex=" + resultIndex + "]";
	}
}
