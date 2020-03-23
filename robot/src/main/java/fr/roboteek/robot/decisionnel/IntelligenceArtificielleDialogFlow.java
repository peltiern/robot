package fr.roboteek.robot.decisionnel;

import com.google.cloud.dialogflow.v2.AudioEncoding;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.InputAudioConfig;
import com.google.cloud.dialogflow.v2.OutputAudioConfig;
import com.google.cloud.dialogflow.v2.OutputAudioEncoding;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SsmlVoiceGender;
import com.google.cloud.dialogflow.v2.SynthesizeSpeechConfig;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.dialogflow.v2.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import fr.roboteek.robot.Constantes;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IntelligenceArtificielleDialogFlow {

    private static final String LANGUAGE_CODE = "fr-FR";

    /**
     * Session DialogFlow.
     */
    private SessionsClient sessionsClient;

    private SessionName session;

    private OutputAudioConfig outputAudioConfig;

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

        // Initialisation de la configuration statique de DialogFlow
        initDialogFlowConfig();

        // Initialisation de l'encodeur FLAC
        flacEncoder = new FLAC_FileEncoder();
        cheminFichierFlac = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "reconnaissance.flac";
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
        TextInput textInput = TextInput.newBuilder()
                .setText(inputText)
                .setLanguageCode(LANGUAGE_CODE)
                .build();

        // Build the query with the TextInput
        QueryInput queryInput = QueryInput.newBuilder()
                .setText(textInput)
                .build();

        DetectIntentRequest request =
                DetectIntentRequest.newBuilder()
                        .setQueryInput(queryInput)
                        .setOutputAudioConfig(outputAudioConfig)
                        .setSession(session.toString())
                        .build();

        // Performs the detect intent request
        DetectIntentResponse response = sessionsClient.detectIntent(request);

        return mapDialogFlowResponse(response);
    }

    private ReponseIntelligenceArtificielle traiterRequeteAudio(String inputWavFilePath) {
        // Instructs the speech recognizer how to process the audio content.
        InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_FLAC)
                .setLanguageCode(LANGUAGE_CODE)
                .setSampleRateHertz(44100)
                .build();

        // Build the query with the InputAudioConfig
        QueryInput queryInput = QueryInput.newBuilder()
                .setAudioConfig(inputAudioConfig)
                .build();

        // Read the bytes from the audio file
        try {
            File fichierWav = new File(inputWavFilePath);
            File fichierFlac = new File(cheminFichierFlac);
            flacEncoder.encode(fichierWav, fichierFlac);
            // Récupération du fichier FLAC;
            byte[] inputAudio = Files.readAllBytes(Paths.get(cheminFichierFlac));

            // Build the DetectIntentRequest
            DetectIntentRequest request = DetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .setInputAudio(ByteString.copyFrom(inputAudio))
                    .setOutputAudioConfig(outputAudioConfig)
                    .build();

            // Performs the detect intent request
            DetectIntentResponse response = sessionsClient.detectIntent(request);

            return mapDialogFlowResponse(response);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initDialogFlowConfig() {
        try {
            sessionsClient = SessionsClient.create();
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            session = SessionName.of("robot-1248", "1234567890"/*UUID.randomUUID().toString()*/);

            // Configuration de la synthèse vocale
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setName("fr-FR-Wavenet-C")
                    .setSsmlGender(SsmlVoiceGender.SSML_VOICE_GENDER_NEUTRAL)
                    .build();

            SynthesizeSpeechConfig synthesizeSpeechConfig = SynthesizeSpeechConfig.newBuilder()
                    .setVoice(voice)
                    .build();

            outputAudioConfig = OutputAudioConfig.newBuilder()
                    .setAudioEncoding(OutputAudioEncoding.OUTPUT_AUDIO_ENCODING_LINEAR_16)
                    .setSampleRateHertz(16000)
                    .setSynthesizeSpeechConfig(synthesizeSpeechConfig)
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ReponseIntelligenceArtificielle mapDialogFlowResponse(DetectIntentResponse response) {
        QueryResult queryResult = response.getQueryResult();
        String responseText = queryResult.getFulfillmentText();

        ReponseIntelligenceArtificielle reponse = new ReponseIntelligenceArtificielle();
        reponse.setInputText(queryResult.getQueryText());
        reponse.setOutputText(responseText);
        reponse.setOutputAudio(response.getOutputAudio().toByteArray());
        return reponse;
    }
}
