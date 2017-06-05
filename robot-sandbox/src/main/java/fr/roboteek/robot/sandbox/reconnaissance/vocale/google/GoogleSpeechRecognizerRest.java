package fr.roboteek.robot.sandbox.reconnaissance.vocale.google;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import fr.roboteek.robot.sandbox.reconnaissance.vocale.DetecteurVocal;
import fr.roboteek.robot.sandbox.reconnaissance.vocale.SpeechRecognizer;
import fr.roboteek.robot.sandbox.reconnaissance.vocale.google.RecognitionConfig.AudioEncoding;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

/**
 * Moteur de reconnaissance vocale utilisant le service web Cloud Speech de Google.
 * @author Nicolas Peltier
 *
 */
public class GoogleSpeechRecognizerRest implements SpeechRecognizer {

	/** URL du service web. */
	private static String WEB_SERVICE_SPEECH_URL = "https://speech.googleapis.com/v1beta1/speech:syncrecognize";

	/** Singleton de la classe. */
	private static GoogleSpeechRecognizerRest instance;

	/** Classe GSON permettant la création des objets JSON. */
	private Gson gson;

	/** Clé API Cloud Speech (à récupérer sur le le site Cloud Speech). */
	private String paramApiKey = "?key=XXXXXXXXXXXXXXXXXXX_XXXXXXXX-XXXXXXXXXX";

	/** Objet contenant les infos nécessaires pour l'appel du service web. */
	private RecognizeRequest request;

	/** Encodeur de fichiers WAV --> FLAC. */
	private FLAC_FileEncoder flacEncoder;

	/** Chemin du fichier FLAC. */
	private String cheminFichierFlac;

	/** Constructeur privé. */
	private GoogleSpeechRecognizerRest() {

		flacEncoder = new FLAC_FileEncoder();
		cheminFichierFlac = "reconnaissance.flac";

		gson = new GsonBuilder().create();

		// Paramètre fixe de la requête
		RecognitionConfig config = new RecognitionConfig();
		config.setEncoding(AudioEncoding.FLAC);
		config.setSampleRate(44100);
		config.setLanguageCode("FR");

		// Objet permettant de faire la requête
		// Cet objet sera complété par le fichier audio à reconnaitre à chaque appel du service
		request = new RecognizeRequest();
		request.setConfig(config);
	}

	/**
	 * Récupère l'instance du moteur de reconnaissance.
	 * @return l'instance du moteur de reconnaissance
	 */
	public static GoogleSpeechRecognizerRest getInstance() {
		if (instance == null) {
			instance = new GoogleSpeechRecognizerRest();
		}
		return instance;
	}

	public String reconnaitre(String cheminFichierWav) {
		
		// Encodage du fichier WAV en fichier FLAC
		File fichierWav = new File(cheminFichierWav);
		File fichierFlac = new File(cheminFichierFlac);
		flacEncoder.encode(fichierWav, fichierFlac);
		
		// Récupération du fichier FLAC
		Path path = Paths.get(cheminFichierFlac);
		RecognitionAudio audioRequest = new RecognitionAudio();
		try {
			// Passage à l'objet de requêtage du fichier FLAC encodé en Base 64
			audioRequest.setContent(StringUtils.newStringUtf8(Base64.encodeBase64(Files.readAllBytes(path), false)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		request.setAudio(audioRequest);


		RecognizeResponse response = null;

		try {
			// Appel du service web avec Unirest et transformation de la réponse JSON
			response = gson.fromJson(
					Unirest.post(WEB_SERVICE_SPEECH_URL + paramApiKey)
					.body(gson.toJson(request))
					.asString()
					.getBody(),
					RecognizeResponse.class);
		} catch (UnirestException e) {
			e.printStackTrace();
		}

		// Récupération du résultat
		if (response != null) {
			List<SpeechRecognitionResult> listeRecognitionResults = response.getResults();
			if (listeRecognitionResults != null && !listeRecognitionResults.isEmpty()) {
				SpeechRecognitionResult result = listeRecognitionResults.get(0);
				final List<SpeechRecognitionAlternative> listeAlternatives = result.getAlternatives();
				if (listeAlternatives != null && !listeAlternatives.isEmpty()) {
					SpeechRecognitionAlternative alternative = listeAlternatives.get(0);
					return alternative.getTranscript();
				}
			}
		}
		//		fichierFlac.delete();
		return "";
	}

	public static void main(String[] args) throws IOException {
		final SpeechRecognizer googleRecognizerRest = GoogleSpeechRecognizerRest.getInstance();
		// Création et lancement du détecteur vocal en passant le moteur de reconnaissance à utiliser
		final DetecteurVocal detecteurVocal = new DetecteurVocal(googleRecognizerRest);
		detecteurVocal.demarrer();
	}
}