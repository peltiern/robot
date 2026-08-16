package fr.roboteek.robot.memoire.personne;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la persistance MapDB des personnes.
 */
class PersonneRepositoryTest {

    @TempDir
    File dossierTemp;

    private PersonneRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PersonneRepository(cheminBase());
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void uneBaseVideNeContientPersonne() {
        assertTrue(repository.toutes().isEmpty());
        assertNull(repository.parId("inexistant"));
    }

    @Test
    void unIdentifiantNulNeFaitPasEchouerLaRecherche() {
        assertNull(repository.parId(null));
    }

    @Test
    void enregistrerPuisRelireUnePersonne() {
        Personne marie = Personne.nouvelle("Marie");

        repository.enregistrer(marie);

        Personne relue = repository.parId(marie.id());
        assertEquals("Marie", relue.prenom());
        assertNull(relue.derniereRencontre());
        assertNull(relue.resumeDerniereConversation());
    }

    @Test
    void deuxPersonnesDuMemePrenomRestentDistinctes() {
        Personne premiere = Personne.nouvelle("Marie");
        Personne seconde = Personne.nouvelle("Marie");

        repository.enregistrer(premiere);
        repository.enregistrer(seconde);

        assertNotEquals(premiere.id(), seconde.id());
        assertEquals(2, repository.toutes().size());
    }

    @Test
    void enregistrerUnePersonneExistanteLaMetAJour() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);

        LocalDateTime maintenant = LocalDateTime.of(2026, 8, 9, 15, 0);
        repository.enregistrer(marie.rencontreeLe(maintenant).avecResumeDeConversation("son chat"));

        assertEquals(1, repository.toutes().size());
        Personne relue = repository.parId(marie.id());
        assertEquals(maintenant, relue.derniereRencontre());
        assertEquals("son chat", relue.resumeDerniereConversation());
    }

    @Test
    void lesPersonnesPersistentApresReouvertureDeLaBase() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);
        repository.close();

        repository = new PersonneRepository(cheminBase());

        assertEquals("Marie", repository.parId(marie.id()).prenom());
    }

    private String cheminBase() {
        return new File(dossierTemp, "personnes.db").getAbsolutePath();
    }
}
