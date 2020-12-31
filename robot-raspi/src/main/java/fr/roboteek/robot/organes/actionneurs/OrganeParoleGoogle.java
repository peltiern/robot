package fr.roboteek.robot.organes.actionneurs;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.common.eventbus.Subscribe;
import com.google.protobuf.ByteString;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static fr.roboteek.robot.configuration.Configurations.googleSpeechSynthesisConfig;

/**
 * Organe permettant de synthétiser un texte en passant par la synthèse vocale de Google
 * et en appliquant des effets avec SOX.
 */
public class OrganeParoleGoogle extends AbstractOrgane {

    private String fichierSyntheseVocale;

    private TextToSpeechClient textToSpeechClient;

    private GoogleSpeechSynthesisConfig config;

    private VoiceSelectionParams voice;

    private AudioConfig audioConfig;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(OrganeParoleGoogle.class);

    /** Constructeur. */
    public OrganeParoleGoogle() {
        super();
        config = googleSpeechSynthesisConfig();
    }

    /**
     * Lit un texte.
     * @param texte le texte à dire
     */
    public void lire(String texte) {
        if(texte != null && !texte.isEmpty()) {
            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
//            RobotEventBus.getInstance().publish(eventPause);

            System.out.println(Thread.currentThread().getName() + " say (lecture) : " + texte);

            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(texte)
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
            String pathOutputFile = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "output-" + System.currentTimeMillis() + ".wav";
            try (OutputStream out = new FileOutputStream(pathOutputFile)) {
                out.write(audioContents.toByteArray());
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
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("Fin Lecture :\t" + texte);
            logger.debug("Fin lecture :\t" + texte);

            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
            eventRedemarrage.setControle(CONTROLE.DEMARRER);
            RobotEventBus.getInstance().publishAsync(eventRedemarrage);
        }
    }

    /**
     * Lit un texte issu d'un contenu audio.
     * @param audioContent le texte à dire
     */
    public void lire(byte[] audioContent) {
        if (audioContent != null) {
            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(CONTROLE.METTRE_EN_PAUSE);
//            RobotEventBus.getInstance().publish(eventPause);

            System.out.println("Lecture issu d'un contenu audio :\t" + audioContent);

            // Write the response to the output file.
            String pathOutputFile = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "output-" + System.currentTimeMillis() + ".wav";
            try (OutputStream out = new FileOutputStream(pathOutputFile)) {
                out.write(audioContent);
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
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            System.out.println("Fin Lecture :\t" + audioContent);
            logger.debug("Fin lecture :\t" + audioContent);

            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
            eventRedemarrage.setControle(CONTROLE.DEMARRER);
            RobotEventBus.getInstance().publishAsync(eventRedemarrage);
        }
    }
    
    /**
     * Intercepte les évènements pour lire du texte.
     * @param paroleEvent évènement pour lire du texte
     */
    @Subscribe
    public void handleParoleEvent(ParoleEvent paroleEvent) {
    	System.out.println("ParoleEvent = " + paroleEvent);
    	if (paroleEvent.getAudioContent() != null) {
    	    lire(paroleEvent.getAudioContent());
        } else if (paroleEvent.getTexte() != null && !paroleEvent.getTexte().trim().equals("")) {
            System.out.println(Thread.currentThread().getName() + " say (avant lecture) : " + paroleEvent.getTexte());
            lire(paroleEvent.getTexte().trim());
        }
    }

    @Override
    public void initialiser() {
        fichierSyntheseVocale = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + config.voiceFilter();
        try {
            textToSpeechClient = TextToSpeechClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Build the voice request
        voice = VoiceSelectionParams.newBuilder()
                .setLanguageCode(config.languageCode())
                .setName(config.voiceName())
                .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                .build();

        // Select the type of audio file you want returned
        audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.LINEAR16)
                .build();
    }

    @Override
    public void arreter() {
        
    }
    
    public static void main(String[] args) {
        
        final OrganeParoleGoogle organeParole = new OrganeParoleGoogle();
        organeParole.initialiser();

        organeParole.lire("Bonjour. Mon nom est Wally et je suis content de te rencontrer. Quel est ton nom ?");
    }

}
