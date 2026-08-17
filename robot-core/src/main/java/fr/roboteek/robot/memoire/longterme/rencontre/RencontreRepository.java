package fr.roboteek.robot.memoire.longterme.rencontre;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Accès à la table {@code rencontre}, et rien d'autre. Le sens de ces lignes — quand une rencontre
 * mérite d'être inscrite, et quand elle doit être reprise — est l'affaire de
 * {@link JournalDesRencontres}.
 */
@Component
public class RencontreRepository {

    private final JdbcClient jdbc;

    public RencontreRepository(DataSource sourceDeDonneesMemoire) {
        this.jdbc = JdbcClient.create(sourceDeDonneesMemoire);
    }

    /** La timeline d'une personne, de la plus récente à la plus ancienne. */
    public List<Rencontre> parPersonne(String idPersonne, int limite) {
        return jdbc.sql("""
                        SELECT id, id_personne, instant, type, secondes_absence FROM rencontre
                        WHERE id_personne = ? ORDER BY instant DESC, id DESC LIMIT ?
                        """)
                .params(idPersonne, limite)
                .query(RencontreRepository::lire)
                .list();
    }

    /**
     * Combien de fois chacun a été rencontré, pour tout le monde d'un coup.
     * <p>
     * En une requête et non une par personne : la liste des fiches affiche ce compte, et
     * interroger la base à chaque ligne d'une liste la rendrait lente le jour où le robot
     * connaîtra du monde.
     */
    public Map<String, Integer> nombreParPersonne() {
        return jdbc.sql("SELECT id_personne, COUNT(*) AS nombre FROM rencontre GROUP BY id_personne")
                .query((ligne, numeroLigne) -> Map.entry(ligne.getString("id_personne"), ligne.getInt("nombre")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Écrit une rencontre.
     * <p>
     * {@code RETURNING} et non un {@code SELECT last_insert_rowid()} juste après : faute de pool,
     * chaque requête ouvre sa propre connexion, et ce compteur est propre à une connexion. Le
     * second appel serait tombé sur une connexion neuve et aurait rendu {@code 0} — un identifiant
     * qui ne désigne aucune ligne, et une rencontre refusée qu'on n'aurait jamais su reprendre.
     *
     * @return l'identifiant de la ligne écrite
     */
    public long ajouter(String idPersonne, LocalDateTime instant, Rencontre.Type type, long secondesDAbsence) {
        return jdbc.sql("""
                        INSERT INTO rencontre (id_personne, instant, type, secondes_absence)
                        VALUES (?, ?, ?, ?) RETURNING id
                        """)
                .params(idPersonne, instant.toString(), type.name(), secondesDAbsence)
                .query(Long.class)
                .single();
    }

    /**
     * Efface une ligne précise. Sert quand une rencontre annoncée n'a mené à rien : le cerveau
     * l'a refusée, elle n'a pas eu lieu, elle n'a rien à faire dans une timeline.
     *
     * @return vrai si la ligne existait
     */
    public boolean supprimer(long id) {
        return jdbc.sql("DELETE FROM rencontre WHERE id = ?").param(id).update() > 0;
    }

    /**
     * Ne garde que les {@code aGarder} rencontres les plus récentes d'une personne.
     * <p>
     * Sans quoi la table grossirait sans fin : quelqu'un qui vit dans la pièce est rencontré
     * plusieurs fois par heure, et personne n'ira jamais lire la trois-centième.
     *
     * @return le nombre de lignes effacées
     */
    public int elaguer(String idPersonne, int aGarder) {
        return jdbc.sql("""
                        DELETE FROM rencontre WHERE id_personne = ? AND id NOT IN (
                            SELECT id FROM rencontre WHERE id_personne = ? ORDER BY instant DESC, id DESC LIMIT ?
                        )
                        """)
                .params(idPersonne, idPersonne, aGarder)
                .update();
    }

    private static Rencontre lire(ResultSet ligne, int numeroLigne) throws SQLException {
        return new Rencontre(ligne.getLong("id"),
                ligne.getString("id_personne"),
                LocalDateTime.parse(ligne.getString("instant")),
                Rencontre.Type.valueOf(ligne.getString("type")),
                ligne.getLong("secondes_absence"));
    }
}
