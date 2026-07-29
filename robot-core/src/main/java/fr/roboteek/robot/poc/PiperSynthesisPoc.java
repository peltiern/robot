package fr.roboteek.robot.poc;

import java.io.File;
import java.io.IOException;

/**
 * PoC autonome (hors Spring) : valide que Piper (TTS neuronal, binaire natif externe,
 * pas de binding Java) tourne avec une latence acceptable sur le Jetson Nano 4 Go,
 * avant intégration dans {@code OrganeParole}/{@code SpeechSynthesizerService}.
 * <p>
 * Contrairement à Vosk (JNA), Piper n'a pas de binding Java : l'intégration se fait en
 * process externe ({@link ProcessBuilder}), comme le fait déjà {@code OrganeParole}
 * pour la lecture audio via SOX.
 * <p>
 * Ce PoC simule le pire cas : un process Piper relancé à chaque phrase (le modèle est
 * donc rechargé à chaque appel). À comparer avec {@link PiperPersistentPoc}, qui garde
 * le process (et le modèle) en mémoire entre les phrases.
 * <p>
 * Usage : {@code java -jar robot-core-piper-poc.jar <piper-binaire> <modele.onnx> <espeak-data-dir> <texte>}
 */
public class PiperSynthesisPoc {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 4) {
            System.err.println("Usage: PiperSynthesisPoc <piper-binaire> <modele.onnx> <espeak-data-dir> <texte>");
            System.exit(1);
        }
        String cheminPiper = args[0];
        String cheminModele = args[1];
        String cheminEspeakData = args[2];
        String texte = args[3];

        File fichierSortie = File.createTempFile("piper-poc-", ".wav");

        long tDebut = System.currentTimeMillis();
        Process process = new ProcessBuilder(
                cheminPiper,
                "-m", cheminModele,
                "-f", fichierSortie.getAbsolutePath(),
                "--espeak_data", cheminEspeakData)
                .redirectErrorStream(true)
                .start();

        process.getOutputStream().write((texte + "\n").getBytes());
        process.getOutputStream().close();

        String logs = new String(process.getInputStream().readAllBytes());
        int code = process.waitFor();
        long dureeMs = System.currentTimeMillis() - tDebut;

        System.out.println(logs);
        System.out.println("Code retour : " + code);
        System.out.println("Latence totale (process + chargement modèle + synthèse) : " + dureeMs + " ms");
        System.out.println("Fichier généré : " + fichierSortie.getAbsolutePath() + " (" + fichierSortie.length() + " octets)");
    }
}
