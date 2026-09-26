package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.OrganeParole;
import fr.roboteek.robot.organes.actionneurs.voix.CatalogueDesModeles;
import fr.roboteek.robot.organes.actionneurs.voix.ModeleVoix;
import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;
import fr.roboteek.robot.organes.actionneurs.voix.Voix;
import fr.roboteek.robot.organes.actionneurs.voix.VoixDuRobot;
import fr.roboteek.robot.systemenerveux.event.EssaiDeVoixEvent;
import fr.roboteek.robot.web.controller.dto.EssaiDeVoix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La voix vue par HTTP : la lire, l'adopter, l'essayer sans l'adopter. */
class VoixControllerTest {

    @TempDir
    Path dossier;

    private VoixDuRobot voix;

    private final List<Object> publies = new ArrayList<>();

    private VoixController controleur;

    @BeforeEach
    void setUp() throws IOException {
        Path modeles = Files.createDirectories(dossier.resolve("models"));
        Files.writeString(modeles.resolve("fr_FR-tom-medium.onnx"), "modèle");
        Files.writeString(modeles.resolve("fr_FR-tom-medium.onnx.json"), "{}");
        voix = new VoixDuRobot(dossier.resolve("voix.json"));
        controleur = new VoixController(voix, new CatalogueDesModeles(modeles), new OrganeParole(voix), publies::add);
    }

    @Test
    void laVoixDeDepartEstLOrigineEtLesModelesDeposesSontProposes() {
        var etat = controleur.etat();

        assertEquals(Voix.ORIGINE, etat.adoptee());
        assertEquals(ReglagesVoix.ORIGINE, etat.origine());
        assertFalse(etat.reglable(), "organe non démarré : aucun fournisseur n'a encore été choisi");
        assertEquals(List.of(new ModeleVoix("fr_FR-tom-medium.onnx", null, "fr_FR-tom-medium")), etat.modeles());
    }

    @Test
    void adopterChangeLaVoixEtRendLesReglagesBornes() {
        var reponse = controleur.adopter(new Voix("fr_FR-tom-medium.onnx", null, new ReglagesVoix(-4, 9, 300, 2500, 20, 1, 0.5, 60)));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertEquals(2, reponse.getBody().reglages().debit());
        assertEquals(reponse.getBody(), controleur.etat().adoptee());
    }

    /** Le nom du modèle finit dans un chemin : un modèle qui n'est pas déposé est refusé. */
    @Test
    void unModeleInconnuEstRefuse() {
        Voix ailleurs = new Voix("../../etc/passwd", null, ReglagesVoix.ORIGINE);

        assertEquals(HttpStatus.BAD_REQUEST, controleur.adopter(ailleurs).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controleur.essayer(new EssaiDeVoix("Bonjour", ailleurs)).getStatusCode());
        assertEquals(Voix.ORIGINE, voix.voix());
        assertTrue(publies.isEmpty());
    }

    /** Essayer ne touche pas à la voix adoptée : on écoute avant de choisir. */
    @Test
    void essayerDemandeLaPhraseSansAdopterLaVoix() {
        Voix essayee = new Voix("fr_FR-tom-medium.onnx", null, new ReglagesVoix(-6, 1, 0, 20000, 0, 0, 1, 60));

        var reponse = controleur.essayer(new EssaiDeVoix("Bonjour, je suis Wall-E", essayee));

        assertEquals(HttpStatus.ACCEPTED, reponse.getStatusCode());
        assertEquals(1, publies.size());
        EssaiDeVoixEvent essai = (EssaiDeVoixEvent) publies.getFirst();
        assertEquals("Bonjour, je suis Wall-E", essai.getTexte());
        assertEquals(essayee, essai.getVoix());
        assertEquals(Voix.ORIGINE, voix.voix());
    }

    @Test
    void unEssaiSansPhraseOuSansReglagesEstRefuse() {
        assertEquals(HttpStatus.BAD_REQUEST, controleur.essayer(new EssaiDeVoix("  ", Voix.ORIGINE)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controleur.essayer(new EssaiDeVoix("Bonjour", null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controleur.essayer(new EssaiDeVoix("Bonjour", new Voix(null, null, null))).getStatusCode());
        assertTrue(publies.isEmpty());
    }
}
