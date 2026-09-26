package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.OrganeParole;
import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;
import fr.roboteek.robot.organes.actionneurs.voix.VoixDuRobot;
import fr.roboteek.robot.systemenerveux.event.EssaiDeVoixEvent;
import fr.roboteek.robot.web.controller.dto.EssaiDeVoix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

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
    void setUp() {
        voix = new VoixDuRobot(dossier.resolve("voix.json"));
        controleur = new VoixController(voix, new OrganeParole(voix), publies::add);
    }

    @Test
    void laVoixDeDepartEstLOrigine() {
        var etat = controleur.etat();

        assertEquals(ReglagesVoix.ORIGINE, etat.adoptee());
        assertEquals(ReglagesVoix.ORIGINE, etat.origine());
        assertFalse(etat.reglable(), "organe non démarré : aucun fournisseur n'a encore été choisi");
    }

    @Test
    void adopterChangeLaVoixEtRendLesReglagesBornes() {
        var reponse = controleur.adopter(new ReglagesVoix(-4, 9, 300, 2500, 20, 1, 0.5, 60));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertEquals(2, reponse.getBody().debit());
        assertEquals(reponse.getBody(), controleur.etat().adoptee());
    }

    /** Essayer ne touche pas à la voix adoptée : on écoute avant de choisir. */
    @Test
    void essayerDemandeLaPhraseSansAdopterLaVoix() {
        ReglagesVoix essayee = new ReglagesVoix(-6, 1, 0, 20000, 0, 0, 1, 60);

        var reponse = controleur.essayer(new EssaiDeVoix("Bonjour, je suis Wall-E", essayee));

        assertEquals(HttpStatus.ACCEPTED, reponse.getStatusCode());
        assertEquals(1, publies.size());
        EssaiDeVoixEvent essai = (EssaiDeVoixEvent) publies.getFirst();
        assertEquals("Bonjour, je suis Wall-E", essai.getTexte());
        assertEquals(essayee, essai.getReglages());
        assertEquals(ReglagesVoix.ORIGINE, voix.reglages());
    }

    @Test
    void unEssaiSansPhraseOuSansReglagesEstRefuse() {
        assertEquals(HttpStatus.BAD_REQUEST, controleur.essayer(new EssaiDeVoix("  ", ReglagesVoix.ORIGINE)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controleur.essayer(new EssaiDeVoix("Bonjour", null)).getStatusCode());
        assertTrue(publies.isEmpty());
    }
}
