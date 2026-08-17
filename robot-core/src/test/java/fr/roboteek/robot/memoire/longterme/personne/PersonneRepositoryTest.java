package fr.roboteek.robot.memoire.longterme.personne;

import fr.roboteek.robot.memoire.longterme.BaseMemoireDeTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la persistance des personnes.
 */
class PersonneRepositoryTest {

    @TempDir
    File dossierTemp;

    private DataSource memoire;

    private PersonneRepository repository;

    @BeforeEach
    void setUp() {
        memoire = BaseMemoireDeTest.dans(dossierTemp);
        repository = new PersonneRepository(memoire);
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
        repository.enregistrer(marie.rencontreeLe(maintenant));

        assertEquals(1, repository.toutes().size());
        Personne relue = repository.parId(marie.id());
        assertEquals(maintenant, relue.derniereRencontre());
    }

    @Test
    void lesPersonnesSontRenduesParOrdreAlphabetique() {
        repository.enregistrer(Personne.nouvelle("zoé"));
        repository.enregistrer(Personne.nouvelle("Alice"));
        repository.enregistrer(Personne.nouvelle("marc"));

        assertEquals(List.of("Alice", "marc", "zoé"), repository.toutes().stream().map(Personne::prenom).toList());
    }

    @Test
    void supprimerUnePersonneLaFaitDisparaitre() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);

        assertTrue(repository.supprimer(marie.id()));

        assertNull(repository.parId(marie.id()));
        assertTrue(repository.toutes().isEmpty());
    }

    @Test
    void supprimerQuelquunQuiNexistePasNeFaitRien() {
        assertFalse(repository.supprimer("inexistant"));
    }

    @Test
    void sansPortraitLaVignetteEstAbsente() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);

        assertNull(repository.vignette(marie.id()));
        assertNull(repository.vignette("inexistant"));
        assertTrue(repository.idsAvecVignette().isEmpty());
    }

    @Test
    void enregistrerPuisRelireUnPortrait() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);

        repository.enregistrerVignette(marie.id(), "un-jpeg".getBytes());

        assertArrayEquals("un-jpeg".getBytes(), repository.vignette(marie.id()));
        assertEquals(Set.of(marie.id()), repository.idsAvecVignette());
    }

    /** Le dernier enrôlement fait foi : les gens changent, la photo doit suivre. */
    @Test
    void unNouveauPortraitRemplaceLAncien() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);
        repository.enregistrerVignette(marie.id(), "ancien".getBytes());

        repository.enregistrerVignette(marie.id(), "recent".getBytes());

        assertArrayEquals("recent".getBytes(), repository.vignette(marie.id()));
    }

    /**
     * Renommer ne touche pas au portrait.
     * <p>
     * {@code enregistrer} fait un {@code INSERT ... ON CONFLICT DO UPDATE} qui ne nomme que le
     * prénom et la date : s'il écrasait toute la ligne, corriger un prénom mal compris effacerait
     * la photo au passage, sans que rien ne le signale.
     */
    @Test
    void renommerNeFaitPasPerdreLePortrait() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);
        repository.enregistrerVignette(marie.id(), "un-jpeg".getBytes());

        repository.enregistrer(marie.renommee("Marion"));

        assertEquals("Marion", repository.parId(marie.id()).prenom());
        assertArrayEquals("un-jpeg".getBytes(), repository.vignette(marie.id()));
    }

    @Test
    void lesPersonnesPersistentApresReouvertureDeLaBase() {
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);

        PersonneRepository relu = new PersonneRepository(BaseMemoireDeTest.dans(dossierTemp));

        assertEquals("Marie", relu.parId(marie.id()).prenom());
    }
}
