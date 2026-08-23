package fr.roboteek.robot.memoire.longterme;

import fr.roboteek.robot.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;

/**
 * La base unique où le robot garde ce qui doit lui survivre : les personnes, leurs visages,
 * leurs rencontres et leurs conversations.
 * <p>
 * Un seul fichier SQLite, et des liens en cascade entre les tables : c'est ce qui permet
 * d'effacer une personne <b>et</b> tout ce qui s'y rattache sans en oublier un morceau. Une
 * empreinte orpheline suffirait à faire « reconnaître » au robot quelqu'un qui n'existe plus.
 */
@Configuration
public class BaseMemoire {

    private static final Logger logger = LoggerFactory.getLogger(BaseMemoire.class);

    private static final String FICHIER = "memoire.db";

    /**
     * Source de données de la mémoire longue.
     * <p>
     * Volontairement sans pool de connexions, là où {@code spring-boot-starter-jdbc} en
     * fournirait un : SQLite n'accepte qu'un écrivain à la fois, et une dizaine de connexions
     * concurrentes ne feraient qu'y multiplier les {@code SQLITE_BUSY}. Ouvrir une connexion sur
     * un fichier local coûte de toute façon presque rien, et le robot n'écrit que quelques lignes
     * par minute.
     */
    @Bean
    public DataSource sourceDeDonneesMemoire() {
        File fichier = new File(Constantes.DOSSIER_MEMOIRE, FICHIER);
        // Le dossier peut manquer sur une installation neuve : SQLite échouerait à l'ouverture,
        // au démarrage du robot sur le Jetson, après déploiement.
        fichier.getParentFile().mkdirs();

        DataSource source = sourceVers(fichier.getAbsolutePath());
        appliquerLeSchema(source);
        logger.info("Mémoire longue ouverte : {}", fichier.getAbsolutePath());
        return source;
    }

    /**
     * Ouvre une base à l'emplacement demandé, réglée comme celle du robot. Sert aux tests, qui
     * doivent travailler sur une base isolée mais rigoureusement configurée de la même façon.
     */
    public static DataSource sourceVers(String cheminFichier) {
        SQLiteConfig config = new SQLiteConfig();
        // Sans cela, SQLite ignore purement et simplement les clés étrangères : supprimer une
        // personne laisserait ses visages et ses rencontres derrière elle, et les ON DELETE
        // CASCADE du schéma ne seraient qu'une décoration.
        config.enforceForeignKeys(true);
        // Le journal d'écriture anticipée laisse les lectures se poursuivre pendant une écriture.
        // Le fil de capture vidéo écrit des rencontres pendant que la tablette lit les fiches :
        // sans WAL, l'un des deux échouerait.
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        // Et s'il y a malgré tout collision, attendre plutôt qu'échouer.
        config.setBusyTimeout(5000);

        SQLiteDataSource source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + cheminFichier);
        return source;
    }

    /** Crée les tables manquantes. Rejoué à chaque démarrage, sans effet si tout est déjà là. */
    public static void appliquerLeSchema(DataSource source) {
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(new ClassPathResource("memoire/schema.sql"));
        schema.execute(source);
        ajouterLesColonnesManquantes(source);
    }

    /**
     * Rattrape les colonnes ajoutées à une table qui existait déjà.
     * <p>
     * Indispensable, et facile à oublier : {@code CREATE TABLE IF NOT EXISTS} ne touche pas à une
     * table en place. Sur une base neuve, {@code schema.sql} suffit et ce qui suit ne fait rien ;
     * sur la base du robot, où des personnes sont déjà enregistrées, la colonne ne serait jamais
     * apparue et toute lecture de la vignette aurait échoué au premier appel — en production
     * seulement, jamais sur un poste de développement.
     * <p>
     * SQLite ne connaît pas {@code ADD COLUMN IF NOT EXISTS} : on regarde d'abord ce que la table
     * contient. {@code schema.sql} reste la description de référence — ce qui est ajouté ici doit
     * y figurer aussi, pour qu'une base neuve et une base migrée aient la même forme.
     */
    private static void ajouterLesColonnesManquantes(DataSource source) {
        ajouterLaColonneSiElleManque(source, "personne", "vignette", "BLOB");
    }

    private static void ajouterLaColonneSiElleManque(DataSource source, String table, String colonne, String type) {
        JdbcClient jdbc = JdbcClient.create(source);
        List<String> colonnes = jdbc.sql("SELECT name FROM pragma_table_info(?)")
                .param(table)
                .query(String.class)
                .list();
        if (colonnes.contains(colonne)) {
            return;
        }
        jdbc.sql("ALTER TABLE " + table + " ADD COLUMN " + colonne + " " + type).update();
        logger.info("Mémoire longue : colonne {}.{} ajoutée", table, colonne);
    }
}
