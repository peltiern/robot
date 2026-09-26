package fr.roboteek.robot.organes.actionneurs.voix;

import fr.roboteek.robot.Constantes;
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
import java.nio.file.StandardCopyOption;

/**
 * La voix adoptée : les réglages avec lesquels le robot parle, gardés d'un démarrage à l'autre.
 * <p>
 * Dans un fichier à part, et non dans {@code robot.properties} : celui-ci n'est lu qu'au démarrage,
 * et c'est justement ce qui obligeait à redémarrer le robot pour chaque retouche. Ici, une voix
 * adoptée vaut dès la phrase suivante.
 */
@Component
public class VoixDuRobot {

    private static final Logger logger = LoggerFactory.getLogger(VoixDuRobot.class);

    private final Path fichier;

    private final ObjectMapper json = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private volatile ReglagesVoix reglages;

    /**
     * {@code @Autowired} obligatoire : deux constructeurs, et sans lui Spring n'en choisit aucun,
     * se rabat sur un constructeur vide qui n'existe pas, et tout le contexte échoue au démarrage
     * (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public VoixDuRobot() {
        this(Path.of(Constantes.FICHIER_VOIX));
    }

    /** Permet aux tests de travailler dans un dossier temporaire. */
    public VoixDuRobot(Path fichier) {
        this.fichier = fichier;
        this.reglages = lire();
    }

    /** Les réglages avec lesquels le robot parle. */
    public ReglagesVoix reglages() {
        return reglages;
    }

    /** Adopte une voix : elle vaut dès la phrase suivante, et au prochain démarrage. */
    public ReglagesVoix adopter(ReglagesVoix nouveaux) {
        ReglagesVoix bornes = nouveaux.bornes();
        try {
            Files.createDirectories(fichier.getParent());
            Path temporaire = Files.createTempFile(fichier.getParent(), "voix-", ".tmp");
            try {
                json.writeValue(temporaire.toFile(), bornes);
                Files.move(temporaire, fichier, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporaire);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Voix non enregistrée", e);
        }
        reglages = bornes;
        logger.info("Voix adoptée : {}", bornes);
        return bornes;
    }

    /**
     * Sans fichier, ou avec un fichier illisible, le robot parle avec la voix d'origine : mieux vaut
     * une voix qu'on n'a pas choisie qu'un robot muet.
     */
    private ReglagesVoix lire() {
        if (!Files.isRegularFile(fichier)) {
            return ReglagesVoix.ORIGINE;
        }
        try {
            return json.readValue(fichier.toFile(), ReglagesVoix.class).bornes();
        } catch (JacksonException e) {
            logger.error("Réglages de voix illisibles dans {}, voix d'origine", fichier, e);
            return ReglagesVoix.ORIGINE;
        }
    }
}
