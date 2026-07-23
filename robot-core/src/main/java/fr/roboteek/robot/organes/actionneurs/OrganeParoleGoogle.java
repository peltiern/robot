package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.GoogleSpeechSynthesizerService;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerService;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static fr.roboteek.robot.configuration.Configurations.googleSpeechSynthesisConfig;

/**
 * Organe permettant de synthétiser un texte en passant par la synthèse vocale de Google
 * et en appliquant des effets avec SOX.
 * <p>
 * Migré en bean Spring : cycle de vie géré par {@link SmartLifecycle},
 * évènements reçus via {@link EventListener} (relayés depuis le bus Guava par le pont
 * tant que la migration n'est pas terminée).
 */
@Component
public class OrganeParoleGoogle extends AbstractOrgane implements SmartLifecycle {

    private final GoogleSpeechSynthesisConfig config;
    private SpeechSynthesizerService speechSynthesizerService;
    private String fichierSyntheseVocale;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(OrganeParoleGoogle.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Constructeur.
     */
    public OrganeParoleGoogle() {
        super();
        config = googleSpeechSynthesisConfig();
    }

    /**
     * Lit un texte.
     *
     * @param texte le texte à dire
     */
    public void lire(String texte) {
        if (texte != null && !texte.isEmpty()) {
            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
//            RobotEventBus.getInstance().publish(eventPause);

            logger.debug("Lecture :\t{}", texte);

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            byte[] audioContents = speechSynthesizerService.synthesize(texte);

            if (audioContents != null) {
                // Write the response to the output file.
                String pathOutputFile = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "output-" + System.currentTimeMillis() + ".wav";
                try (OutputStream out = new FileOutputStream(pathOutputFile)) {
                    out.write(audioContents);
                } catch (IOException e) {
                    logger.error("Erreur lors de l'écriture du fichier de synthèse vocale {}", pathOutputFile, e);
                }

                try {
                    Process p = new ProcessBuilder(fichierSyntheseVocale, pathOutputFile).start();
                    p.waitFor();
                } catch (IOException e) {
                    logger.error("Erreur lors de la lecture du fichier de synthèse vocale {}", pathOutputFile, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Lecture de la synthèse vocale interrompue");
                }

                logger.debug("Fin lecture :\t{}", texte);
            }

            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
            eventRedemarrage.setControle(CONTROLE.DEMARRER);
            RobotEventBus.getInstance().publishAsync(eventRedemarrage);
        }
    }

    /**
     * Intercepte les évènements pour lire du texte.
     *
     * @param paroleEvent évènement pour lire du texte
     */
    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        if (running && StringUtils.isNotBlank(paroleEvent.getTexte())) {
            lire(paroleEvent.getTexte().trim());
        }
    }

    @Override
    public void initialiser() {
        fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + config.voiceFilter();
        // TODO Gestion dynamique de la synthèse vocale (par fichier de config à un niveau supérieur)
        speechSynthesizerService = GoogleSpeechSynthesizerService.getInstance();
    }

    @Override
    public void arreter() {

    }

    @Override
    public void start() {
        initialiser();
        running = true;
        logger.info("OrganeParoleGoogle démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("OrganeParoleGoogle arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.ACTIONNEURS;
    }
}
