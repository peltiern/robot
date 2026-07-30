package fr.roboteek.robot.services.recognizer;

/**
 * Reconnaissance vocale en streaming : le texte est décodé au fil de l'arrivée
 * de l'audio plutôt qu'après coup sur un fichier complet (voir
 * {@link SpeechRecognizerService}), pour réduire la latence perçue après la
 * fin d'une phrase. Interface séparée volontairement : un fournisseur peut
 * implémenter uniquement {@link SpeechRecognizerService} (chemin batch).
 */
public interface StreamingSpeechRecognizerService {

    /**
     * Démarre une nouvelle phrase.
     *
     * @param preRollAudio audio précédant le premier bloc "parlé" détecté (mémorisé pour ne pas
     *                      couper le tout début du mot), au format de capture d'origine (non converti)
     */
    void startPhrase(byte[] preRollAudio);

    /**
     * Fournit un nouveau bloc audio appartenant à la phrase en cours, au format de capture
     * d'origine (non converti).
     *
     * @param audioBlock le bloc audio
     */
    void acceptAudioBlock(byte[] audioBlock);

    /**
     * Termine la phrase en cours et renvoie le texte reconnu.
     *
     * @return le texte reconnu, chaîne vide si rien n'a été reconnu
     */
    String finishPhrase();

    /**
     * Abandonne la phrase en cours (ex. mise en pause) sans produire de résultat.
     */
    void cancelPhrase();
}
