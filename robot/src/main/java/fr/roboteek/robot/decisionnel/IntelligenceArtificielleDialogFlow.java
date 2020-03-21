package fr.roboteek.robot.decisionnel;

import com.google.cloud.dialogflow.v2.AudioEncoding;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.InputAudioConfig;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.ByteString;
import fr.roboteek.robot.Constantes;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IntelligenceArtificielleDialogFlow {

    private static final String LANGUAGE_CODE = "fr-FR";

    /**
     * Session DialogFlow.
     */
    private SessionsClient sessionsClient;

    private SessionName session;

    /**
     * Encodeur de fichiers WAV --> FLAC.
     */
    private FLAC_FileEncoder flacEncoder;

    /**
     * Chemin du fichier FLAC.
     */
    private String cheminFichierFlac;

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(IntelligenceArtificielleDialogFlow.class);

    public IntelligenceArtificielleDialogFlow() {
        try {
            sessionsClient = SessionsClient.create();
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            session = SessionName.of("robot-1248", "1234567890"/*UUID.randomUUID().toString()*/);

            flacEncoder = new FLAC_FileEncoder();
            cheminFichierFlac = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "reconnaissance.flac";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Répond à une requête.
     *
     * @param requete la requête demandé (une phrase ou un fichier son)
     * @return la réponse issue de l'intelligence artificielle
     */
    public ReponseIntelligenceArtificielle repondreARequete(RequeteIntelligenceArtificielle requete) {

        if (StringUtils.isNotEmpty(requete.getInputText())) {
            return traiterRequeteTexte(requete.getInputText());
        } else if (StringUtils.isNotEmpty(requete.getInputWavFilePath())) {
            return traiterRequeteAudio(requete.getInputWavFilePath());
        }

        return null;
    }

    private ReponseIntelligenceArtificielle traiterRequeteTexte(String inputText) {
        // Set the text and language code for the query
        TextInput.Builder textInput = TextInput.newBuilder().setText(inputText).setLanguageCode(LANGUAGE_CODE);

        // Build the query with the TextInput
        QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

        // Performs the detect intent request
        DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

        // Display the query result
        QueryResult queryResult = response.getQueryResult();

        String responseText = queryResult.getFulfillmentText();

        ReponseIntelligenceArtificielle reponse = new ReponseIntelligenceArtificielle();
        reponse.setInputText(inputText);
        reponse.setOutputText(responseText);

        // TODO générer la réponse audio

        return reponse;
    }

    private ReponseIntelligenceArtificielle traiterRequeteAudio(String inputWavFilePath) {
        // Instructs the speech recognizer how to process the audio content.
        InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_FLAC) // audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16
                .setLanguageCode(LANGUAGE_CODE) // languageCode = "en-US"
                .setSampleRateHertz(44100) // sampleRateHertz = 16000
                .build();

        // Build the query with the InputAudioConfig
        QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

        // Read the bytes from the audio file
        try {
            File fichierWav = new File(inputWavFilePath);
            File fichierFlac = new File(cheminFichierFlac);
            flacEncoder.encode(fichierWav, fichierFlac);
            // Récupération du fichier FLAC
            Path path = Paths.get(cheminFichierFlac);
            byte[] inputAudio = Files.readAllBytes(Paths.get(cheminFichierFlac));

            // Build the DetectIntentRequest
            DetectIntentRequest request = DetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .setInputAudio(ByteString.copyFrom(inputAudio))
                    .build();

            // Performs the detect intent request
            DetectIntentResponse response = sessionsClient.detectIntent(request);

            // Display the query result
            QueryResult queryResult = response.getQueryResult();

            String responseText = queryResult.getFulfillmentText();

            ReponseIntelligenceArtificielle reponse = new ReponseIntelligenceArtificielle();
            reponse.setInputText(queryResult.getQueryText());
            reponse.setOutputText(responseText);

            // TODO générer la réponse audio

            return reponse;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
