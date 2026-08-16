package fr.roboteek.robot.memoire.longterme;

import javax.sql.DataSource;
import java.io.File;

/**
 * Une mémoire longue jetable, dans un dossier temporaire, réglée exactement comme celle du
 * robot — clés étrangères comprises, sans quoi les tests ne verraient pas les suppressions en
 * cascade et laisseraient passer une base qui garde des empreintes orphelines en production.
 */
public final class BaseMemoireDeTest {

    private BaseMemoireDeTest() {
    }

    public static DataSource dans(File dossier) {
        DataSource source = BaseMemoire.sourceVers(new File(dossier, "memoire.db").getAbsolutePath());
        BaseMemoire.appliquerLeSchema(source);
        return source;
    }
}
