package fr.roboteek.robot.organes.actionneurs.animation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Persistance des animations en JSON sur le système de fichiers.
 * Chaque animation est stockée dans un fichier {nom}.json dans le dossier des animations.
 */
public class AnimationRepository {

    private static final Logger logger = LoggerFactory.getLogger(AnimationRepository.class);
    private static final String EXTENSION = ".json";

    private final File directory;
    private final ObjectMapper objectMapper;

    public AnimationRepository(String directoryPath) {
        this.directory = new File(directoryPath);
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        ensureDirectoryExists();
    }

    /**
     * Sauvegarde une animation. Le fichier est nommé d'après {@code animation.getName()}.
     *
     * @throws IllegalArgumentException si l'animation n'a pas de nom
     */
    public void save(Animation animation) {
        if (animation.getName() == null || animation.getName().isBlank()) {
            throw new IllegalArgumentException("L'animation doit avoir un nom pour être sauvegardée");
        }
        File file = fileFor(animation.getName());
        try {
            objectMapper.writeValue(file, animation);
            logger.info("Animation '{}' sauvegardée dans {}", animation.getName(), file.getPath());
        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde de l'animation '{}'", animation.getName(), e);
            throw new RuntimeException("Impossible de sauvegarder l'animation : " + animation.getName(), e);
        }
    }

    /**
     * Charge une animation par son nom. Retourne {@code Optional.empty()} si le fichier n'existe pas.
     */
    public Optional<Animation> load(String name) {
        File file = fileFor(name);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            Animation animation = objectMapper.readValue(file, Animation.class);
            logger.debug("Animation '{}' chargée depuis {}", name, file.getPath());
            return Optional.of(animation);
        } catch (IOException e) {
            logger.error("Erreur lors du chargement de l'animation '{}'", name, e);
            return Optional.empty();
        }
    }

    /**
     * Charge toutes les animations du dossier.
     */
    public List<Animation> loadAll() {
        List<Animation> animations = new ArrayList<>();
        File[] files = directory.listFiles((d, n) -> n.endsWith(EXTENSION));
        if (files == null) return animations;

        Arrays.sort(files);
        for (File file : files) {
            try {
                Animation animation = objectMapper.readValue(file, Animation.class);
                animations.add(animation);
            } catch (IOException e) {
                logger.error("Erreur lors du chargement du fichier {}", file.getName(), e);
            }
        }
        logger.debug("{} animation(s) chargée(s) depuis {}", animations.size(), directory.getPath());
        return animations;
    }

    /**
     * Supprime l'animation portant ce nom. Retourne true si supprimée, false si introuvable.
     */
    public boolean delete(String name) {
        File file = fileFor(name);
        if (!file.exists()) {
            return false;
        }
        boolean deleted = file.delete();
        if (deleted) {
            logger.info("Animation '{}' supprimée", name);
        } else {
            logger.error("Impossible de supprimer le fichier {}", file.getPath());
        }
        return deleted;
    }

    /**
     * Retourne la liste des noms d'animations disponibles sur disque.
     */
    public List<String> listNames() {
        File[] files = directory.listFiles((d, n) -> n.endsWith(EXTENSION));
        if (files == null) return List.of();
        return Arrays.stream(files)
                .map(f -> f.getName().replace(EXTENSION, ""))
                .sorted()
                .toList();
    }

    public boolean exists(String name) {
        return fileFor(name).exists();
    }

    // ── Privé ─────────────────────────────────────────────────────────────────

    private File fileFor(String name) {
        return new File(directory, name + EXTENSION);
    }

    private void ensureDirectoryExists() {
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.info("Dossier des animations créé : {}", directory.getPath());
            } else {
                logger.error("Impossible de créer le dossier des animations : {}", directory.getPath());
            }
        }
    }
}
