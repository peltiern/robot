package fr.roboteek.robot.organes.actionneurs;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.services.providers.google.speech.synthesizer.GoogleSpeechSynthesizerService;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerService;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static fr.roboteek.robot.configuration.Configurations.googleSpeechSynthesisConfig;

/**
 * Organe permettant de synthétiser un texte en passant par la synthèse vocale de Google
 * et en appliquant des effets avec SOX.
 */
public class OrganeParoleGoogle extends AbstractOrgane {

    private final GoogleSpeechSynthesisConfig config;
    private SpeechSynthesizerService speechSynthesizerService;
    private String fichierSyntheseVocale;

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(OrganeParoleGoogle.class);

    /**
     * Constructeur.
     */
    public OrganeParoleGoogle() {
        super();
        config = googleSpeechSynthesisConfig();
    }

    public static void main(String[] args) {

        final OrganeParoleGoogle organeParole = new OrganeParoleGoogle();
        organeParole.initialiser();

        organeParole.lire("Bonjour. Mon nom est Wall-E et je suis content de te rencontrer. Quel est ton nom ?");
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

            System.out.println(Thread.currentThread().getName() + " say (lecture) : " + texte);

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            logger.info("Avant appel : " + System.currentTimeMillis());
            byte[] audioContents = speechSynthesizerService.synthesize(texte);
            logger.info("Après appel : " + System.currentTimeMillis());

            if (audioContents != null) {
                // Write the response to the output file.
                String pathOutputFile = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "output-" + System.currentTimeMillis() + ".wav";
                try (OutputStream out = new FileOutputStream(pathOutputFile)) {
                    out.write(audioContents);
                    logger.info("Fin d'écriture dans le fichier : " + System.currentTimeMillis());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                String[] params = {fichierSyntheseVocale, pathOutputFile};
                try {
                    Process p = Runtime.getRuntime().exec(params);
                    p.waitFor();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                System.out.println("Fin Lecture :\t" + texte);
                logger.debug("Fin lecture :\t" + texte);
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
    @Subscribe
    public void handleParoleEvent(ParoleEvent paroleEvent) {
        System.out.println("ParoleEvent = " + paroleEvent);
        if (StringUtils.isNotBlank(paroleEvent.getTexte())) {
            System.out.println(Thread.currentThread().getName() + " say (avant lecture) : " + paroleEvent.getTexte());
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
}
