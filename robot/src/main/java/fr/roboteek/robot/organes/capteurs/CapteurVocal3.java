package fr.roboteek.robot.organes.capteurs;

import org.apache.log4j.Logger;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.server.WebSpeechServer;
import fr.roboteek.robot.server.WebSpeechServerListener;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.CONTROLE;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import net.engio.mbassy.listener.Handler;

/**
 * Capteur lié à la reconnaissance vocale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class CapteurVocal3 extends AbstractOrgane implements WebSpeechServerListener {

    /** Serveur Speech. */
    private WebSpeechServer speechWebServer;

    /** Logger. */
    private Logger logger = Logger.getLogger(CapteurVocal3.class);

    public CapteurVocal3() {
        super();

        speechWebServer = WebSpeechServer.getInstance();
        // Le capteur vocal est écouteur du serveur
        speechWebServer.addListener(this);
    }

    @Override
    public void initialiser() {
    }

    @Override
    public void arreter() {
    }

    /**
     * Intercepte les évènements de contrôle de la reconnaissance vocale.
     * @param reconnaissanceVocaleControleEvent évènement de contrôle de la reconnaissance vocale
     */
    @Handler
    public void handleReconnaissanceVocaleControleEvent(ReconnaissanceVocaleControleEvent reconnaissanceVocaleControleEvent) {
        if (reconnaissanceVocaleControleEvent.getControle() == CONTROLE.DEMARRER) {
            logger.debug("Démarrage de la reconnaissance vocale");
            //            microphone.clear();
            //            microphone.startRecording();
            //            misEnPause = false;
            //            recognizer.addResultListener(this);
        } else if (reconnaissanceVocaleControleEvent.getControle() == CONTROLE.METTRE_EN_PAUSE) {
            logger.debug("Mise en pause de la reconnaissance vocale");
            //            recognizer.removeResultListener(this);
            //            misEnPause = true;
            //            microphone.stopRecording();
        }
    }

    public void onSpeechResult(String speechResult) {
        if (speechResult != null && !speechResult.trim().equals("")) {
            // Envoi de l'évènement de reconnaissance
            final ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
            event.setTexteReconnu(speechResult);
            RobotEventBus.getInstance().publish(event);
        }
    }

}
