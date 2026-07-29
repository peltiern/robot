package fr.roboteek.robot.services.providers.vosk.speech.recognizer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.recognizer.vosk.VoskSpeechRecognizerConfig;
import fr.roboteek.robot.services.recognizer.SpeechRecognizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

/**
 * Reconnaissance vocale locale avec Vosk : modèle chargé une fois en mémoire, aucun appel
 * réseau. Le micro capture en 44,1 kHz ({@link fr.roboteek.robot.organes.capteurs.AbstractCapteurVocal})
 * alors que Vosk attend du 16 kHz mono 16 bits : le WAV reçu est rééchantillonné à la volée
 * (API Java Sound), comme validé par le PoC ({@code fr.roboteek.robot.poc.VoskRecognitionPoc}).
 */
public class VoskSpeechRecognizerService implements SpeechRecognizerService {

    private static final Logger logger = LoggerFactory.getLogger(VoskSpeechRecognizerService.class);

    private static final float FREQUENCE_ECHANTILLONNAGE_VOSK = 16000f;

    static {
        // Le jar Maven de Vosk n'embarque que la lib native linux-x86_64 ; sur le Jetson
        // (aarch64), libvosk.so doit être déployé manuellement dans ce dossier (cf. PoC Vosk,
        // asset "vosk-linux-aarch64" des releases GitHub alphacep/vosk-api). Sans effet si le
        // dossier est absent : JNA retombe sur sa résolution habituelle (jar embarqué).
        System.setProperty("jna.library.path", Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "vosk" + File.separator + "native");
    }

    private static VoskSpeechRecognizerService instance;

    private final Model model;

    private VoskSpeechRecognizerService() {
        VoskSpeechRecognizerConfig config = Configurations.voskSpeechRecognizerConfig();
        String modelPath = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "vosk" + File.separator + config.modelName();
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        try {
            model = new Model(modelPath);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger le modèle Vosk : " + modelPath, e);
        }
        logger.info("Modèle Vosk chargé : {}", modelPath);
    }

    public static synchronized VoskSpeechRecognizerService getInstance() {
        if (instance == null) {
            instance = new VoskSpeechRecognizerService();
        }
        return instance;
    }

    @Override
    public String recognize(String wavFilePath) {
        try (AudioInputStream fluxOriginal = AudioSystem.getAudioInputStream(new File(wavFilePath))) {
            AudioFormat formatCible = new AudioFormat(FREQUENCE_ECHANTILLONNAGE_VOSK, 16, 1, true, false);
            try (AudioInputStream fluxConverti = AudioSystem.getAudioInputStream(formatCible, fluxOriginal);
                 Recognizer recognizer = new Recognizer(model, FREQUENCE_ECHANTILLONNAGE_VOSK)) {

                byte[] tampon = new byte[4096];
                int lus;
                while ((lus = fluxConverti.read(tampon)) >= 0) {
                    recognizer.acceptWaveForm(tampon, lus);
                }

                JsonObject resultat = JsonParser.parseString(recognizer.getFinalResult()).getAsJsonObject();
                return resultat.has("text") ? resultat.get("text").getAsString() : "";
            }
        } catch (Exception e) {
            logger.error("Échec de la reconnaissance vocale Vosk pour {}", wavFilePath, e);
            return "";
        }
    }
}
