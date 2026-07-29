package fr.roboteek.robot.poc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * PoC : mesure la latence par phrase une fois le process Piper "chaud" (modèle déjà
 * chargé en mémoire), en gardant un seul process vivant entre plusieurs phrases —
 * l'architecture retenue pour l'intégration réelle (le process Piper serait démarré une
 * fois au boot de l'organe, pas relancé à chaque {@code lire(texte)}).
 * <p>
 * À comparer avec {@link PiperSynthesisPoc}, qui relance un process (et recharge le
 * modèle) à chaque phrase.
 * <p>
 * Usage : {@code java -jar robot-core-piper-poc.jar <piper-binaire> <modele.onnx> <espeak-data-dir> <dossier-sortie> <texte1> [texte2] [texte3] ...}
 */
public class PiperPersistentPoc {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 5) {
            System.err.println("Usage: PiperPersistentPoc <piper-binaire> <modele.onnx> <espeak-data-dir> <dossier-sortie> <texte1> [texte2] ...");
            System.exit(1);
        }
        String cheminPiper = args[0];
        String cheminModele = args[1];
        String cheminEspeakData = args[2];
        String dossierSortie = args[3];
        String[] phrases = Arrays.copyOfRange(args, 4, args.length);

        new File(dossierSortie).mkdirs();

        Process process = new ProcessBuilder(
                cheminPiper,
                "-m", cheminModele,
                "-d", dossierSortie,
                "--espeak_data", cheminEspeakData)
                .redirectErrorStream(true)
                .start();

        Writer entree = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        BufferedReader sortie = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        // Consomme les logs de chargement du modèle avant la première phrase (hors mesure).
        long tChargementDebut = System.currentTimeMillis();
        String ligne;
        while ((ligne = sortie.readLine()) != null) {
            System.out.println(ligne);
            if (ligne.contains("Initialized piper")) {
                break;
            }
        }
        System.out.println("Chargement du modèle : " + (System.currentTimeMillis() - tChargementDebut) + " ms");

        for (String phrase : phrases) {
            long tDebut = System.currentTimeMillis();
            entree.write(phrase + "\n");
            entree.flush();

            while ((ligne = sortie.readLine()) != null) {
                if (ligne.contains("Real-time factor")) {
                    break;
                }
            }
            long latenceMs = System.currentTimeMillis() - tDebut;
            System.out.println("« " + phrase + " » -> " + latenceMs + " ms (process chaud)");
        }

        entree.close();
        process.waitFor();
    }
}
