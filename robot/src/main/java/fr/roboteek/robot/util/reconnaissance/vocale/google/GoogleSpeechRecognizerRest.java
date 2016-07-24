package fr.roboteek.robot.util.reconnaissance.vocale.google;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.client.ClientConfig;

import com.google.api.client.util.StringUtils;

import fr.roboteek.robot.util.reconnaissance.vocale.google.RecognitionConfig.AudioEncoding;

/**
 * Moteur de reconnaissance vocale utilisant le service web Cloud Speech de Google.
 * @author Nicolas
 *
 */
public class GoogleSpeechRecognizerRest implements GoogleSpeechRecognizer {

	/** Clé de la variable d'environnement contenant la clé de l'API Google. */
	private static String ENV_VAR_GOOGLE_API_KEY = "GOOGLE_API_KEY";

	/** URL du service web. */
	private static String WEB_SERVICE_SPEECH_URL = "https://speech.googleapis.com/v1beta1/speech:syncrecognize";

	/** Singleton de la classe. */
	private static GoogleSpeechRecognizerRest instance;

	/** Objet permettant d'appeler le service web. */
	private Invocation.Builder invocationBuilder;

	/** Objet contenant les infos nécessaires pour l'appel du service web. */
	private RecognizeRequest request;

	/** Constructeur privé. */
	private GoogleSpeechRecognizerRest() {

		// Construction du parmètre de clé de l'API
		final String paramApiKey = "?key=" + System.getenv(ENV_VAR_GOOGLE_API_KEY);

		// Construction de l'objet permettant d'appeler le service web
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget webTarget = client.target(WEB_SERVICE_SPEECH_URL + paramApiKey);
		invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

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

	public String reconnaitre(String cheminFichierFlac) {
		// Récupération du fichier FLAC
		Path path = Paths.get(cheminFichierFlac);
		RecognitionAudio audioRequest = new RecognitionAudio();
		try {
//			System.out.println("Fichier = " + StringUtils.newStringUtf8(Base64.encodeBase64(Files.readAllBytes(path), false)));
			// Passage à l'objet de requêtage du fichier FLAC encodé en Base 64
			audioRequest.setContent(StringUtils.newStringUtf8(Base64.encodeBase64(Files.readAllBytes(path), false)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.setAudio(audioRequest);
		
		// Appel du service web
		Response response = invocationBuilder.post(Entity.json(request));

		// Récupération du résultat
		List<SpeechRecognitionResult> listeRecognitionResults = response.readEntity(RecognizeResponse.class).getResults();
//		System.out.println("Reponse = " + response);
		if (listeRecognitionResults != null && !listeRecognitionResults.isEmpty()) {
			SpeechRecognitionResult result = listeRecognitionResults.get(0);
			final List<SpeechRecognitionAlternative> listeAlternatives = result.getAlternatives();
			if (listeAlternatives != null && !listeAlternatives.isEmpty()) {
				SpeechRecognitionAlternative alternative = listeAlternatives.get(0);
				return alternative.getTranscript();
			}
		}
		return "";
	}

	public static void main(String[] args) throws IOException {
		final GoogleSpeechRecognizer googleRecognizerRest = GoogleSpeechRecognizerRest.getInstance();
		final String resultat = googleRecognizerRest.reconnaitre("test.flac");
		System.out.println("Résultat = " + resultat);
	}
}