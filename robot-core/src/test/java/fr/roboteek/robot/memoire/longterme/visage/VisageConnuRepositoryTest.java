package fr.roboteek.robot.memoire.longterme.visage;

import fr.roboteek.robot.memoire.longterme.BaseMemoireDeTest;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la persistance des visages connus, indépendamment d'OpenCV et des modèles ONNX
 * (pure logique de stockage).
 */
class VisageConnuRepositoryTest {

    @TempDir
    File dossierTemp;

    private DataSource memoire;

    private VisageConnuRepository repository;

    private PersonneRepository personneRepository;

    private Personne amy;

    private Personne einstein;

    @BeforeEach
    void setUp() {
        memoire = BaseMemoireDeTest.dans(dossierTemp);
        repository = new VisageConnuRepository(memoire);
        personneRepository = new PersonneRepository(memoire);

        amy = Personne.nouvelle("Amy");
        einstein = Personne.nouvelle("Einstein");
        personneRepository.enregistrer(amy);
        personneRepository.enregistrer(einstein);
    }

    @Test
    void uneBaseVideNeContientAucunVisage() {
        assertTrue(repository.tousLesVisages().isEmpty());
    }

    @Test
    void ajouterPuisRelireUnVisage() {
        repository.ajouter(amy.id(), new float[]{1f, 2f, 3f});

        List<VisageConnu> visages = repository.tousLesVisages();

        assertEquals(1, visages.size());
        assertEquals(amy.id(), visages.get(0).idPersonne());
        assertArrayEquals(new float[]{1f, 2f, 3f}, visages.get(0).embedding());
    }

    @Test
    void plusieursEntreesPourLaMemePersonneSontToutesConservees() {
        repository.ajouter(amy.id(), new float[]{1f, 2f});
        repository.ajouter(amy.id(), new float[]{3f, 4f});

        List<VisageConnu> visages = repository.tousLesVisages();

        assertEquals(2, visages.size());
        assertTrue(visages.stream().allMatch(visage -> visage.idPersonne().equals(amy.id())));
    }

    @Test
    void lesVisagesSeRetrouventParPersonne() {
        repository.ajouter(amy.id(), new float[]{1f, 2f});
        repository.ajouter(amy.id(), new float[]{3f, 4f});
        repository.ajouter(einstein.id(), new float[]{5f, 6f});

        assertEquals(2, repository.parPersonne(amy.id()).size());
        assertEquals(1, repository.parPersonne(einstein.id()).size());
        assertEquals(Map.of(amy.id(), 2, einstein.id(), 1), repository.nombreParPersonne());
    }

    @Test
    void oublierLesVisagesDunePersonneLaisseCeuxDesAutres() {
        repository.ajouter(amy.id(), new float[]{1f, 2f});
        repository.ajouter(einstein.id(), new float[]{5f, 6f});

        assertEquals(1, repository.supprimerParPersonne(amy.id()));

        assertTrue(repository.parPersonne(amy.id()).isEmpty());
        assertEquals(1, repository.parPersonne(einstein.id()).size());
    }

    /**
     * Le cœur de l'affaire : une empreinte qui survit à sa personne ferait « reconnaître » au
     * robot quelqu'un qui n'existe plus, et la reconnaissance rendrait un identifiant introuvable.
     */
    @Test
    void supprimerUnePersonneEmporteSesVisages() {
        repository.ajouter(amy.id(), new float[]{1f, 2f});
        repository.ajouter(einstein.id(), new float[]{5f, 6f});

        personneRepository.supprimer(amy.id());

        assertEquals(1, repository.tousLesVisages().size());
        assertEquals(einstein.id(), repository.tousLesVisages().get(0).idPersonne());
    }

    /** Même garde-fou dans l'autre sens : pas d'empreinte rattachée à personne. */
    @Test
    void unVisageNePeutPasDesignerQuelquunDinconnu() {
        assertThrows(RuntimeException.class, () -> repository.ajouter("inexistant", new float[]{1f, 2f}));
    }

    @Test
    void lesVisagesPersistentApresReouvertureDeLaBase() {
        repository.ajouter(einstein.id(), new float[]{5f, 6f});

        VisageConnuRepository relu = new VisageConnuRepository(BaseMemoireDeTest.dans(dossierTemp));
        List<VisageConnu> visages = relu.tousLesVisages();

        assertEquals(1, visages.size());
        assertEquals(einstein.id(), visages.get(0).idPersonne());
        assertArrayEquals(new float[]{5f, 6f}, visages.get(0).embedding());
    }
    /**
     * La reconnaissance garde les empreintes en mémoire et ne relit la base que si ce numéro a
     * bougé : s'il ne bougeait pas, un visage tout juste appris ne serait jamais reconnu.
     */
    @Test
    void ajouterUnVisageChangeLaVersion() {
        int avant = repository.version();

        repository.ajouter(amy.id(), new float[]{1f, 2f});

        assertNotEquals(avant, repository.version());
    }

    /** Et dans l'autre sens : sans changement de version, un visage effacé resterait reconnu. */
    @Test
    void effacerLesVisagesDunePersonneChangeLaVersion() {
        repository.ajouter(amy.id(), new float[]{1f, 2f});
        int avant = repository.version();

        repository.supprimerParPersonne(amy.id());

        assertNotEquals(avant, repository.version());
    }

    /** Lire ne change rien : autrement le cache se rechargerait à chaque image, sans raison. */
    @Test
    void lireNeChangePasLaVersion() {
        repository.ajouter(amy.id(), new float[]{1f, 2f});
        int avant = repository.version();

        repository.tousLesVisages();
        repository.parPersonne(amy.id());
        repository.nombreParPersonne();

        assertEquals(avant, repository.version());
    }
}
