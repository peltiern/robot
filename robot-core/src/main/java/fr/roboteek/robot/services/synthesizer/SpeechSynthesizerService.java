package fr.roboteek.robot.services.synthesizer;

/**
 * Synthèse vocale : d'un texte vers un WAV.
 */
public interface SpeechSynthesizerService {

    /** Fournisseurs disponibles, choisis dans {@code robot.properties}. */
    enum Provider {
        GOOGLE,
        PIPER
    }

    /**
     * @param texte le texte à synthétiser
     * @return le contenu du fichier WAV
     */
    byte[] synthesize(String texte);
}
