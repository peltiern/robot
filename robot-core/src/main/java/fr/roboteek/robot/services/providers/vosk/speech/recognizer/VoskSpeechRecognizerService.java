package fr.roboteek.robot.services.providers.vosk.speech.recognizer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.recognizer.vosk.VoskSpeechRecognizerConfig;
import fr.roboteek.robot.services.recognizer.SpeechRecognizerService;
import fr.roboteek.robot.services.recognizer.StreamingSpeechRecognizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Reconnaissance vocale locale avec Vosk : modèle chargé une fois en mémoire, aucun appel réseau.
 * <p>
 * <b>Aucun ré-échantillonnage</b> : la capture se fait directement à la fréquence native des
 * modèles Vosk (voir {@link Constantes#FREQUENCE_ECHANTILLONNAGE_CAPTURE_HZ}), donc l'audio est
 * transmis tel quel. C'est un point important et durement acquis : la conversion 44,1 kHz → 16 kHz
 * par l'API Java Sound n'applique aucun filtre anti-repliement, et surtout, faite par blocs
 * successifs, elle repart de zéro à chaque bloc — dérive de 2 échantillons par bloc et forme
 * d'onde altérée (13 à 32 dB de SNR résiduel), ce qui avait rendu la reconnaissance en streaming
 * inexploitable. Ne pas réintroduire de conversion ici.
 * <p>
 * Implémente aussi {@link StreamingSpeechRecognizerService} : le texte est décodé au fil de la
 * phrase ({@link Recognizer} persistant réutilisé via {@code reset()}). Ce chemin est totalement
 * indépendant de {@link #recognize(String)}, qui garde son propre {@link Recognizer} jetable.
 */
public class VoskSpeechRecognizerService implements SpeechRecognizerService, StreamingSpeechRecognizerService {

    private static final Logger logger = LoggerFactory.getLogger(VoskSpeechRecognizerService.class);

    private static final float FREQUENCE_ECHANTILLONNAGE_VOSK = Constantes.FREQUENCE_ECHANTILLONNAGE_CAPTURE_HZ;

    // Taille de chunk cible pour le streaming : ~400ms, 16 bits mono à la fréquence de capture.
    private static final int TAILLE_CHUNK_CIBLE_OCTETS = (int) (FREQUENCE_ECHANTILLONNAGE_VOSK * 2 * 0.4);

    static {
        // Le jar Maven de Vosk n'embarque que la lib native linux-x86_64 ; sur le Jetson
        // (aarch64), libvosk.so doit être déployé manuellement dans ce dossier (cf. PoC Vosk,
        // asset "vosk-linux-aarch64" des releases GitHub alphacep/vosk-api). Sans effet si le
        // dossier est absent : JNA retombe sur sa résolution habituelle (jar embarqué).
        System.setProperty("jna.library.path", Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "vosk" + File.separator + "native");
    }

    private static VoskSpeechRecognizerService instance;

    private final Model model;

    // Recognizer persistant dédié au streaming (indépendant de celui, jetable, de recognize()),
    // réutilisé entre phrases via reset() ; toujours appelé depuis le seul thread "Audio
    // Dispatcher" (voir AbstractCapteurVocal), donc pas de synchronisation nécessaire.
    private final Recognizer streamingRecognizer;
    private final ByteArrayOutputStream chunkEnCours = new ByteArrayOutputStream();
    private final StringBuilder texteStreamingAccumule = new StringBuilder();

    private VoskSpeechRecognizerService() {
        VoskSpeechRecognizerConfig config = Configurations.voskSpeechRecognizerConfig();
        String modelPath = Constantes.DOSSIER_RECONNAISSANCE_VOCALE + File.separator + "vosk" + File.separator + config.modelName();
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        try {
            model = new Model(modelPath);
            streamingRecognizer = new Recognizer(model, FREQUENCE_ECHANTILLONNAGE_VOSK);
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
        try (AudioInputStream fluxOriginal = AudioSystem.getAudioInputStream(new File(wavFilePath));
             AudioInputStream flux = adapterSiNecessaire(fluxOriginal, wavFilePath);
             Recognizer recognizer = new Recognizer(model, FREQUENCE_ECHANTILLONNAGE_VOSK)) {

            // acceptWaveForm() renvoie true dès que Vosk détecte lui-même une pause : il
            // finalise ET réinitialise alors son segment en cours. Sans consommer ce résultat
            // intermédiaire via getResult(), un silence au milieu du fichier (hésitation, ex.
            // "je m'appelle... euh... nicolas") ferait perdre tout ce qui précède la pause :
            // seul le dernier segment resterait dans getFinalResult().
            StringBuilder texte = new StringBuilder();
            byte[] tampon = new byte[4096];
            int lus;
            while ((lus = flux.read(tampon)) >= 0) {
                if (recognizer.acceptWaveForm(tampon, lus)) {
                    ajouterTexteResultat(texte, recognizer.getResult());
                }
            }
            ajouterTexteResultat(texte, recognizer.getFinalResult());
            return texte.toString();

        } catch (Exception e) {
            logger.error("Échec de la reconnaissance vocale Vosk pour {}", wavFilePath, e);
            return "";
        }
    }

    /**
     * Renvoie le flux tel quel s'il est déjà au format attendu par Vosk — le cas normal, la capture
     * se faisant à cette fréquence — et sinon le convertit en une seule passe continue.
     * <p>
     * Ce repli n'existe que pour les WAV d'origine externe (fichiers de test), {@code recognize()}
     * acceptant n'importe quel chemin. Il ne doit jamais se déclencher en fonctionnement normal,
     * d'où l'avertissement : s'il apparaît dans les logs, la capture et Vosk ne sont plus d'accord
     * sur la fréquence, ce qui dégrade la reconnaissance sans autre signe visible.
     */
    private static AudioInputStream adapterSiNecessaire(AudioInputStream flux, String wavFilePath) {
        AudioFormat format = flux.getFormat();
        boolean dejaAuBonFormat = format.getSampleRate() == FREQUENCE_ECHANTILLONNAGE_VOSK
                && format.getSampleSizeInBits() == 16
                && format.getChannels() == 1
                && !format.isBigEndian();
        if (dejaAuBonFormat) {
            return flux;
        }
        logger.warn("WAV {} en {} Hz / {} canaux / {} bits : conversion vers {} Hz mono 16 bits."
                        + " Attendu uniquement pour un fichier externe — sinon la capture a changé de format.",
                wavFilePath, format.getSampleRate(), format.getChannels(), format.getSampleSizeInBits(),
                FREQUENCE_ECHANTILLONNAGE_VOSK);
        return AudioSystem.getAudioInputStream(
                new AudioFormat(FREQUENCE_ECHANTILLONNAGE_VOSK, 16, 1, true, false), flux);
    }

    @Override
    public void startPhrase(byte[] preRollAudio) {
        streamingRecognizer.reset();
        chunkEnCours.reset();
        texteStreamingAccumule.setLength(0);
        if (preRollAudio.length > 0) {
            nourrirChunk(preRollAudio);
        }
    }

    @Override
    public void acceptAudioBlock(byte[] audioBlock) {
        chunkEnCours.writeBytes(audioBlock);
        if (chunkEnCours.size() >= TAILLE_CHUNK_CIBLE_OCTETS) {
            nourrirChunk(chunkEnCours.toByteArray());
            chunkEnCours.reset();
        }
    }

    @Override
    public String finishPhrase() {
        if (chunkEnCours.size() > 0) {
            nourrirChunk(chunkEnCours.toByteArray());
            chunkEnCours.reset();
        }
        try {
            ajouterTexteResultat(texteStreamingAccumule, streamingRecognizer.getFinalResult());
            return texteStreamingAccumule.toString();
        } catch (Exception e) {
            logger.error("Échec de la reconnaissance vocale Vosk (streaming)", e);
            return "";
        } finally {
            texteStreamingAccumule.setLength(0);
        }
    }

    @Override
    public void cancelPhrase() {
        streamingRecognizer.reset();
        chunkEnCours.reset();
        texteStreamingAccumule.setLength(0);
    }

    /**
     * Transmet un bloc audio à Vosk tel quel. Aucune conversion : c'est précisément la conversion
     * par bloc, qui repartait de zéro à chaque appel, qui avait rendu ce chemin inexploitable (voir
     * l'en-tête de la classe). Le découpage en chunks n'a désormais plus aucune incidence sur le
     * signal transmis — il ne détermine que la granularité du décodage.
     */
    private void nourrirChunk(byte[] blocAudio) {
        // Voir le commentaire de recognize() : true = Vosk finalise + réinitialise en interne
        // ce segment, il faut donc récupérer ce résultat intermédiaire avant de continuer.
        if (streamingRecognizer.acceptWaveForm(blocAudio, blocAudio.length)) {
            String resultatSegment = streamingRecognizer.getResult();
            // En DEBUG : la segmentation interne s'est révélée fréquente en usage réel (Vosk
            // finalise dès une courte pause au milieu d'une phrase), et le JSON multiligne
            // noierait les logs. À repasser en INFO pour diagnostiquer une phrase fragmentée.
            logger.debug("Segment intermédiaire Vosk (silence interne détecté) : {}", resultatSegment);
            ajouterTexteResultat(texteStreamingAccumule, resultatSegment);
        }
    }

    /**
     * Extrait le champ "text" d'un résultat JSON Vosk (résultat intermédiaire ou final) et
     * l'ajoute, séparé par un espace, au texte déjà accumulé.
     */
    private static void ajouterTexteResultat(StringBuilder texteAccumule, String resultatJson) {
        JsonObject resultat = JsonParser.parseString(resultatJson).getAsJsonObject();
        if (resultat.has("text")) {
            String texte = resultat.get("text").getAsString();
            if (!texte.isEmpty()) {
                if (texteAccumule.length() > 0) {
                    texteAccumule.append(' ');
                }
                texteAccumule.append(texte);
            }
        }
    }
}
