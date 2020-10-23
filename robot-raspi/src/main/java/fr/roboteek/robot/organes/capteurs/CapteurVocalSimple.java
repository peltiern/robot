package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.systemenerveux.event.DetectionVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 *
 * @author Nicolas
 */
public class CapteurVocalSimple extends AbstractCapteurVocal {

    public CapteurVocalSimple() {
        super();
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        // Envoi de l'évènement de détection vocale
        final DetectionVocaleEvent event = new DetectionVocaleEvent();
        event.setCheminFichier(cheminFichierWav);
        RobotEventBus.getInstance().publishAsync(event);
    }

}
