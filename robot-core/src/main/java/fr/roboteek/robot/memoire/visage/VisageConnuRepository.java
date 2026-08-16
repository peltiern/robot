package fr.roboteek.robot.memoire.visage;

import fr.roboteek.robot.Constantes;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(VisageConnuRepository.class);

    /**
     * Nom de la collection MapDB.
     * <p>
     * Suffixé après le passage de {@code VisageConnu(nom, embedding)} à
     * {@code VisageConnu(idPersonne, embedding)}. Sans ce changement de nom, les anciennes
     * entrées seraient relues <b>sans erreur</b> : la désérialisation d'un record apparie les
     * champs par nom, {@code nom} disparaîtrait et {@code idPersonne} vaudrait {@code null} —
     * tous les visages connus deviendraient silencieusement des inconnus, sans le moindre
     * indice dans les logs. L'ancienne collection reste sur le disque, intacte.
     */
    private static final String COLLECTION_VISAGES = "visages-par-personne";

    /** Ancienne collection, relue seulement pour prévenir qu'il faut ré-amorcer la base. */
    private static final String COLLECTION_VISAGES_HERITEE = "visages";

    private final DB db;
    private final Map<String, Object> map;

    public VisageConnuRepository() {
        this(Constantes.DOSSIER_VISAGE + File.separator + "visages-connus.db");
    }

    /** Permet de pointer vers une base isolée (utilisé par les tests). */
    public VisageConnuRepository(String cheminFichier) {
        db = DBMaker.fileDB(cheminFichier).transactionEnable().make();
        map = db.hashMap(COLLECTION_VISAGES, Serializer.STRING, Serializer.JAVA).createOrOpen();
        avertirSiBaseHeritee();
    }

    @SuppressWarnings("unchecked")
    public List<VisageConnu> tousLesVisages() {
        List<VisageConnu> visages = new ArrayList<>();
        for (Object valeur : map.values()) {
            visages.add((VisageConnu) valeur);
        }
        return visages;
    }

    /**
     * Ajoute une empreinte pour une personne. Plusieurs empreintes par personne sont
     * attendues (angles et éclairages différents).
     */
    public void ajouter(String idPersonne, float[] embedding) {
        map.put(UUID.randomUUID().toString(), new VisageConnu(idPersonne, embedding));
        db.commit();
    }

    public void close() {
        db.close();
    }

    /**
     * Prévient si la base contient des visages enregistrés avant le rattachement aux personnes :
     * ils ne sont plus lus, et sans ce message le robot ne reconnaîtrait plus personne sans
     * qu'on comprenne pourquoi.
     */
    private void avertirSiBaseHeritee() {
        if (!map.isEmpty()) {
            return;
        }
        try {
            if (!db.exists(COLLECTION_VISAGES_HERITEE)) {
                return;
            }
            int nombreHerite = db.hashMap(COLLECTION_VISAGES_HERITEE, Serializer.STRING, Serializer.JAVA).open().size();
            if (nombreHerite > 0) {
                logger.warn("{} visage(s) enregistrés dans l'ancien format (sans personne rattachée) sont ignorés."
                        + " Ré-amorcer la base avec SeedVisagesConnus.", nombreHerite);
            }
        } catch (RuntimeException e) {
            // Un simple avertissement ne doit jamais priver le robot de reconnaissance faciale :
            // l'exception remonterait jusqu'à l'initialisation du service, qui serait désactivé.
            logger.debug("Ancienne collection de visages illisible : {}", e.getMessage());
        }
    }
}
