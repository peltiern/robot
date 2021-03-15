package fr.roboteek.robot.sandbox.reconnaissance.vocale.bing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import fr.roboteek.robot.sandbox.reconnaissance.vocale.DetecteurVocal;
import fr.roboteek.robot.sandbox.reconnaissance.vocale.SpeechRecognizer;


/**
 * Moteur de reconnaissance vocale utilisant le service web Speech de Microsoft.
 *
 * @author Nicolas Peltier
 */
public class BingSpeechRecognizerRest implements SpeechRecognizer {

    /**
     * URL du service web de récupération du token.
     */
    private static String WEB_SERVICE_TOKEN_URL = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";

    /**
     * URL du service web de reconnaissance.
     */
    private static String WEB_SERVICE_SPEECH_URL = "https://speech.platform.bing.com/recognize";

    /**
     * Singleton de la classe.
     */
    private static BingSpeechRecognizerRest instance;

    /**
     * Clé API Bing Speech (à récupérer sur le site de l'API).
     */
    private String bingSpeechApiKey = "32e6e62de6ff4bd08310acf11c85dbb1";

    /**
     * GUID de l'application Bing (GUID personnel à générer par le développeur).
     */
    private static String bingSpeechApplicationId = "b53084f8-1170-4aba-bcae-8dfb2ace89ad";

    /**
     * Classe GSON permettant la création des objets JSON.
     */
    private Gson gson;

    /**
     * Constructeur privé.
     */
    private BingSpeechRecognizerRest() {

        // Décodeur JSON
        gson = new GsonBuilder().create();
    }

    /**
     * Récupère l'instance du moteur de reconnaissance.
     *
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

            // Appel du service web avec Unirest et transformation de la réponse JSON
            response = gson.fromJson(
                    Unirest.post(WEB_SERVICE_SPEECH_URL)
                            .queryString("scenarios", "smd")
                            .queryString("appid", bingSpeechApplicationId)
                            .queryString("locale", "fr-FR")
                            .queryString("device.os", "Windows")
                            .queryString("version", "3.0")
                            .queryString("format", "json")
                            .queryString("instanceid", UUID.randomUUID())
                            .queryString("requestid", UUID.randomUUID())
                            .header("Authorization", "Bearer " + getToken())
                            .header("Content-type", "audio/wav")
                            .body(contenuFichierWav)
                            .asString()
                            .getBody(),
                    BingSpeechResponse.class);
        } catch (UnirestException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Récupération du résultat
        if (response != null && response.getHeader() != null && response.getHeader().getStatus() != null && response.getHeader().getStatus().equals("success")) {
            if (response.getHeader().getName() != null) {
                return response.getHeader().getName();
            }
        }

        return "";
    }

    /**
     * Récupère le jeton permettant d'utiliser le service de reconnaissance.
     *
     * @return le jeton
     */
    private String getToken() {
        try {
            return Unirest.post(WEB_SERVICE_TOKEN_URL)
                    .header("Ocp-Apim-Subscription-Key", bingSpeechApiKey)
                    .asString()
                    .getBody();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return "";

    }

    public static void main(String[] args) throws IOException {
        final BingSpeechRecognizerRest bingSpeechRecognizerRest = BingSpeechRecognizerRest.getInstance();
        // Création et lancement du détecteur vocal en passant le moteur de reconnaissance à utiliser
        final DetecteurVocal detecteurVocal = new DetecteurVocal(bingSpeechRecognizerRest);
        detecteurVocal.demarrer();
    }
}
