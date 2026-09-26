package fr.roboteek.robot.organes.actionneurs.son;

import fr.roboteek.robot.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Les sons créés dans le Studio, deux fichiers par son dans {@code $ROBOT_HOME/sons} : la
 * <b>recette</b> ({@code .json}) et le son lui-même ({@code .wav}).
 * <p>
 * <b>Le robot ne fabrique pas les sons, il les joue.</b> La synthèse vit dans le navigateur, pour
 * que le Studio fonctionne robot éteint comme l'éditeur d'animation ; le navigateur envoie donc ce
 * qu'il a fait entendre, et le robot rejoue ce fichier par {@code play}, le seul chemin audio
 * éprouvé sur le Jetson.
 * <p>
 * La recette n'est <b>pas relue</b> par le robot, seulement rangée et rendue telle quelle à
 * l'éditeur : la décrire en Java obligerait à la faire évoluer des deux côtés à chaque nouveau
 * réglage du Studio, sans que le robot en tire quoi que ce soit.
 * <p>
 * Ce n'est pas un {@code Repository} : le suffixe est réservé à l'accès à une table.
 */
@Component
public class BibliothequeDesSons {

    private static final Logger logger = LoggerFactory.getLogger(BibliothequeDesSons.class);

    private static final String RECETTE = ".json";

    private static final String AUDIO = ".wav";

    /**
     * Noms acceptés : le nom vient d'un chemin d'URL et sert à fabriquer deux noms de fichiers ; sans
     * ce filtre, un {@code ../} ferait écrire n'importe où. Les lettres accentuées y sont, parce que
     * le Studio nomme lui-même ses sons « humeur colère » ou « arpège » — refusés, ils restaient pour
     * toujours dans la file d'attente du navigateur. Pas de barre oblique pour autant : c'est elle, et
     * non l'accent, qui fait sortir du dossier. Le conteneur du robot tourne en {@code C.UTF-8}, sans
     * quoi Java ne saurait pas écrire ces noms de fichiers.
     */
    private static final String NOM_VALIDE = "[\\p{L}\\p{M}\\p{N}._ -]{1,64}";

    private final Path dossier;

    private final ObjectMapper json = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /**
     * {@code @Autowired} obligatoire : deux constructeurs, et sans lui Spring n'en choisit aucun,
     * se rabat sur un constructeur vide qui n'existe pas, et tout le contexte échoue au démarrage
     * (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public BibliothequeDesSons() {
        this(Path.of(Constantes.DOSSIER_SONS_CREES));
    }

    /** Permet aux tests de travailler dans un dossier temporaire. */
    public BibliothequeDesSons(Path dossier) {
        this.dossier = dossier;
    }

    /**
     * Noms des sons enregistrés, triés pour un affichage stable.
     * <p>
     * Ce sont les recettes qui font foi, et non les {@code .wav} : l'enregistrement écrit l'audio
     * d'abord, si bien qu'un son listé a toujours de quoi être joué. L'inverse annoncerait des sons
     * muets le temps d'une coupure de courant.
     */
    public List<String> noms() {
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        try (Stream<Path> fichiers = Files.list(dossier)) {
            return fichiers
                    .map(fichier -> fichier.getFileName().toString())
                    .filter(nomFichier -> nomFichier.endsWith(RECETTE))
                    .map(nomFichier -> nomFichier.substring(0, nomFichier.length() - RECETTE.length()))
                    .sorted(Comparator.comparing(nom -> nom.toLowerCase(Locale.FRENCH)))
                    .toList();
        } catch (IOException e) {
            logger.error("Sons illisibles dans {}", dossier, e);
            return List.of();
        }
    }

