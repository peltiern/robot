package fr.roboteek.robot.util.reconnaissance.vocale.ibm;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechAlternative;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Transcript;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.util.reconnaissance.vocale.SpeechRecognizer;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

/**
 * Moteur de reconnaissance vocale utilisant le service web Speech To Text d'IBM.
 * @author Nicolas
 *
 */
public class IBMSpeechRecognizerRest implements SpeechRecognizer {

	/** Clé de la variable d'environnement contenant le nom d'utilisateur du service Watson. */
	private static String ENV_VAR_IBM_WATSON_SERVICE_USERNAME = "IBM_WATSON_SERVICE_USERNAME";
	
	/** Clé de la variable d'environnement contenant le mot de passe du service Watson. */
	private static String ENV_VAR_IBM_WATSON_SERVICE_PASSWORD = "IBM_WATSON_SERVICE_PASSWORD";

	/** URL du service web. */
	private static String WEB_SERVICE_SPEECH_TO_TEXT_URL = "https://stream-fra.watsonplatform.net/speech-to-text/api";

	/** Singleton de la classe. */
	private static IBMSpeechRecognizerRest instance;
	
	/** Service "Speech To Text". */
	private SpeechToText service;
	
	/** Options de reconnaissance. */
	private RecognizeOptions options;

	/** Encodeur de fichiers WAV --> FLAC. */
	private FLAC_FileEncoder flacEncoder;

	/** Chemin du fichier FLAC. */
	private String cheminFichierFlac;

	/** Constructeur privé. */
	private IBMSpeechRecognizerRest() {

		flacEncoder = new FLAC_FileEncoder();
		cheminFichierFlac = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "Google" + File.separator + "reconnaissance.flac";

		// Création du service
		service = new SpeechToText();
		service.setEndPoint(WEB_SERVICE_SPEECH_TO_TEXT_URL);
//		service.setUsernameAndPassword("dcd0c12e-a5a8-4423-b088-5b329473e240", "nXuPMyb1nzna");
		service.setUsernameAndPassword(System.getenv(ENV_VAR_IBM_WATSON_SERVICE_USERNAME), System.getenv(ENV_VAR_IBM_WATSON_SERVICE_PASSWORD));
		
		// Création des options
		options = new RecognizeOptions.Builder()
				  .contentType("audio/flac")
				  .model("fr-FR_BroadbandModel")
				  .timestamps(true)
				  .build();
	}

	/**
	 * Récupère l'instance du moteur de reconnaissance.
	 * @return l'instance du moteur de reconnaissance
	 */
	public static IBMSpeechRecognizerRest getInstance() {
		if (instance == null) {
			instance = new IBMSpeechRecognizerRest();
		}
		return instance;
	}

	public String reconnaitre(String cheminFichierWav) {
		File fichierWav = new File(cheminFichierWav);
		File fichierFlac = new File(cheminFichierFlac);
		flacEncoder.encode(fichierWav, fichierFlac);
		
		final SpeechResults resultats = service.recognize(fichierFlac, options).execute();

		// Récupération du résultat
		if (resultats != null) {
			final List<Transcript> listeTranscripts = resultats.getResults();
			if (listeTranscripts != null && !listeTranscripts.isEmpty()) {
				Transcript transcript = listeTranscripts.get(0);
				final List<SpeechAlternative> listeAlternatives = transcript.getAlternatives();
				if (listeAlternatives != null && !listeAlternatives.isEmpty()) {
					SpeechAlternative alternative = listeAlternatives.get(0);
					return alternative.getTranscript();
				}
			}
		}
		//		fichierFlac.delete();
		return "";
	}

	public static void main(String[] args) throws IOException {
		final SpeechRecognizer ibmRecognizerRest = IBMSpeechRecognizerRest.getInstance();
		long debut = System.currentTimeMillis();
		final String resultat = ibmRecognizerRest.reconnaitre("test.wav");
		long fin = System.currentTimeMillis();
		System.out.println("Résultat = " + resultat);
		System.out.println("Temps = " + (fin - debut));
	}
}