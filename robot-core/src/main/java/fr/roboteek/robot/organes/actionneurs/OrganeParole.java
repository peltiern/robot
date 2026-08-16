package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.SpeechProviderConfig;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.configuration.speech.synthesis.piper.PiperSpeechSynthesisConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.GoogleSpeechSynthesizerService;
import fr.roboteek.robot.services.providers.piper.speech.synthesizer.PiperSpeechSynthesizerService;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerService;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleTermineeEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
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
import static fr.roboteek.robot.configuration.Configurations.piperSpeechSynthesisConfig;

/**
 * Organe permettant de synthétiser un texte (fournisseur cloud ou local, cf.
 * {@link SpeechProviderConfig}) et en appliquant des effets avec SOX.
 * <p>
 * Migré en bean Spring : cycle de vie géré par {@link SmartLifecycle},
 * évènements reçus via {@link EventListener} (relayés depuis le bus Guava par le pont
 * tant que la migration n'est pas terminée).
 */
@Component
public class OrganeParole extends AbstractOrgane implements SmartLifecycle {

    private final GoogleSpeechSynthesisConfig googleConfig;
    private final PiperSpeechSynthesisConfig piperConfig;
    private SpeechSynthesizerService speechSynthesizerService;
    private String fichierSyntheseVocale;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(OrganeParole.class);

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Constructeur.
     */
    public OrganeParole() {
        super();
        googleConfig = googleSpeechSynthesisConfig();
        piperConfig = piperSpeechSynthesisConfig();
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
            applicationEventPublisher.publishEvent(eventPause);
            try {
                lireEtAttendre(texte);
            } finally {
                // Les deux reprises sont dans un finally, et ce n'est pas de la précaution de
                // principe : la reconnaissance vocale est mise en pause AVANT la synthèse. Si
                // celle-ci échoue — Piper mort, fichier illisible, exception quelconque —, sans
                // ces lignes le robot resterait sourd définitivement, et qui attend la fin de la
                // phrase pour écouter la réponse attendrait pour rien.
                final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
                eventRedemarrage.setControle(CONTROLE.DEMARRER);
                applicationEventPublisher.publishEvent(eventRedemarrage);
                applicationEventPublisher.publishEvent(new ParoleTermineeEvent(texte));
            }
        }
    }

    private void lireEtAttendre(String texte) {
        logger.info("Lecture :\t{}", texte);

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
        SpeechProviderConfig providerConfig = Configurations.speechProviderConfig();
        switch (providerConfig.synthesizerProvider()) {
            case PIPER -> {
                try {
                    speechSynthesizerService = PiperSpeechSynthesizerService.getInstance();
                    fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + piperConfig.voiceFilter();
                } catch (Exception e) {
                    logger.error("Impossible d'initialiser Piper, repli sur Google", e);
                    speechSynthesizerService = GoogleSpeechSynthesizerService.getInstance();
                    fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + googleConfig.voiceFilter();
                }
            }
            case GOOGLE -> {
                speechSynthesizerService = GoogleSpeechSynthesizerService.getInstance();
                fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + googleConfig.voiceFilter();
            }
        }
    }

    @Override
    public void arreter() {

    }

    @Override
    public void start() {
        initialiser();
        running = true;
        logger.info("OrganeParole démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("OrganeParole arrêté");
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
