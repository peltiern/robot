package fr.roboteek.robot.util.speech.recognizer.google;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.recognizer.google.GoogleSpeechRecognizerConfig;
import fr.roboteek.robot.util.speech.recognizer.SpeechRecognizer;
import net.sourceforge.javaflacencoder.FLAC_FileEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Speech recognition engine with Google Cloud Speech Service.
 */
public class GoogleSpeechRecognizer implements SpeechRecognizer {

    /**
     * Class singleton.
     */
    private static GoogleSpeechRecognizer instance;

    /**
     * FLAC encoder WAV --> FLAC.
     */
    private FLAC_FileEncoder flacEncoder;

    /**
     * FLAC file path.
     */
    private String flacFilePath;

    private GoogleSpeechRecognizerConfig config;

    private SpeechClient speechClient;

    private RecognitionConfig recognitionConfig;

    /**
     * Private constructor.
     */
    private GoogleSpeechRecognizer() {

        config = Configurations.googleSpeechRecognizerConfig();

        flacEncoder = new FLAC_FileEncoder();
        flacFilePath = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "google" + File.separator + "recognition.flac";

        try {
            speechClient = SpeechClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Config
        recognitionConfig =
                RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                        .setSampleRateHertz(44100)
                        //.setAudioChannelCount(2)
                        .setLanguageCode(config.languageCode())
                        .build();
    }

    /**
     * Get the instance of the speech recognition engine.
     *
     * @return the instance of the speech recognition engine
     */
    public static GoogleSpeechRecognizer getInstance() {
        if (instance == null) {
            instance = new GoogleSpeechRecognizer();
        }
        return instance;
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
        ByteString audioBytes = ByteString.copyFrom(data);

        RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

        RecognizeResponse response = speechClient.recognize(recognitionConfig, audio);

        // Get the result
        if (response != null) {
            List<SpeechRecognitionResult> listeRecognitionResults = response.getResultsList();
            //		System.out.println("Reponse = " + response);
            if (listeRecognitionResults != null && !listeRecognitionResults.isEmpty()) {
                SpeechRecognitionResult result = listeRecognitionResults.get(0);
                final List<SpeechRecognitionAlternative> listeAlternatives = result.getAlternativesList();
                if (listeAlternatives != null && !listeAlternatives.isEmpty()) {
                    SpeechRecognitionAlternative alternative = listeAlternatives.get(0);
                    return alternative.getTranscript();
                }
            }
        }
        //		fichierFlac.delete();
        return "";
    }

    public static void main(String[] args) {
        final SpeechRecognizer googleRecognizerRest = GoogleSpeechRecognizer.getInstance();
        long start = System.currentTimeMillis();
        final String result = googleRecognizerRest.recognize(Constantes.DOSSIER_TESTS + File.separator + "test.wav");
        long end = System.currentTimeMillis();
        System.out.println("Result = " + result);
        System.out.println("Time = " + (end - start));
    }
}
