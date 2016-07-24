/**
 * 
 */
package fr.roboteek.robot.util.reconnaissance.vocale.google;

/**
 * Interface de d'une reconnaissance vocale de Google. (Cloud Speech)
 * @author Nicolas
 *
 */
public interface GoogleSpeechRecognizer {

	/**
	 * Reconnait le contenu d'un fichier FLAC.
	 * @param cheminFichierFlac le chemin du fichier FLAC
	 * @return le contenu
	 */
	String reconnaitre(String cheminFichierFlac);
}
