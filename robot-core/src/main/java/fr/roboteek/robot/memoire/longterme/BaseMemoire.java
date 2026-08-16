package fr.roboteek.robot.memoire.longterme;

import fr.roboteek.robot.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;

/**
 * La base unique où le robot garde ce qui doit lui survivre : les personnes, leurs visages,
 * leurs rencontres et leurs conversations.
 * <p>
 * Un seul fichier SQLite, {@code memoire.db}, là où il y avait quatre bases MapDB séparées.
 * Ce n'est pas un détail de rangement : c'est ce qui permet d'effacer une personne <b>et</b>
 * tout ce qui s'y rattache sans risquer d'en oublier un morceau, une empreinte orpheline
 * suffisant à faire « reconnaître » au robot quelqu'un qui n'existe plus.
 * <p>
 * MapDB a été abandonné le 2026-08-16 pour trois raisons tenaces :
 * <ul>
 *   <li>il stockait les records par <b>sérialisation Java</b> — ajouter un champ relisait les
 *       anciennes entrées sans la moindre erreur, le champ à {@code null} ;</li>
 *   <li>il <b>verrouillait son fichier</b>, si bien qu'un dépôt ne pouvait pas être partagé
 *       entre plusieurs beans : écrire une empreinte devait passer par le service de
 *       reconnaissance, seul détenteur du verrou ;</li>
 *   <li>il n'offrait <b>ni index ni requête</b> : retrouver les visages d'une personne imposait
 *       de tout charger et de filtrer en mémoire.</li>
 * </ul>
 * Les données MapDB n'ont pas été reprises : on est reparti d'une base vide, les visages se
 * réimportant par fichier.
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
    }
}
