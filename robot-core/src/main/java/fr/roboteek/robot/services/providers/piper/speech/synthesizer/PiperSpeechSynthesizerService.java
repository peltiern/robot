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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

    private final String dossierPiper = Constantes.DOSSIER_SYNTHESE_VOCALE + File.separator + "piper";

    private Process process;
    private Writer entree;
    private BufferedReader sortie;

    /** Le modèle chargé, et sa voix pour un modèle qui en a plusieurs : ce qui dit s'il faut relancer. */
    private String modele;
    private Integer locuteur;

    private PiperSpeechSynthesizerService(String modele, Integer locuteur) {
        demarrer(modele, locuteur);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> arreterProcess(), "piper-shutdown"));
    }

    /**
     * @param modele   le fichier du modèle sous {@code piper/models/}, {@code null} pour celui de
     *                 {@code robot.properties}
     * @param locuteur la voix d'un modèle qui en a plusieurs, {@code null} sinon
     */
    public static synchronized PiperSpeechSynthesizerService getInstance(String modele, Integer locuteur) {
        if (instance == null) {
            instance = new PiperSpeechSynthesizerService(modele, locuteur);
        }
        return instance;
    }

    /**
     * Passe à une autre voix de base. Rien à faire si c'est déjà elle : c'est le cas de presque
     * toutes les phrases. Sinon Piper est relancé — environ une seconde de chargement —, l'ancien
     * arrêté d'abord : deux modèles chargés ensemble pèseraient sur la mémoire du Jetson Nano.
     * <p>
     * Si le nouveau modèle ne démarre pas, l'ancien est relancé : mieux vaut une voix qu'on n'a pas
     * choisie qu'un robot muet.
     *
     * @return vrai si la voix demandée est celle qui parlera
     */
    public synchronized boolean utiliser(String modele, Integer locuteur) {
        String voulu = modele == null ? modeleParDefaut() : modele;
        if (process != null && process.isAlive() && voulu.equals(this.modele) && Objects.equals(locuteur, this.locuteur)) {
            return true;
        }
        String ancienModele = this.modele;
        Integer ancienLocuteur = this.locuteur;
        arreterProcess();
        try {
            demarrer(voulu, locuteur);
            return true;
        } catch (IllegalStateException e) {
            logger.error("Voix {} (locuteur {}) impossible à charger, retour à {}", voulu, locuteur, ancienModele, e);
            arreterProcess();
            demarrer(ancienModele, ancienLocuteur);
            return false;
        }
    }

    private static String modeleParDefaut() {
        PiperSpeechSynthesisConfig config = Configurations.piperSpeechSynthesisConfig();
        return config.voiceModelFileName();
    }

    private void demarrer(String modeleVoulu, Integer locuteurVoulu) {
        String fichierModele = modeleVoulu == null ? modeleParDefaut() : modeleVoulu;
        String binaryPath = dossierPiper + File.separator + "piper-bin" + File.separator + "piper";
        String modelPath = dossierPiper + File.separator + "models" + File.separator + fichierModele;
        String espeakDataPath = dossierPiper + File.separator + "piper-bin" + File.separator + "espeak-ng-data";
        File dossierSortie = new File(dossierPiper + File.separator + "out");
        dossierSortie.mkdirs();

        List<String> commande = new ArrayList<>(List.of(
                binaryPath,
                "-m", modelPath,
                "-d", dossierSortie.getAbsolutePath(),
                "--espeak_data", espeakDataPath));
        if (locuteurVoulu != null) {
            commande.addAll(List.of("--speaker", String.valueOf(locuteurVoulu)));
        }
        try {
            process = new ProcessBuilder(commande)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de démarrer le process Piper : " + binaryPath, e);
        }

        entree = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        sortie = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        if (!attendreChargementModele()) {
            process.destroyForcibly();
            throw new IllegalStateException("Piper n'a pas chargé le modèle " + modelPath);
        }
        modele = fichierModele;
        locuteur = locuteurVoulu;
        logger.info("Process Piper démarré et modèle chargé : {}{}", modelPath, locuteurVoulu == null ? "" : " (locuteur " + locuteurVoulu + ")");
    }

    private void arreterProcess() {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            // Attendre la fin : relancer avant que l'ancien ait rendu sa mémoire, c'est deux modèles
            // chargés ensemble.
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        process = null;
    }

    /** @return vrai si Piper a annoncé son modèle chargé ; faux s'il s'est arrêté avant. */
    private boolean attendreChargementModele() {
        try {
            String ligne;
            while ((ligne = sortie.readLine()) != null) {
                if (ligne.contains("Initialized piper")) {
                    return true;
                }
            }
            logger.warn("Process Piper terminé avant confirmation de chargement du modèle");
        } catch (IOException e) {
            logger.error("Erreur en attendant le chargement du modèle Piper", e);
        }
        return false;
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
