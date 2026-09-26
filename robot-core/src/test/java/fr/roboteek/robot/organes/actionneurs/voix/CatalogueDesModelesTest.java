package fr.roboteek.robot.organes.actionneurs.voix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Les modèles de voix déposés sur le robot, tels que l'appli les propose. */
class CatalogueDesModelesTest {

    @TempDir
    Path dossier;

    private void deposer(String nom, String fiche) throws IOException {
        Files.writeString(dossier.resolve(nom + ".onnx"), "modèle");
        if (fiche != null) {
            Files.writeString(dossier.resolve(nom + ".onnx.json"), fiche);
        }
    }

    @Test
    void unModeleAUneVoixDonneUneEntree() throws IOException {
        deposer("fr_FR-tom-medium", "{\"num_speakers\":1,\"speaker_id_map\":{}}");

        assertEquals(List.of(new ModeleVoix("fr_FR-tom-medium.onnx", null, "fr_FR-tom-medium")),
                new CatalogueDesModeles(dossier).modeles());
    }

    /** « upmc » a deux voix, jessica et pierre : chacune se choisit à part. */
    @Test
    void unModeleAPlusieursVoixDonneUneEntreeParVoix() throws IOException {
        deposer("fr_FR-upmc-medium", "{\"num_speakers\":2,\"speaker_id_map\":{\"pierre\":1,\"jessica\":0}}");

        assertEquals(List.of(
                        new ModeleVoix("fr_FR-upmc-medium.onnx", 0, "fr_FR-upmc-medium · jessica"),
                        new ModeleVoix("fr_FR-upmc-medium.onnx", 1, "fr_FR-upmc-medium · pierre")),
                new CatalogueDesModeles(dossier).modeles());
    }

    /** Piper refuse un modèle sans sa fiche : le proposer ferait taire le robot. */
    @Test
    void unModeleSansSaFicheEstEcarte() throws IOException {
        deposer("fr_FR-gilles-low", null);
        deposer("fr_FR-siwis-medium", "{}");

        assertEquals(List.of(new ModeleVoix("fr_FR-siwis-medium.onnx", null, "fr_FR-siwis-medium")),
                new CatalogueDesModeles(dossier).modeles());
    }

    /** Ce qui vient d'une requête HTTP n'est un modèle que s'il est dans la liste. */
    @Test
    void seulUnModeleDeposeEstReconnu() throws IOException {
        deposer("fr_FR-upmc-medium", "{\"speaker_id_map\":{\"jessica\":0,\"pierre\":1}}");
        CatalogueDesModeles catalogue = new CatalogueDesModeles(dossier);

        assertTrue(catalogue.contient("fr_FR-upmc-medium.onnx", 1));
        assertFalse(catalogue.contient("fr_FR-upmc-medium.onnx", null));
        assertFalse(catalogue.contient("fr_FR-upmc-medium.onnx", 7));
        assertFalse(catalogue.contient("../../etc/passwd", null));
    }

    @Test
    void unDossierAbsentNeProposeRien() {
        assertEquals(List.of(), new CatalogueDesModeles(dossier.resolve("absent")).modeles());
    }
}
