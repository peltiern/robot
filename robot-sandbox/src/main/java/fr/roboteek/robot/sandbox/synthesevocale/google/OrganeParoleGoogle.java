package fr.roboteek.robot.sandbox.synthesevocale.google;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class OrganeParoleGoogle {

    private static Logger logger = Logger.getLogger(OrganeParoleGoogle.class);

    /**
     * Demonstrates using the Text-to-Speech API.
     */
    public static void main(String... args) throws Exception {
        // Instantiates a client
        logger.info("Avant : " + System.currentTimeMillis());
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            logger.info("Début : " + System.currentTimeMillis());
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText("Bonjour Nicolas. Comment vas-tu aujourd'hui ?")
                    .build();

            // Build the voice request, select the language code ("en-US") and the ssml voice gender
            // ("neutral")
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("fr-FR")
                    .setName("fr-FR-Wavenet-C")
                    .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                    .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16)
                    //.addEffectsProfileId("medium-bluetooth-speaker-class-device")
                    .addEffectsProfileId("large-home-entertainment-class-device")
                    .build();

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            logger.info("Avant appel : " + System.currentTimeMillis());
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice,
                    audioConfig);
            logger.info("Après appel : " + System.currentTimeMillis());

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            try (OutputStream out = new FileOutputStream("output2.wav")) {
                out.write(audioContents.toByteArray());
                logger.info("Fin d'écriture dans le fichier : " + System.currentTimeMillis());
            }
        }
    }

}
