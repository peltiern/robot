package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.services.providers.google.speech.recognizer.GoogleSpeechRecognizerService;
import fr.roboteek.robot.services.recognizer.SpeechRecognizerService;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 *
 * @author Nicolas
 */
public class CapteurVocalAvecReconnaissance extends AbstractCapteurVocal {

    /**
     * Speech recognizer.
     */
    private SpeechRecognizerService speechRecognizerService;

    public CapteurVocalAvecReconnaissance() {
        super("CapteurVocalAvecReconnaissance");
    }

    @Override
    public void initialiser() {
        super.initialiser();

        // TODO Gestion dynamique de la reconnaisance vocale (par fichier de config à un niveau supérieur)
        speechRecognizerService = GoogleSpeechRecognizerService.getInstance();
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        // Appel du moteur de reconnaissance
        long debut = System.currentTimeMillis();
        final String resultat = speechRecognizerService.recognize(cheminFichierWav);
        long fin = System.currentTimeMillis();
        System.out.println("Temps reconnaissance : " + (fin - debut));

        if (resultat != null && !resultat.trim().equals("")) {
            // Envoi de l'évènement de reconnaissance
            final ReconnaissanceVocaleEvent event = new ReconnaissanceVocaleEvent();
            event.setTexteReconnu(resultat);
            System.out.println("Résultat = " + resultat);
            // Suppression du fichier
//					fichierWav.delete();
            // Lancement de l'évènement de reconnaissance vocale
            RobotEventBus.getInstance().publishAsync(event);
        }
    }
}
