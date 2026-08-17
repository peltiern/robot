package fr.roboteek.robot.memoire.longterme.personne;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Accès à la table {@code personne} de la mémoire longue, et rien d'autre.
 * <p>
 * Ce dépôt ignore tout des visages, des rencontres et des conversations : effacer une personne
 * <b>partout</b> est l'affaire de {@code RepertoireDesPersonnes}, qui les connaît toutes.
 */
@Component
public class PersonneRepository {

    private final JdbcClient jdbc;

    public PersonneRepository(DataSource sourceDeDonneesMemoire) {
        this.jdbc = JdbcClient.create(sourceDeDonneesMemoire);
    }

    /**
     * @return la personne portant cet identifiant, ou {@code null} si elle est inconnue
     */
    public Personne parId(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT id, prenom, derniere_rencontre FROM personne WHERE id = ?")
                .param(id)
                .query(PersonneRepository::lire)
                .optional()
                .orElse(null);
    }

    /** Toutes les personnes connues, par ordre alphabétique — c'est ainsi qu'on les lit. */
    public List<Personne> toutes() {
        return jdbc.sql("SELECT id, prenom, derniere_rencontre FROM personne ORDER BY prenom COLLATE NOCASE")
                .query(PersonneRepository::lire)
                .list();
    }

    /** Crée ou met à jour la personne (la clé est son identifiant). */
    public void enregistrer(Personne personne) {
        jdbc.sql("""
                        INSERT INTO personne (id, prenom, derniere_rencontre) VALUES (?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET prenom = excluded.prenom,
                                                      derniere_rencontre = excluded.derniere_rencontre
                        """)
                .params(personne.id(),
                        personne.prenom(),
                        personne.derniereRencontre() == null ? null : personne.derniereRencontre().toString())
                .update();
    }

    /**
     * Le portrait d'une personne, en JPEG.
     * <p>
     * Lu à part et jamais avec la fiche : une liste de trente personnes ne doit pas traîner trente
     * images derrière elle alors qu'aucune n'est affichée en pleine taille. C'est aussi pourquoi
     * les autres requêtes nomment leurs colonnes au lieu d'un {@code SELECT *}.
     *
     * @return les octets, ou {@code null} si la personne est inconnue ou n'a pas de portrait
     */
    public byte[] vignette(String id) {
        if (id == null) {
            return null;
        }
        return jdbc.sql("SELECT vignette FROM personne WHERE id = ?")
                .param(id)
                .query(byte[].class)
                .optional()
                .orElse(null);
    }

    /** Pose ou remplace le portrait. Le dernier enrôlement fait foi : les gens changent. */
    public void enregistrerVignette(String id, byte[] vignette) {
        jdbc.sql("UPDATE personne SET vignette = ? WHERE id = ?").params(vignette, id).update();
    }

    /** Les personnes qui ont un portrait, pour n'afficher un rond vide que là où il en manque un. */
    public Set<String> idsAvecVignette() {
        return Set.copyOf(jdbc.sql("SELECT id FROM personne WHERE vignette IS NOT NULL")
                .query(String.class)
                .list());
    }

    /**
     * Efface la personne, et avec elle ses visages et ses rencontres — la base s'en charge
     * ({@code ON DELETE CASCADE}). Sa conversation, elle, n'est pas rattachée par clé étrangère
     * et reste à effacer par ailleurs.
     *
     * @return vrai si quelqu'un a effectivement été supprimé
     */
    public boolean supprimer(String id) {
        return jdbc.sql("DELETE FROM personne WHERE id = ?").param(id).update() > 0;
    }

    private static Personne lire(ResultSet ligne, int numeroLigne) throws SQLException {
        String derniereRencontre = ligne.getString("derniere_rencontre");
        return new Personne(ligne.getString("id"),
                ligne.getString("prenom"),
                derniereRencontre == null ? null : LocalDateTime.parse(derniereRencontre));
    }
}
