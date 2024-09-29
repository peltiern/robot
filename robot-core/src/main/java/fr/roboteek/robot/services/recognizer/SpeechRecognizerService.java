package fr.roboteek.robot.services.recognizer;

/**
 * Interface de reconnaissance vocale.
 */
public interface SpeechRecognizerService {

    /**
     * Reconnaissance vocale du contenu d'un fichier audio WAV.
     *
     * @param wavFilePath le chemin vers le fichier audio WAV
     * @return le texte reconnu
     */
    String recognize(String wavFilePath);
}