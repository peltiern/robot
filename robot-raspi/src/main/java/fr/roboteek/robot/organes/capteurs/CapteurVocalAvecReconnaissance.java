package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.speech.recognizer.SpeechRecognizer;
import fr.roboteek.robot.util.speech.recognizer.google.GoogleSpeechRecognizer;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 *
 * @author Nicolas
 */
public class CapteurVocalAvecReconnaissance extends AbstractCapteurVocal {

    /** Speech recognizer. */
    private SpeechRecognizer speechRecognizer;

    public CapteurVocalAvecReconnaissance() {
        super("CapteurVocalAvecReconnaissance");
    }

    @Override
    public void initialiser() {
        super.initialiser();

        // TODO Gestion dynamique de la reconnaisance vocale (par fichier de config à un niveau supérieur)
        speechRecognizer = GoogleSpeechRecognizer.getInstance();
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        // Appel du moteur de reconnaissance
        long debut = System.currentTimeMillis();
        final String resultat = speechRecognizer.recognize(cheminFichierWav);
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
