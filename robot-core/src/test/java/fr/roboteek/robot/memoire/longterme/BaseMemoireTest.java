package fr.roboteek.robot.memoire.longterme;

import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie l'ouverture de la mémoire longue, et surtout ce qui arrive à une base <b>déjà en
 * place</b> quand le schéma évolue.
 */
class BaseMemoireTest {

    @TempDir
    File dossierTemp;

    @Test
    void leSchemaSeRejoueSansDommageSurUneBaseDejaCreee() {
        DataSource source = BaseMemoireDeTest.dans(dossierTemp);
        PersonneRepository repository = new PersonneRepository(source);
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);

        BaseMemoire.appliquerLeSchema(source);

        assertEquals("Marie", repository.parId(marie.id()).prenom());
    }

    /**
     * Le piège de l'évolution de schéma : {@code CREATE TABLE IF NOT EXISTS} ne touche pas à une
     * table qui existe déjà. Sur la base du robot, où des personnes sont enregistrées depuis des
     * jours, une colonne ajoutée à {@code schema.sql} ne serait jamais apparue — et la première
     * lecture aurait échoué en production, jamais sur un poste de développement.
     * <p>
     * On reconstitue donc l'état d'avant : une table {@code personne} sans sa colonne
     * {@code vignette}, avec quelqu'un dedans.
     */
    @Test
    void uneColonneAjouteeApparaitSurUneBaseAncienneSansPerdreLesDonnees() {
        DataSource source = BaseMemoire.sourceVers(new File(dossierTemp, "ancienne.db").getAbsolutePath());
        JdbcClient jdbc = JdbcClient.create(source);
        jdbc.sql("""
                CREATE TABLE personne (
                    id TEXT PRIMARY KEY,
                    prenom TEXT NOT NULL,
                    derniere_rencontre TEXT
                )
                """).update();
        jdbc.sql("INSERT INTO personne (id, prenom) VALUES ('id-nicolas', 'Nicolas')").update();

        BaseMemoire.appliquerLeSchema(source);

        List<String> colonnes = jdbc.sql("SELECT name FROM pragma_table_info('personne')")
                .query(String.class)
                .list();
        assertTrue(colonnes.contains("vignette"), "colonnes trouvées : " + colonnes);

        PersonneRepository repository = new PersonneRepository(source);
        assertEquals("Nicolas", repository.parId("id-nicolas").prenom(), "la personne existante est intacte");

        repository.enregistrerVignette("id-nicolas", "un-jpeg".getBytes());
        assertArrayEquals("un-jpeg".getBytes(), repository.vignette("id-nicolas"));
    }

    /** Et rejouer la migration ne doit pas échouer sur une colonne déjà ajoutée. */
    @Test
    void laMigrationSeRejoueSansErreur() {
        DataSource source = BaseMemoireDeTest.dans(dossierTemp);

        BaseMemoire.appliquerLeSchema(source);
        BaseMemoire.appliquerLeSchema(source);

        PersonneRepository repository = new PersonneRepository(source);
        Personne marie = Personne.nouvelle("Marie");
        repository.enregistrer(marie);
        repository.enregistrerVignette(marie.id(), "un-jpeg".getBytes());
        assertArrayEquals("un-jpeg".getBytes(), repository.vignette(marie.id()));
    }
}
