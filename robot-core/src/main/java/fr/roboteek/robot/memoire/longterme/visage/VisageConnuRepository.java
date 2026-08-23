package fr.roboteek.robot.memoire.longterme.visage;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Accès à la table {@code visage} : les empreintes biométriques qui permettent de reconnaître
 * quelqu'un.
 */
@Component
public class VisageConnuRepository {

    /** Taille d'une empreinte SFace, en flottants. Une ligne qui s'en écarte est corrompue. */
    private static final int TAILLE_EMPREINTE = 128;

    private final JdbcClient jdbc;

    /** Voir {@link #version()}. */
    private final AtomicInteger version = new AtomicInteger();

    public VisageConnuRepository(DataSource sourceDeDonneesMemoire) {
        this.jdbc = JdbcClient.create(sourceDeDonneesMemoire);
    }

    /**
     * Numéro de version du contenu de la table, changé à chaque écriture.
     * <p>
     * C'est ce qui permet à la reconnaissance de garder les empreintes en mémoire : elle ne relit
     * la base que si ce numéro a bougé, au lieu de la relire à chaque image.
     */
    public int version() {
        return version.get();
    }

    /**
     * Signale que la table a changé sans passer par ce dépôt : c'est le cas quand on efface une
     * personne, SQLite emportant ses visages en cascade.
     * <p>
     * Sans ce signal, la reconnaissance comparerait encore les visages qu'elle voit à l'empreinte
     * de quelqu'un qui n'existe plus — exactement l'empreinte orpheline que la cascade est là pour
     * éviter.
     */
    public void noterChangementExterne() {
        version.incrementAndGet();
    }

    /** Toutes les empreintes, celles de tout le monde : ce que parcourt la reconnaissance. */
    public List<VisageConnu> tousLesVisages() {
        return jdbc.sql("SELECT id, id_personne, empreinte FROM visage")
                .query(VisageConnuRepository::lire)
                .list();
    }

    /** Les empreintes d'une seule personne. */
    public List<VisageConnu> parPersonne(String idPersonne) {
        return jdbc.sql("SELECT id, id_personne, empreinte FROM visage WHERE id_personne = ?")
                .param(idPersonne)
                .query(VisageConnuRepository::lire)
                .list();
    }

    /**
     * Combien d'empreintes chacun possède, pour toutes les personnes qui en ont au moins une.
     * <p>
     * En une requête et non une par personne : la liste des fiches affiche ce compte, et
     * interroger la base pour chaque ligne d'une liste est le plus sûr moyen de la rendre lente
     * le jour où le robot connaîtra du monde.
     */
    public Map<String, Integer> nombreParPersonne() {
        return jdbc.sql("SELECT id_personne, COUNT(*) AS nombre FROM visage GROUP BY id_personne")
                .query((ligne, numeroLigne) -> Map.entry(ligne.getString("id_personne"), ligne.getInt("nombre")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Ajoute une empreinte pour une personne. Plusieurs empreintes par personne sont
     * attendues (angles et éclairages différents).
     */
    public void ajouter(String idPersonne, float[] embedding) {
        jdbc.sql("INSERT INTO visage (id, id_personne, empreinte) VALUES (?, ?, ?)")
                .params(UUID.randomUUID().toString(), idPersonne, enOctets(embedding))
                .update();
        version.incrementAndGet();
    }

    /**
     * Oublie tous les visages d'une personne, sans oublier la personne elle-même : elle reste
     * connue, mais le robot ne la reconnaîtra plus tant qu'on ne lui aura pas donné d'autres
     * photos.
     *
     * @return le nombre d'empreintes effacées
     */
    public int supprimerParPersonne(String idPersonne) {
        int effaces = jdbc.sql("DELETE FROM visage WHERE id_personne = ?").param(idPersonne).update();
        version.incrementAndGet();
        return effaces;
    }

    private static VisageConnu lire(ResultSet ligne, int numeroLigne) throws SQLException {
        return new VisageConnu(ligne.getString("id"),
                ligne.getString("id_personne"),
                enFlottants(ligne.getBytes("empreinte")));
    }

    /**
     * Little-endian explicite des deux côtés : la valeur par défaut de {@link ByteBuffer} est
     * big-endian, et une base écrite sur une machine puis relue avec l'autre convention ne rendrait
     * que du bruit — des empreintes valides en apparence, mais qui ne reconnaîtraient personne.
     */
    private static byte[] enOctets(float[] embedding) {
        ByteBuffer tampon = ByteBuffer.allocate(embedding.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        tampon.asFloatBuffer().put(embedding);
        return tampon.array();
    }

    private static float[] enFlottants(byte[] octets) {
        float[] embedding = new float[octets.length / Float.BYTES];
        ByteBuffer.wrap(octets).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(embedding);
        return embedding;
    }

    /** Une empreinte de la bonne taille, la seule que le modèle SFace sache comparer. */
    public static boolean estValide(float[] embedding) {
        return embedding != null && embedding.length == TAILLE_EMPREINTE;
    }
}
