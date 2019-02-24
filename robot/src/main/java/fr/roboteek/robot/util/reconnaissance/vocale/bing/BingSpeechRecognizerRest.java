package fr.roboteek.robot.util.reconnaissance.vocale.bing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import fr.roboteek.robot.util.reconnaissance.vocale.SpeechRecognizer;

/**
 * Moteur de reconnaissance vocale utilisant le service web Speech de Microsoft.
 * @author Nicolas
 *
 */
public class BingSpeechRecognizerRest implements SpeechRecognizer {
		
		/** Clé de la variable d'environnement "Clé API Speech". */
		private static String ENV_VAR_BING_SPEECH_API_KEY = "BING_SPEECH_API_KEY";
		
		/** Clé de la variable d'environnement "GUID de l'application". */
		private static String ENV_VAR_APPLICATION_ID = "BING_SPEECH_API_APPLICATION_ID";

		/** URL du service web de récupération du token. */
		private static String WEB_SERVICE_TOKEN_URL = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";
		
		/** URL du service web de reconnaissance. */
		private static String WEB_SERVICE_SPEECH_URL = "https://speech.platform.bing.com/speech/recognition/dictation/cognitiveservices/v1";

		/** Singleton de la classe. */
		private static BingSpeechRecognizerRest instance;
		
		/** Clé API Speech. */
		private String speehApiKey;
		
		/** Classe GSON permettant la création des objets JSON. */
		private Gson gson;

		/** Constructeur privé. */
		private BingSpeechRecognizerRest() {
			speehApiKey = System.getenv(ENV_VAR_BING_SPEECH_API_KEY);
			
			gson = new GsonBuilder().create();
		}

		/**
		 * Récupère l'instance du moteur de reconnaissance.
		 * @return l'instance du moteur de reconnaissance
		 */
		public static BingSpeechRecognizerRest getInstance() {
			if (instance == null) {
				instance = new BingSpeechRecognizerRest();
			}
			return instance;
		}

		public String reconnaitre(String cheminFichierWav) {
			
			BingSpeechResponse response = null;
			
			try {
				// Récupération du contenu du fichier WAV
				final byte[] contenuFichierWav = Files.readAllBytes(Paths.get(cheminFichierWav));
				
				// Appel du service web
				response = gson.fromJson(Unirest.post(WEB_SERVICE_SPEECH_URL)
						.queryString("language", "fr-FR")
						.queryString("locale", "fr-FR")
						.queryString("format", "json")
						.queryString("requestid", UUID.randomUUID())
						.header("Authorization", "Bearer " + getToken())
						.header("Content-type", "audio/wav")
						.body(contenuFichierWav)
						.asString().getBody(), BingSpeechResponse.class);
			} catch (UnirestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			// Récupération du résultat
			if (response != null && response.getRecognitionStatus() != null && response.getRecognitionStatus().equalsIgnoreCase("success")) {
				if (response.getDisplayText() != null) {
					return response.getDisplayText();
				}
			}
			
			return "";
		}
		
		/**
		 * Récupère le jeton permettant d'utiliser le service de reconnaissance.
		 * @return le jeton
		 */
		private String getToken() {
			try {
				return Unirest.post(WEB_SERVICE_TOKEN_URL)
				.header("Ocp-Apim-Subscription-Key", speehApiKey)
				.asString().getBody();
			} catch (UnirestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "";

		}

		public static void main(String[] args) throws IOException {
			final BingSpeechRecognizerRest bingSpeechRecognizerRest = BingSpeechRecognizerRest.getInstance();
			long debut = System.currentTimeMillis();
			final String resultat = bingSpeechRecognizerRest.reconnaitre("/home/npeltier/Robot/Programme/reconnaissanceVocale/test.wav");
			long fin = System.currentTimeMillis();
			System.out.println("Résultat = " + resultat);
			System.out.println("Temps = " + (fin - debut));
		}
}