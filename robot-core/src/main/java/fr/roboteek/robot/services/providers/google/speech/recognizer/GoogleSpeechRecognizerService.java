package fr.roboteek.robot.services.providers.google.speech.recognizer;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.ApiKeys;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.recognizer.google.GoogleSpeechRecognizerConfig;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.GoogleSpeechToTextClient;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request.AudioEncoding;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request.RecognitionAudio;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request.RecognitionConfig;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request.RecognizeRequest;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response.RecognizeResponse;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response.SpeechRecognitionAlternative;
import fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.response.SpeechRecognitionResult;
import fr.roboteek.robot.services.recognizer.SpeechRecognizerService;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * Speech recognition engine with Google Cloud Speech Service.
 */
public class GoogleSpeechRecognizerService implements SpeechRecognizerService {

    /**
     * Class singleton.
     */
    private static GoogleSpeechRecognizerService instance;

    /**
     * FLAC encoder WAV --> FLAC.
     */
    private final FLAC_FileEncoder flacEncoder;

    /**
     * FLAC file path.
     */
    private final String flacFilePath;

    private GoogleSpeechToTextClient client;
    private RecognitionConfig recognitionConfig;

    /**
     * Private constructor.
     */
    private GoogleSpeechRecognizerService() {

        GoogleSpeechRecognizerConfig config = Configurations.googleSpeechRecognizerConfig();

        flacEncoder = new FLAC_FileEncoder();
        flacFilePath = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "google" + File.separator + "recognition.flac";

        client = Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(GoogleSpeechToTextClient.class, "https://speech.googleapis.com");

        recognitionConfig = new RecognitionConfig(AudioEncoding.FLAC, 44100, 1, config.languageCode());
    }

    /**
     * Get the instance of the speech recognition engine.
     *
     * @return the instance of the speech recognition engine
     */
    public static GoogleSpeechRecognizerService getInstance() {
        if (instance == null) {
            instance = new GoogleSpeechRecognizerService();
        }
        return instance;
    }

    public static void main(String[] args) {
        final SpeechRecognizerService googleRecognizerRest = GoogleSpeechRecognizerService.getInstance();
        long start = System.currentTimeMillis();
        final String result = googleRecognizerRest.recognize(Constantes.DOSSIER_TESTS + File.separator + "test.wav");
        long end = System.currentTimeMillis();
        System.out.println("Result = " + result);
        System.out.println("Time = " + (end - start));
    }

    public String recognize(String wavFilePath) {
        //System.out.println("RECOGNIZER = " + wavFilePath + ", " + flacFilePath);
        File wavFile = new File(wavFilePath);
        File flacFile = new File(flacFilePath);
        flacEncoder.encode(wavFile, flacFile);
        // Récupération du fichier FLAC
        Path path = Paths.get(flacFilePath);
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        RecognitionAudio recognitionAudio = new RecognitionAudio(Base64.getEncoder().encodeToString(data));
        RecognizeRequest request = new RecognizeRequest(recognitionConfig, recognitionAudio);

        try {
            RecognizeResponse response = client.recognize(request, ApiKeys.GOOGLE_SPEECH_API_KEY);

            // Get the result
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        //		fichierFlac.delete();
        return "";
    }
}
