package fr.roboteek.robot.organes.actionneurs.voix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La voix adoptée, gardée d'un démarrage à l'autre. */
class VoixDuRobotTest {

    @TempDir
    Path dossier;

    @Test
    void sansFichierLeRobotParleAvecLaVoixDOrigine() {
        assertEquals(ReglagesVoix.ORIGINE, new VoixDuRobot(dossier.resolve("voix.json")).reglages());
    }

    @Test
    void uneVoixAdopteeValeTouteDeSuiteEtAuProchainDemarrage() {
        Path fichier = dossier.resolve("synthese-vocale").resolve("voix.json");
        ReglagesVoix robot = new ReglagesVoix(-4, 0.9, 300, 2500, 20, 1, 0.5, 60);

        VoixDuRobot voix = new VoixDuRobot(fichier);
        voix.adopter(robot);

        assertEquals(robot, voix.reglages());
        assertEquals(robot, new VoixDuRobot(fichier).reglages());
    }

    /** Ce qui arrive par HTTP est borné avant d'être gardé : sinon le robot se tairait à chaque démarrage. */
    @Test
    void uneVoixAdopteeEstBornee() {
        VoixDuRobot voix = new VoixDuRobot(dossier.resolve("voix.json"));

        assertEquals(0.5, voix.adopter(new ReglagesVoix(0, 0, 0, 20000, 0, 0, 0, 0)).debit());
    }

    @Test
    void unFichierIllisibleRendLaVoixDOrigine() throws IOException {
        Path fichier = dossier.resolve("voix.json");
        Files.writeString(fichier, "pas du json");

        assertEquals(ReglagesVoix.ORIGINE, new VoixDuRobot(fichier).reglages());
    }
}
