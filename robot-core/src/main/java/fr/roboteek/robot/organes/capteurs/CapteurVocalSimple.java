package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.systemenerveux.event.DetectionVocaleEvent;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 *
 * @author Nicolas
 */
public class CapteurVocalSimple extends AbstractCapteurVocal {

    public CapteurVocalSimple() {
        super("CapteurVocalSimple");
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        // Envoi de l'évènement de détection vocale
        final DetectionVocaleEvent event = new DetectionVocaleEvent();
        event.setCheminFichier(cheminFichierWav);
        applicationEventPublisher.publishEvent(event);
    }
}
