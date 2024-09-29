package fr.roboteek.robot.services.synthesizer;

/**
 * Interface de synthèse vocale.
 */
public interface SpeechSynthesizerService {

    /**
     * Synthétise un texte en voix sous forme de fichier audio WAV.
     *
     * @param texte le texte à synthétiser
     * @return le contenu du fichier WAV
     */
    byte[] synthesize(String texte);
}
