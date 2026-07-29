package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.SpeechProviderConfig;
import fr.roboteek.robot.services.providers.google.speech.recognizer.GoogleSpeechRecognizerService;
import fr.roboteek.robot.services.providers.vosk.speech.recognizer.VoskSpeechRecognizerService;
import fr.roboteek.robot.services.recognizer.SpeechRecognizerService;
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
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        // Appel du moteur de reconnaissance
        long debut = System.currentTimeMillis();
        final String resultat = speechRecognizerService.recognize(cheminFichierWav);
        logger.debug("Temps reconnaissance : {} ms", System.currentTimeMillis() - debut);

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
