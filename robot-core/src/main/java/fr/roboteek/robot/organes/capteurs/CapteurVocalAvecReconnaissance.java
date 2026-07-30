package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.SpeechProviderConfig;
import fr.roboteek.robot.services.providers.google.speech.recognizer.GoogleSpeechRecognizerService;
import fr.roboteek.robot.services.providers.vosk.speech.recognizer.VoskSpeechRecognizerService;
import fr.roboteek.robot.services.recognizer.SpeechRecognizerService;
import fr.roboteek.robot.services.recognizer.StreamingSpeechRecognizerService;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 * <p>
 * Migré en bean Spring : cycle de vie (acquisition micro) géré par {@link SmartLifecycle},
 * évènements de contrôle reçus via les évènements Spring (voir {@link AbstractCapteurVocal}).
 * <p>
 * Décode <b>au fil de la parole</b> quand le moteur le sait faire (Vosk, via
 * {@link StreamingSpeechRecognizerService}), et retombe sur le décodage après coup sinon (Google).
 * L'intérêt n'est pas seulement d'aller plus vite : en batch, le délai entre la fin de la phrase et
 * le texte croît avec la longueur de la phrase, puisque tout est décodé à ce moment-là. En
 * streaming, l'essentiel est déjà décodé quand la phrase se termine, et le délai devient à peu près
 * constant. Accessoirement, cela découpe aussi le décodage — qui a lieu sur le thread de capture —
 * en tranches courtes, au lieu d'un unique blocage pouvant dépasser la capacité du buffer de ligne.
 *
 * @author Nicolas
 */
@Component
public class CapteurVocalAvecReconnaissance extends AbstractCapteurVocal implements SmartLifecycle {

    /**
     * Speech recognizer.
     */
    private SpeechRecognizerService speechRecognizerService;

    /**
     * Le même moteur vu comme moteur de streaming, ou {@code null} s'il ne sait pas décoder au fil
     * de la parole (Google) : dans ce cas le comportement reste strictement celui d'avant.
     */
    private StreamingSpeechRecognizerService streamingRecognizerService;

    /**
     * Texte produit par {@link StreamingSpeechRecognizerService#finishPhrase()}, transmis de
     * {@link #surFinPhrase()} à {@link #traiterDetectionVocale(String)} — les deux étant appelés
     * successivement sur le seul thread de capture, aucune synchronisation n'est nécessaire.
     */
    private String texteStreamingDeLaPhrase;

    /**
     * Horodatage de la fin de phrase, pour mesurer le délai réellement perçu : de la détection de
     * fin de phrase jusqu'au texte disponible.
     */
    private long instantFinPhrase;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(CapteurVocalAvecReconnaissance.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    public CapteurVocalAvecReconnaissance() {
        super("CapteurVocalAvecReconnaissance");
    }

    @Override
    public void initialiser() {
        super.initialiser();

        SpeechProviderConfig providerConfig = Configurations.speechProviderConfig();
        switch (providerConfig.recognizerProvider()) {
            case VOSK -> {
                try {
                    speechRecognizerService = VoskSpeechRecognizerService.getInstance();
                } catch (Exception e) {
                    logger.error("Impossible d'initialiser Vosk, repli sur Google", e);
                    speechRecognizerService = GoogleSpeechRecognizerService.getInstance();
                }
            }
            case GOOGLE -> speechRecognizerService = GoogleSpeechRecognizerService.getInstance();
        }

        streamingRecognizerService = speechRecognizerService instanceof StreamingSpeechRecognizerService streaming
                ? streaming : null;
        logger.info("Reconnaissance vocale : décodage {}",
                streamingRecognizerService != null ? "au fil de la parole (streaming)" : "après coup (batch)");
    }

    @Override
    protected void surDebutPhrase(byte[] audioPreOnset) {
        if (streamingRecognizerService != null) {
            streamingRecognizerService.startPhrase(audioPreOnset);
        }
    }

    @Override
    protected void surBlocAudio(byte[] blocAudio) {
        if (streamingRecognizerService != null) {
            streamingRecognizerService.acceptAudioBlock(blocAudio);
        }
    }

    @Override
    protected void surFinPhrase() {
        instantFinPhrase = System.currentTimeMillis();
        if (streamingRecognizerService != null) {
            texteStreamingDeLaPhrase = streamingRecognizerService.finishPhrase();
        }
    }

    @Override
    protected void surInterruptionPhrase() {
        if (streamingRecognizerService != null) {
            streamingRecognizerService.cancelPhrase();
        }
        texteStreamingDeLaPhrase = null;
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        final String resultat;
        if (streamingRecognizerService != null) {
            // Déjà décodé au fil de la parole par surFinPhrase() : ne pas redécoder le fichier.
            resultat = texteStreamingDeLaPhrase;
            texteStreamingDeLaPhrase = null;
        } else {
            resultat = speechRecognizerService.recognize(cheminFichierWav);
        }
        // Mesure du délai qui compte pour l'utilisateur : de la fin de la phrase au texte prêt.
        logger.info("Texte reconnu {} ms après la fin de la phrase ({})",
                System.currentTimeMillis() - instantFinPhrase,
                streamingRecognizerService != null ? "streaming" : "batch");

        if (resultat != null && !resultat.trim().equals("")) {
            // Envoi de l'évènement de reconnaissance
            final ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
            event.setTexteReconnu(resultat);
            logger.debug("Résultat = {}", resultat);
            // Suppression du fichier
//					fichierWav.delete();
            // Lancement de l'évènement de reconnaissance vocale
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void start() {
        initialiser();
        super.start();
        running = true;
        logger.info("CapteurVocalAvecReconnaissance démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("CapteurVocalAvecReconnaissance arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.CAPTEURS;
    }
}
