package fr.roboteek.robot.organes.actionneurs.voix;

import fr.roboteek.robot.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Les voix de base que Piper peut prendre : les modèles déposés sur le robot.
 * <p>
 * Relu à chaque demande, et non au démarrage : un modèle se dépose à la main sur le Jetson, et doit
 * apparaître dans l'appli sans redémarrer le robot.
 */
@Component
public class CatalogueDesModeles {

    private static final Logger logger = LoggerFactory.getLogger(CatalogueDesModeles.class);

    private static final String EXTENSION = ".onnx";

    private final Path dossier;

    /**
     * {@code @Autowired} obligatoire : deux constructeurs, et sans lui Spring n'en choisit aucun,
     * se rabat sur un constructeur vide qui n'existe pas, et tout le contexte échoue au démarrage
     * (voir {@code CablageDesBeansTest}).
     */
    @Autowired
    public CatalogueDesModeles() {
        this(Path.of(Constantes.DOSSIER_SYNTHESE_VOCALE, "piper", "models"));
    }

    /** Permet aux tests de travailler dans un dossier temporaire. */
    public CatalogueDesModeles(Path dossier) {
        this.dossier = dossier;
    }

    /**
     * Une entrée par voix : un modèle à plusieurs voix (« upmc » : jessica, pierre) en donne une par
     * locuteur. Un modèle sans sa fiche {@code .onnx.json} est écarté — Piper refuse de le charger,
     * et le proposer ferait taire le robot.
     */
    public List<ModeleVoix> modeles() {
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        List<ModeleVoix> modeles = new ArrayList<>();
        try (Stream<Path> fichiers = Files.list(dossier)) {
            fichiers.map(fichier -> fichier.getFileName().toString())
                    .filter(nom -> nom.endsWith(EXTENSION))
                    .sorted(Comparator.naturalOrder())
                    .forEach(fichier -> modeles.addAll(voixDe(fichier)));
        } catch (IOException e) {
            logger.error("Modèles de voix illisibles dans {}", dossier, e);
        }
        return modeles;
    }

    /** Une voix qui existe vraiment : c'est ce qui garde un nom venu d'une requête HTTP hors du disque. */
    public boolean contient(String fichier, Integer locuteur) {
        return modeles().stream()
                .anyMatch(m -> m.fichier().equals(fichier) && Objects.equals(m.locuteur(), locuteur));
    }

    /** Le chemin du modèle, pour Piper. */
    public String chemin(String fichier) {
        return dossier.resolve(fichier).toString();
    }

    private List<ModeleVoix> voixDe(String fichier) {
        Path fiche = dossier.resolve(fichier + ".json");
        if (!Files.isRegularFile(fiche)) {
            logger.warn("Modèle de voix {} sans sa fiche {} : écarté", fichier, fiche.getFileName());
            return List.of();
        }
        String nom = fichier.substring(0, fichier.length() - EXTENSION.length());
        try {
            JsonNode locuteurs = JsonMapper.builder().build().readTree(new File(fiche.toString())).path("speaker_id_map");
            if (!locuteurs.isObject() || locuteurs.size() <= 1) {
                return List.of(new ModeleVoix(fichier, null, nom));
            }
            // Triés par numéro, l'ordre dans lequel le modèle les a appris.
            Map<Integer, String> parNumero = new TreeMap<>();
            locuteurs.properties().forEach(entree -> parNumero.put(entree.getValue().asInt(), entree.getKey()));
            return parNumero.entrySet().stream()
                    .map(e -> new ModeleVoix(fichier, e.getKey(), nom + " · " + e.getValue()))
                    .toList();
        } catch (JacksonException e) {
            logger.warn("Fiche du modèle de voix {} illisible : écarté", fichier, e);
            return List.of();
        }
    }
}