    /** La recette d'un son, telle que le Studio l'a écrite, ou vide si elle manque ou est illisible. */
    public Optional<JsonNode> recette(String nom) {
        Path fichier = fichierDe(nom, RECETTE);
        if (fichier == null || !Files.isRegularFile(fichier)) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.readTree(fichier.toFile()));
        } catch (JacksonException e) {
            logger.error("Recette du son « {} » illisible, fichier ignoré", nom, e);
            return Optional.empty();
        }
    }

    /** Le fichier audio d'un son, pour le servir ou le donner à {@code play}. */
    public Optional<Path> audio(String nom) {
        Path fichier = fichierDe(nom, AUDIO);
        return fichier != null && Files.isRegularFile(fichier) ? Optional.of(fichier) : Optional.empty();
    }

    /**
     * Enregistre un son, en écrasant le précédent du même nom.
     * <p>
     * L'audio est écrit avant la recette, et chacun par fichier temporaire puis remplacement
     * atomique : une coupure en plein enregistrement laisse la version précédente entière, jamais
     * un fichier tronqué — c'est-à-dire une version en retard plutôt que le travail perdu.
     *
     * @throws IllegalArgumentException si le nom est refusé, si la recette ne dit pas sa version,
     *                                  ou si l'audio n'est pas un WAV
     */
    public void enregistrer(String nom, JsonNode recette, byte[] wav) {
        Path fichierRecette = fichierDe(nom, RECETTE);
        Path fichierAudio = fichierDe(nom, AUDIO);
        if (fichierRecette == null) {
            throw new IllegalArgumentException("Nom de son refusé : « " + nom + " »");
        }
        if (recette == null || !recette.has("version")) {
            // Sans version écrite dans le fichier, un son d'aujourd'hui serait indistinguable d'un
            // son d'un Studio à venir. C'est la leçon du format d'animation, payée une fois.
            throw new IllegalArgumentException("Son « " + nom + " » sans version dans sa recette");
        }
        if (!estUnWav(wav)) {
            // Le robot ne relit pas la recette, donc le WAV est tout ce qu'il a. Accepter n'importe
            // quels octets, c'est découvrir le problème quand `play` échouera, loin d'ici.
            throw new IllegalArgumentException("Son « " + nom + " » : l'audio n'est pas un WAV");
        }
        try {
            Files.createDirectories(dossier);
            ecrire(fichierAudio, temporaire -> Files.write(temporaire, wav));
            ecrire(fichierRecette, temporaire -> json.writeValue(temporaire.toFile(), recette));
        } catch (IOException e) {
            throw new UncheckedIOException("Son « " + nom + " » non enregistré", e);
        }
    }

    /** Supprime un son, recette et audio ; rend {@code false} si rien n'existait. */
    public boolean supprimer(String nom) {
        Path fichierRecette = fichierDe(nom, RECETTE);
        Path fichierAudio = fichierDe(nom, AUDIO);
        if (fichierRecette == null) {
            return false;
        }
        try {
            boolean recetteSupprimee = Files.deleteIfExists(fichierRecette);
            boolean audioSupprime = Files.deleteIfExists(fichierAudio);
            return recetteSupprimee || audioSupprime;
        } catch (IOException e) {
            logger.error("Son « {} » non supprimé", nom, e);
            return false;
        }
    }

    public boolean existe(String nom) {
        Path fichier = fichierDe(nom, RECETTE);
        return fichier != null && Files.isRegularFile(fichier);
    }

    /** Un WAV commence par {@code RIFF....WAVE} ; en deçà de l'en-tête, il n'y a rien à jouer. */
    private static boolean estUnWav(byte[] octets) {
        if (octets == null || octets.length < 44) {
            return false;
        }
        String entete = new String(octets, 0, 12, StandardCharsets.US_ASCII);
        return entete.startsWith("RIFF") && entete.endsWith("WAVE");
    }

    private void ecrire(Path destination, EcritureFichier ecriture) throws IOException {
        Path temporaire = Files.createTempFile(dossier, "son-", ".tmp");
        try {
            ecriture.dans(temporaire);
            Files.move(temporaire, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaire);
        }
    }

    /** Fichier d'un son, ou {@code null} si le nom est refusé (voir {@link #NOM_VALIDE}). */
    private Path fichierDe(String nom, String extension) {
        if (nom == null) {
            return null;
        }
        // Un « é » en un caractère ou en « e » plus l'accent : le même son, donc le même fichier.
        String normalise = Normalizer.normalize(nom, Normalizer.Form.NFC);
        if (!normalise.matches(NOM_VALIDE)) {
            return null;
        }
        return dossier.resolve(normalise + extension);
    }

    @FunctionalInterface
    private interface EcritureFichier {
        void dans(Path temporaire) throws IOException;
    }
}
