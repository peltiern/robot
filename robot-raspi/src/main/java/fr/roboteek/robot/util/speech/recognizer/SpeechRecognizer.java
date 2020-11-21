package fr.roboteek.robot.util.speech.recognizer;

/**
 * Speech recognition interface.
 *
 */
public interface SpeechRecognizer {

	/**
	 * Recognizes a WAV file content.
	 * @param wavFilePath the WAV file path
	 * @return the content
	 */
	String recognize(String wavFilePath);
}
