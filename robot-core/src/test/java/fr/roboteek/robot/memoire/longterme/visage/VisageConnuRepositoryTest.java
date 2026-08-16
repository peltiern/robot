package fr.roboteek.robot.memoire.visage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la persistance MapDB des visages connus, indépendamment d'OpenCV/des
 * modèles ONNX (pure logique de stockage).
 */
class VisageConnuRepositoryTest {

    @TempDir
    File dossierTemp;

    private VisageConnuRepository repository;

    @BeforeEach
    void setUp() {
        repository = new VisageConnuRepository(new File(dossierTemp, "visages.db").getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void uneBaseVideNeContientAucunVisage() {
        assertTrue(repository.tousLesVisages().isEmpty());
    }

    @Test
    void ajouterPuisRelireUnVisage() {
        repository.ajouter("id-amy", new float[]{1f, 2f, 3f});

        List<VisageConnu> visages = repository.tousLesVisages();

        assertEquals(1, visages.size());
        assertEquals("id-amy", visages.get(0).idPersonne());
        assertArrayEquals(new float[]{1f, 2f, 3f}, visages.get(0).embedding());
    }

    @Test
    void plusieursEntreesPourLaMemePersonneSontToutesConservees() {
        repository.ajouter("id-amy", new float[]{1f, 2f});
        repository.ajouter("id-amy", new float[]{3f, 4f});

        List<VisageConnu> visages = repository.tousLesVisages();

        assertEquals(2, visages.size());
        assertTrue(visages.stream().allMatch(v -> v.idPersonne().equals("id-amy")));
    }

    @Test
    void lesVisagesPersistentApresReouvertureDeLaBase() {
        repository.ajouter("id-einstein", new float[]{5f, 6f});
        repository.close();

        VisageConnuRepository reouverte = new VisageConnuRepository(new File(dossierTemp, "visages.db").getAbsolutePath());
        List<VisageConnu> visages = reouverte.tousLesVisages();

        assertEquals(1, visages.size());
        assertEquals("id-einstein", visages.get(0).idPersonne());
        reouverte.close();

        // Évite un double close() dans @AfterEach : la base a déjà été fermée puis rouverte.
        repository = new VisageConnuRepository(new File(dossierTemp, "visages-vide.db").getAbsolutePath());
    }
}
