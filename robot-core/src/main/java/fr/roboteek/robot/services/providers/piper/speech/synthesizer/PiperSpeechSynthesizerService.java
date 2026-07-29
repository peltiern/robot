package fr.roboteek.robot.services.providers.piper.speech.synthesizer;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.speech.synthesis.piper.PiperSpeechSynthesisConfig;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Synthèse vocale locale avec Piper : pas de binding Java, le binaire natif est piloté en
 * process externe (comme SOX pour la lecture, cf. {@link fr.roboteek.robot.organes.actionneurs.OrganeParole}).
 * <p>
 * Le process est démarré une seule fois et gardé vivant (modèle chargé une seule fois) : un
 * process relancé à chaque phrase coûte ~1s de rechargement de modèle à chaque appel (mesuré
 * sur le Jetson Nano via {@code fr.roboteek.robot.poc.PiperSynthesisPoc}), contre ~600-700ms
 * par phrase une fois chaud (mesuré via {@code fr.roboteek.robot.poc.PiperPersistentPoc}).
 * <p>
 * Protocole : une ligne de texte écrite sur l'entrée standard du process déclenche une
 * synthèse ; Piper (mode {@code --output_dir}) écrit le WAV dans un fichier et affiche son
 * chemin sur la sortie standard, suivi d'une ligne de log "Real-time factor" signalant la fin.
 */
public class PiperSpeechSynthesizerService implements SpeechSynthesizerService {

    private static final Logger logger = LoggerFactory.getLogger(PiperSpeechSynthesizerService.class);

    private static PiperSpeechSynthesizerService instance;

    private final Process process;
    private final Writer entree;
    private final BufferedReader sortie;

    private PiperSpeechSynthesizerService() {
        PiperSpeechSynthesisConfig config = Configurations.piperSpeechSynthesisConfig();
        String dossierPiper = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "piper";
        String binaryPath = dossierPiper + File.separator + "piper-bin" + File.separator + "piper";
        String modelPath = dossierPiper + File.separator + "models" + File.separator + config.voiceModelFileName();
        String espeakDataPath = dossierPiper + File.separator + "piper-bin" + File.separator + "espeak-ng-data";
        File dossierSortie = new File(dossierPiper + File.separator + "out");
        dossierSortie.mkdirs();

        try {
            process = new ProcessBuilder(
                    binaryPath,
                    "-m", modelPath,
                    "-d", dossierSortie.getAbsolutePath(),
                    "--espeak_data", espeakDataPath)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de démarrer le process Piper : " + binaryPath, e);
        }

        entree = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        sortie = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        attendreChargementModele();

        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy, "piper-shutdown"));
        logger.info("Process Piper démarré et modèle chargé : {}", modelPath);
    }

    public static synchronized PiperSpeechSynthesizerService getInstance() {
        if (instance == null) {
            instance = new PiperSpeechSynthesizerService();
        }
        return instance;
    }

    private void attendreChargementModele() {
        try {
            String ligne;
            while ((ligne = sortie.readLine()) != null) {
                if (ligne.contains("Initialized piper")) {
                    return;
                }
            }
            logger.warn("Process Piper terminé avant confirmation de chargement du modèle");
        } catch (IOException e) {
            logger.error("Erreur en attendant le chargement du modèle Piper", e);
        }
    }

    @Override
    public synchronized byte[] synthesize(String texte) {
        String texteUneLigne = texte.replace("\n", " ").replace("\r", " ");
        try {
            entree.write(texteUneLigne + "\n");
            entree.flush();

            String cheminFichier = null;
            String ligne;
            while ((ligne = sortie.readLine()) != null) {
                if (ligne.endsWith(".wav")) {
                    cheminFichier = ligne.trim();
                }
                if (ligne.contains("Real-time factor")) {
                    break;
                }
            }

            if (cheminFichier == null) {
                logger.error("Aucun fichier généré par Piper pour le texte : {}", texte);
                return null;
            }

            Path fichier = Path.of(cheminFichier);
            byte[] contenu = Files.readAllBytes(fichier);
            Files.deleteIfExists(fichier);
            return contenu;
        } catch (IOException e) {
            logger.error("Échec de la synthèse vocale Piper pour le texte : {}", texte, e);
            return null;
        }
    }
}
