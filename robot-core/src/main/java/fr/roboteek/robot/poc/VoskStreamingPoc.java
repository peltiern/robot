package fr.roboteek.robot.poc;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * PoC : valide que nourrir Vosk au fil de la capture (comme le ferait
 * {@code AbstractCapteurVocal} en direct, {@code acceptWaveForm} appelé bloc par
 * bloc) réduit la latence perçue après la fin de la phrase, par rapport au
 * traitement par lot de {@link VoskRecognitionPoc} (fichier entier d'un coup).
 * <p>
 * Simule le rythme réel d'arrivée d'un micro : chaque bloc de 100 ms n'est fourni
 * à Vosk qu'au moment où il aurait réellement été capturé (le code dort le temps
 * restant du bloc s'il est en avance). Si le calcul prend plus longtemps que le
 * temps réel, aucune pause n'est faite : le retard s'accumule naturellement,
 * exactement comme une capture micro qui continuerait en tâche de fond.
 * <p>
 * Usage : {@code java -jar robot-core-poc.jar <modele-vosk-dir> <fichier.wav>}
 */
public class VoskStreamingPoc {

    private static final float FREQUENCE_ECHANTILLONNAGE_VOSK = 16000f;
    private static final long DUREE_BLOC_MS = 100;
    private static final int TAILLE_BLOC_OCTETS = (int) (FREQUENCE_ECHANTILLONNAGE_VOSK * DUREE_BLOC_MS / 1000) * 2;

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        if (args.length < 2) {
            System.err.println("Usage: VoskStreamingPoc <modele-vosk-dir> <fichier.wav>");
            System.exit(1);
        }
        String cheminModele = args[0];
        String cheminWav = args[1];

        LibVosk.setLogLevel(LogLevel.WARNINGS);

        long tChargementDebut = System.currentTimeMillis();
        Model model = new Model(cheminModele);
        Recognizer recognizer = new Recognizer(model, FREQUENCE_ECHANTILLONNAGE_VOSK);
        System.out.println("Chargement du modèle : " + (System.currentTimeMillis() - tChargementDebut) + " ms");

        AudioInputStream fluxOriginal = AudioSystem.getAudioInputStream(new File(cheminWav));
        AudioFormat formatCible = new AudioFormat(FREQUENCE_ECHANTILLONNAGE_VOSK, 16, 1, true, false);
        AudioInputStream fluxConverti = AudioSystem.getAudioInputStream(formatCible, fluxOriginal);

        byte[] tampon = new byte[TAILLE_BLOC_OCTETS];
        int lus;
        long tDebutSimulation = System.currentTimeMillis();
        long dureeAudioSimuleeMs = 0;
        int nbBlocs = 0;

        while ((lus = fluxConverti.read(tampon)) >= 0) {
            recognizer.acceptWaveForm(tampon, lus);
            nbBlocs++;
            dureeAudioSimuleeMs += DUREE_BLOC_MS;

            // Simule l'arrivée temps réel du micro : on ne dort que s'il reste du
            // temps sur ce bloc (sinon on est déjà en retard, on enchaîne aussitôt).
            long aDormir = dureeAudioSimuleeMs - (System.currentTimeMillis() - tDebutSimulation);
            if (aDormir > 0) {
                try {
                    Thread.sleep(aDormir);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long tFinParole = System.currentTimeMillis();
        long retardAccumuleMs = (tFinParole - tDebutSimulation) - dureeAudioSimuleeMs;
        System.out.println("Fin de la phrase simulée (" + nbBlocs + " blocs, " + dureeAudioSimuleeMs + " ms d'audio réel) : "
                + (retardAccumuleMs > 0 ? "retard accumulé de " + retardAccumuleMs + " ms pendant la parole" : "aucun retard, Vosk suit le temps réel"));

        long tResultatDebut = System.currentTimeMillis();
        String resultat = recognizer.getFinalResult();
        long tLatencePercueMs = System.currentTimeMillis() - tFinParole;

        System.out.println("Latence perçue après la fin de la phrase (rattrapage + résultat final) : " + tLatencePercueMs + " ms");
        System.out.println("Résultat : " + resultat);

        fluxConverti.close();
        fluxOriginal.close();
        recognizer.close();
        model.close();
    }
}
