package fr.roboteek.robot.memoire.visage;

import fr.roboteek.robot.Constantes;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stockage persistant (MapDB) des visages connus, pour la reconnaissance faciale.
 * Même pattern que {@code MapDbChatMemoryRepository} (mémoire des conversations).
 */
public class VisageConnuRepository {

    private final DB db;
    private final Map<String, Object> map;

    public VisageConnuRepository() {
        this(Constantes.DOSSIER_VISAGE + File.separator + "visages-connus.db");
    }

    /** Permet de pointer vers une base isolée (utilisé par les tests). */
    public VisageConnuRepository(String cheminFichier) {
        db = DBMaker.fileDB(cheminFichier).transactionEnable().make();
        map = db.hashMap("visages", Serializer.STRING, Serializer.JAVA).createOrOpen();
    }

    @SuppressWarnings("unchecked")
    public List<VisageConnu> tousLesVisages() {
        List<VisageConnu> visages = new ArrayList<>();
        for (Object valeur : map.values()) {
            visages.add((VisageConnu) valeur);
        }
        return visages;
    }

    public void ajouter(String nom, float[] embedding) {
        map.put(UUID.randomUUID().toString(), new VisageConnu(nom, embedding));
        db.commit();
    }

    public void close() {
        db.close();
    }
}
