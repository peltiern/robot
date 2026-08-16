package fr.roboteek.robot.memoire.personne;

import fr.roboteek.robot.Constantes;
import jakarta.annotation.PreDestroy;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stockage persistant (MapDB) des personnes que le robot connaît.
 * <p>
 * Même pattern que {@code MapDbChatMemoryRepository} et {@code VisageConnuRepository}, à une
 * différence près : celui-ci est un bean Spring, parce que la reconnaissance comme le registre
 * de présence doivent voir la même base — MapDB verrouille son fichier, deux ouvertures
 * concurrentes échouent.
 */
@Component
public class PersonneRepository {

    private final DB db;
    private final Map<String, Object> map;

    public PersonneRepository() {
        this(Constantes.DOSSIER_MEMOIRE + File.separator + "personnes.db");
    }

    /** Permet de pointer vers une base isolée (utilisé par les tests). */
    public PersonneRepository(String cheminFichier) {
        // Le dossier de mémoire peut ne pas exister sur une installation neuve : MapDB
        // n'échouerait qu'au démarrage du robot, sur le Jetson, après déploiement.
        File parent = new File(cheminFichier).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        db = DBMaker.fileDB(cheminFichier).transactionEnable().make();
        map = db.hashMap("personnes", Serializer.STRING, Serializer.JAVA).createOrOpen();
    }

    /**
     * @return la personne portant cet identifiant, ou {@code null} si elle est inconnue
     */
    public Personne parId(String id) {
        return id == null ? null : (Personne) map.get(id);
    }

    public List<Personne> toutes() {
        List<Personne> personnes = new ArrayList<>();
        for (Object valeur : map.values()) {
            personnes.add((Personne) valeur);
        }
        return personnes;
    }

    /** Crée ou met à jour la personne (la clé est son identifiant). */
    public void enregistrer(Personne personne) {
        map.put(personne.id(), personne);
        db.commit();
    }

    @PreDestroy
    public void close() {
        db.close();
    }
}
