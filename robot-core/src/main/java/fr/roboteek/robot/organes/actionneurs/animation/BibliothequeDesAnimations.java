package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Les animations enregistrées, un fichier JSON par animation dans {@code $ROBOT_HOME/animations}.
 * <p>
 * Des fichiers et non une table : une animation est un contenu écrit à la main, qu'on veut pouvoir
 * relire, corriger dans un éditeur de texte, copier d'un robot à l'autre et suivre dans un dépôt.
 * La mémoire longue SQLite garde ce que le robot a vécu, pas ce qu'on a écrit pour lui.
 * <p>
 * Ce n'est donc <b>pas</b> un {@code Repository} au sens du projet : le suffixe est réservé à
 * l'accès à une table.
 */
@Component
public class BibliothequeDesAnimations {

    private static final Logger logger = LoggerFactory.getLogger(BibliothequeDesAnimations.class);

    private static final String EXTENSION = ".json";

    /**
     * Noms acceptés. Le nom vient d'un chemin d'URL et sert à fabriquer un nom de fichier : sans
     * ce filtre, un nom contenant {@code ../} ferait lire ou écrire n'importe où sur le disque.
     * Les lettres accentuées sont admises (« Révérence ») : c'est la barre oblique, non l'accent, qui
     * fait sortir du dossier. Pour qu'un tel nom survive à l'aller-retour par le système de fichiers,
     * il est ramené à une seule écriture (voir {@link #fichierDe}) et le conteneur du robot tourne en
     * {@code C.UTF-8}, sans quoi Java ne saurait pas écrire ces noms de fichiers.
     */
    private static final String NOM_VALIDE = "[\\p{L}\\p{M}\\p{N}._ -]{1,64}";

    private final Path dossier;

    private final ObjectMapper json = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /**
     * {@code @Autowired} obligatoire : cette classe a deux constructeurs, et Spring n'en choisit
     * aucun d'office — il se rabat sur un constructeur vide, qui n'existe pas, et le contexte
     * entier échoue au démarrage (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public BibliothequeDesAnimations() {
        this(Path.of(Constantes.DOSSIER_ANIMATIONS));
    }

    /** Permet aux tests de travailler dans un dossier temporaire. */
    public BibliothequeDesAnimations(Path dossier) {
        this.dossier = dossier;
    }

    /** Noms des animations enregistrées, triés pour un affichage stable. */
    public List<String> noms() {
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        try (Stream<Path> fichiers = Files.list(dossier)) {
            return fichiers
                    .filter(fichier -> fichier.getFileName().toString().endsWith(EXTENSION))
                    .map(BibliothequeDesAnimations::nomDepuisFichier)
                    .sorted(Comparator.comparing(nom -> nom.toLowerCase(Locale.FRENCH)))
                    .toList();
        } catch (IOException e) {
            logger.error("Animations illisibles dans {}", dossier, e);
            return List.of();
        }
    }

    /**
     * Charge une animation, ou {@link Optional#empty()} si elle n'existe pas — ou si son fichier
     * est illisible. Un fichier corrompu n'empêche pas les autres animations de fonctionner ; il
     * est journalisé, pas propagé.
     */
    public Optional<Animation> charger(String nom) {
        Path fichier = fichierDe(nom);
        if (fichier == null || !Files.isRegularFile(fichier)) {
            return Optional.empty();
        }
        try {
            Animation animation = json.readValue(fichier.toFile(), Animation.class).avecNom(nom);
            if (!animation.versionLisible()) {
                // Refus bruyant plutôt que lecture de travers : avant la version 1, les pistes des
                // yeux portaient des unités moteur, et la tringlerie n'étant pas linéaire, les
                // rejouer telles quelles décalerait le geste sans que rien ne le dise.
                logger.error("Animation « {} » en version {} : écrite dans une autre unité que la {}, "
                                + "elle est ignorée plutôt que jouée de travers",
                        nom, animation.version(), Animation.VERSION_COURANTE);
                return Optional.empty();
            }
            return Optional.of(animation);
        } catch (JacksonException e) {
            logger.error("Animation « {} » illisible, fichier ignoré", nom, e);
            return Optional.empty();
        }
    }

    /**
     * Enregistre l'animation sous son propre nom, en écrasant la précédente.
     * <p>
     * Écriture dans un fichier temporaire puis remplacement atomique : une coupure au milieu d'un
     * enregistrement laisserait sinon un JSON tronqué à la place de l'animation, c'est-à-dire le
     * travail perdu au lieu d'une version en retard.
     */
    public void enregistrer(Animation animation) {
        Path fichier = fichierDe(animation.nom());
        if (fichier == null) {
            throw new IllegalArgumentException("Nom d'animation refusé : « " + animation.nom() + " »");
        }
        try {
            Files.createDirectories(dossier);
            Path temporaire = Files.createTempFile(dossier, "animation-", ".tmp");
            json.writeValue(temporaire.toFile(), animation);
            Files.move(temporaire, fichier, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Animation « " + animation.nom() + " » non enregistrée", e);
        }
    }

    /** Supprime une animation ; rend {@code false} si elle n'existait pas. */
    public boolean supprimer(String nom) {
        Path fichier = fichierDe(nom);
        if (fichier == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(fichier);
        } catch (IOException e) {
            logger.error("Animation « {} » non supprimée", nom, e);
            return false;
        }
    }

    public boolean existe(String nom) {
        Path fichier = fichierDe(nom);
        return fichier != null && Files.isRegularFile(fichier);
    }

    /** Fichier d'une animation, ou {@code null} si le nom est refusé (voir {@link #NOM_VALIDE}). */
    private Path fichierDe(String nom) {
        if (nom == null) {
            return null;
        }
        // Un « é » en un caractère ou en « e » plus l'accent : la même animation, donc le même fichier.
        String normalise = Normalizer.normalize(nom, Normalizer.Form.NFC);
        if (!normalise.matches(NOM_VALIDE)) {
            return null;
        }
        return dossier.resolve(normalise + EXTENSION);
    }

    private static String nomDepuisFichier(Path fichier) {
        String nomFichier = fichier.getFileName().toString();
        return nomFichier.substring(0, nomFichier.length() - EXTENSION.length());
    }
}
