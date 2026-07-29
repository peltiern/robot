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
 * PoC autonome (hors Spring) : valide que Vosk tourne en CPU pur sur le Jetson
 * Nano 4 Go (latence + RAM), avant toute intégration dans {@code CapteurVocalAvecReconnaissance}.
 * <p>
 * Usage : {@code java -jar robot-core-poc.jar <modele-vosk-dir> <fichier.wav>}
 * <p>
 * Le WAV d'entrée est rééchantillonné en interne (mono, 16 kHz, 16 bits) via l'API
 * Java Sound, quel que soit son format d'origine — le pipeline actuel capture le
 * micro en 44100 Hz alors que Vosk attend 16000 Hz.
 */
public class VoskRecognitionPoc {

    private static final float FREQUENCE_ECHANTILLONNAGE_VOSK = 16000f;

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        if (args.length < 2) {
            System.err.println("Usage: VoskRecognitionPoc <modele-vosk-dir> <fichier.wav>");
            System.exit(1);
        }
        String cheminModele = args[0];
        String cheminWav = args[1];

        // Réduit le bruit des logs natifs Vosk/Kaldi sur la sortie standard.
        LibVosk.setLogLevel(LogLevel.WARNINGS);

        long tChargementDebut = System.currentTimeMillis();
        Model model = new Model(cheminModele);
        Recognizer recognizer = new Recognizer(model, FREQUENCE_ECHANTILLONNAGE_VOSK);
        System.out.println("Chargement du modèle : " + (System.currentTimeMillis() - tChargementDebut) + " ms");

        AudioInputStream fluxOriginal = AudioSystem.getAudioInputStream(new File(cheminWav));
        AudioFormat formatCible = new AudioFormat(FREQUENCE_ECHANTILLONNAGE_VOSK, 16, 1, true, false);
        AudioInputStream fluxConverti = AudioSystem.getAudioInputStream(formatCible, fluxOriginal);

        long tReconnaissanceDebut = System.currentTimeMillis();
        byte[] tampon = new byte[4096];
        int lus;
        while ((lus = fluxConverti.read(tampon)) >= 0) {
            recognizer.acceptWaveForm(tampon, lus);
        }
        String resultat = recognizer.getFinalResult();
        long reconnaissanceMs = System.currentTimeMillis() - tReconnaissanceDebut;

        System.out.println("Reconnaissance (" + reconnaissanceMs + " ms) : " + resultat);

        fluxConverti.close();
        fluxOriginal.close();
        recognizer.close();
        model.close();
    }
}
