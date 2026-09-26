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
        assertEquals(Voix.ORIGINE, new VoixDuRobot(dossier.resolve("voix.json")).voix());
    }

    @Test
    void uneVoixAdopteeValeTouteDeSuiteEtAuProchainDemarrage() {
        Path fichier = dossier.resolve("synthese-vocale").resolve("voix.json");
        Voix robot = new Voix("fr_FR-upmc-medium.onnx", 1, new ReglagesVoix(-4, 0.9, 300, 2500, 20, 1, 0.5, 60));

        VoixDuRobot voix = new VoixDuRobot(fichier);
        voix.adopter(robot);

        assertEquals(robot, voix.voix());
        assertEquals(robot, new VoixDuRobot(fichier).voix());
    }

    /** Ce qui arrive par HTTP est borné avant d'être gardé : sinon le robot se tairait à chaque démarrage. */
    @Test
    void uneVoixAdopteeEstBornee() {
        VoixDuRobot voix = new VoixDuRobot(dossier.resolve("voix.json"));

        assertEquals(0.5, voix.adopter(new Voix(null, null, new ReglagesVoix(0, 0, 0, 20000, 0, 0, 0, 0))).reglages().debit());
    }

    /**
     * Le premier format ne gardait que les réglages : une voix adoptée avant le choix du modèle se
     * relit avec le modèle par défaut, sans rien perdre de ses réglages.
     */
    @Test
    void unFichierDuPremierFormatSeRelitAvecLeModeleParDefaut() throws IOException {
        Path fichier = dossier.resolve("voix.json");
        Files.writeString(fichier, """
                {"hauteur":-4,"debit":1,"passeHaut":450,"passeBas":3200,"grain":12,"machine":1,"metal":0.5,"modulation":60}
                """);

        assertEquals(new Voix(null, null, new ReglagesVoix(-4, 1, 450, 3200, 12, 1, 0.5, 60)), new VoixDuRobot(fichier).voix());
    }

    @Test
    void unFichierIllisibleRendLaVoixDOrigine() throws IOException {
        Path fichier = dossier.resolve("voix.json");
        Files.writeString(fichier, "pas du json");

        assertEquals(Voix.ORIGINE, new VoixDuRobot(fichier).voix());
    }
}
