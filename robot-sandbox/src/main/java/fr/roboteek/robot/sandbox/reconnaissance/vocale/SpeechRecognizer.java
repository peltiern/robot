/**
 * 
 */
package fr.roboteek.robot.sandbox.reconnaissance.vocale;

/**
 * Interface de d'une reconnaissance vocale.
 * @author Nicolas
 *
 */
public interface SpeechRecognizer {

	/**
	 * Reconnait le contenu d'un fichier WAV.
	 * @param cheminFichierWav le chemin du fichier WAV
	 * @return le contenu
	 */
	String reconnaitre(String cheminFichierWav);
}
