package fr.roboteek.robot.services.providers.google.speech.synthesizer;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import fr.roboteek.robot.configuration.ApiKeys;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.client.GoogleTextToSpeechClient;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.request.*;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.response.SynthesizeResponse;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerService;

import java.util.Base64;

import static fr.roboteek.robot.configuration.Configurations.googleSpeechSynthesisConfig;

public class GoogleSpeechSynthesizerService implements SpeechSynthesizerService {

    /**
     * Classe singleton.
     */
    private static GoogleSpeechSynthesizerService instance;

    private final GoogleTextToSpeechClient client;
    private final Voice voice;
    private final AudioConfig audioConfig;

    private GoogleSpeechSynthesizerService() {
        GoogleSpeechSynthesisConfig config = googleSpeechSynthesisConfig();
        client = Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(GoogleTextToSpeechClient.class, "https://texttospeech.googleapis.com");

        voice = new Voice(config.languageCode(), config.voiceName());
        audioConfig = new AudioConfig(AudioEncoding.LINEAR16);
    }

    public static GoogleSpeechSynthesizerService getInstance() {
        if (instance == null) {
            instance = new GoogleSpeechSynthesizerService();
        }
        return instance;
    }

    @Override
    public byte[] synthesize(String texte) {
        // Construire la requête Text-to-Speech
        SynthesizeRequest request = new SynthesizeRequest(new Input(texte), voice, audioConfig);
        SynthesizeResponse response = client.synthesizeText(request, ApiKeys.GOOGLE_SPEECH_API_KEY);

        // Décodage du contenu
        byte[] audioContents = null;
        if (response != null && response.getAudioContent() != null) {
            audioContents = Base64.getDecoder().decode(response.getAudioContent());
        }
        return audioContents;
    }
}
