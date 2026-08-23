package fr.roboteek.robot.services.recognizer;

/**
 * Reconnaissance vocale d'un fichier audio complet.
 * <p>
 * Le chemin « batch » : la reconnaissance ne commence qu'une fois la phrase finie. Voir
 * {@link StreamingSpeechRecognizerService} pour celui qui décode au fil de la parole.
 */
public interface SpeechRecognizerService {

    /** Fournisseurs disponibles, choisis dans {@code robot.properties}. */
    enum Provider {
        GOOGLE,
        VOSK
    }

    /**
     * @param wavFilePath le chemin vers le fichier audio WAV
     * @return le texte reconnu
     */
    String recognize(String wavFilePath);
}
