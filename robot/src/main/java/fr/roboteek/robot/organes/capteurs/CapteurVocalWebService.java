package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.organes.actionneurs.OrganeParoleEspeak;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.reconnaissance.vocale.SpeechRecognizer;
import fr.roboteek.robot.util.reconnaissance.vocale.bing.BingSpeechRecognizerRest;

/**
 * Capteur vocal avec appel d'un web service externe pour effectuer la reconnaisance vocale.
 *
 * @author Nicolas
 */
public class CapteurVocalWebService extends AbstractCapteurVocal {

    /**
     * Moteur de reconnaissance vocale.
     */
    private SpeechRecognizer recognizer;


    public CapteurVocalWebService(SpeechRecognizer recognizer) {
        super();

        this.recognizer = recognizer;
    }

    @Override
    public void traiterDetectionVocale(String cheminFichierWav) {
        // Appel du moteur de reconnaissance
        long debut = System.currentTimeMillis();
        final String resultat = recognizer.reconnaitre(cheminFichierWav);
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

    public static void main(String[] args) {
        OrganeParoleEspeak organeParole = new OrganeParoleEspeak();
        RobotEventBus.getInstance().subscribe(organeParole);
        final CapteurVocalWebService capteurVocal = new CapteurVocalWebService(BingSpeechRecognizerRest.getInstance());
        capteurVocal.initialiser();
        RobotEventBus.getInstance().subscribe(capteurVocal);

    }

}
