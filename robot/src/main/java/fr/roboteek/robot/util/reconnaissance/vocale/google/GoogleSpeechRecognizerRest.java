package fr.roboteek.robot.util.reconnaissance.vocale.google;

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

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.util.reconnaissance.vocale.SpeechRecognizer;
import fr.roboteek.robot.util.reconnaissance.vocale.google.RecognitionConfig.AudioEncoding;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

/**
 * Moteur de reconnaissance vocale utilisant le service web Cloud Speech de Google.
 * @author Nicolas
 *
 */
public class GoogleSpeechRecognizerRest implements SpeechRecognizer {

	/** Clé de la variable d'environnement contenant la clé de l'API Google. */
	private static String ENV_VAR_GOOGLE_API_KEY = "GOOGLE_API_KEY";

	/** URL du service web. */
	private static String WEB_SERVICE_SPEECH_URL = "https://speech.googleapis.com/v1/speech:recognize";

	/** Singleton de la classe. */
	private static GoogleSpeechRecognizerRest instance;

	/** Classe GSON permettant la création des objets JSON. */
	private Gson gson;

	private String paramApiKey;

	/** Objet contenant les infos nécessaires pour l'appel du service web. */
	private RecognizeRequest request;

	/** Encodeur de fichiers WAV --> FLAC. */
	private FLAC_FileEncoder flacEncoder;

	/** Chemin du fichier FLAC. */
	private String cheminFichierFlac;

	/** Constructeur privé. */
	private GoogleSpeechRecognizerRest() {

		flacEncoder = new FLAC_FileEncoder();
		cheminFichierFlac = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "Google" + File.separator + "reconnaissance.flac";

		gson = new GsonBuilder().create();

		// Construction du parmètre de clé de l'API
		paramApiKey = "?key=" + System.getenv(ENV_VAR_GOOGLE_API_KEY);

		// Paramètre fixe de la requête
		RecognitionConfig config = new RecognitionConfig();
		config.setEncoding(AudioEncoding.FLAC);
		config.setSampleRateHertz(44100);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.setAudio(audioRequest);


		RecognizeResponse response = null;

		try {
			// Appel du service web
			System.out.println("==> Appel Cloud Speech");
			
			String test = Unirest.post(WEB_SERVICE_SPEECH_URL + paramApiKey)
					.body(gson.toJson(request))
					.asString().getBody();
			
			System.out.println("Test = " + test);
			
			response = gson.fromJson(Unirest.post(WEB_SERVICE_SPEECH_URL + paramApiKey)
					.body(gson.toJson(request))
					.asString().getBody(), RecognizeResponse.class);
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Récupération du résultat
		if (response != null) {
			List<SpeechRecognitionResult> listeRecognitionResults = response.getResults();
			//		System.out.println("Reponse = " + response);
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
		long debut = System.currentTimeMillis();
		final String resultat = googleRecognizerRest.reconnaitre("test.wav");
		long fin = System.currentTimeMillis();
		System.out.println("Résultat = " + resultat);
		System.out.println("Temps = " + (fin - debut));
	}
}