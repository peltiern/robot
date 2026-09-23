package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.SoundPlayer;
import fr.roboteek.robot.organes.actionneurs.son.BibliothequeDesSons;
import fr.roboteek.robot.web.controller.dto.SonEnCours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ce que le robot fait entendre, vu par HTTP. Aucun son n'est joué ici : {@code play} ne tourne pas
 * en test, et un lecteur en trompe-l'œil suffit à vérifier ce que le contrôleur décide.
 */
class SonEnCoursControllerTest {

    @TempDir
    Path dossier;

    private BibliothequeDesSons bibliotheque;

    private LecteurEnTrompeLOeil lecteur;

    private SonEnCoursController controleur;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesSons(dossier);
        lecteur = new LecteurEnTrompeLOeil();
        controleur = new SonEnCoursController(bibliotheque, lecteur);
        bibliotheque.enregistrer("Coucou", recette(), wav());
    }

    private static JsonNode recette() {
        return JsonMapper.builder().build().readTree("{\"version\":1}");
    }

    private static byte[] wav() {
        byte[] octets = new byte[46];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, octets, 0, 4);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, octets, 8, 4);
        return octets;
    }

    @Test
    void leRobotQuiSeTaitNAPasDeSonEnCours() {
        assertEquals(HttpStatus.NO_CONTENT, controleur.enCours().getStatusCode());
    }

    @Test
    void jouerUnSonDeLaBibliothequeDonneSonFichierAuLecteur() {
        var reponse = controleur.jouer(new SonEnCours("Coucou"));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertEquals(dossier.resolve("Coucou.wav"), lecteur.fichierJoue);
        assertEquals(new SonEnCours("Coucou"), controleur.enCours().getBody());
    }

    @Test
    void unSonInconnuRepondIntrouvable() {
        assertEquals(HttpStatus.NOT_FOUND, controleur.jouer(new SonEnCours("Fantome")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controleur.jouer(new SonEnCours("../evade")).getStatusCode());
    }

    /** 409 et non 500 : l'organe pas encore démarré n'est pas une panne, c'est un refus. */
    @Test
    void unLecteurQuiRefuseDonneUnConflit() {
        lecteur.accepte = false;

        assertEquals(HttpStatus.CONFLICT, controleur.jouer(new SonEnCours("Coucou")).getStatusCode());
    }

    @Test
    void couperEstSansEffetQuandLeRobotSeTaisaitDeja() {
        assertEquals(HttpStatus.NO_CONTENT, controleur.couper().getStatusCode());
        assertTrue(lecteur.coupe);
    }

    /** Un vrai {@link SoundPlayer} non démarré refuse de jouer, sans toucher au matériel. */
    @Test
    void leLecteurNonDemarreRefuse() {
        assertFalse(new SoundPlayer().jouer("Coucou", dossier.resolve("Coucou.wav")));
    }

    private static class LecteurEnTrompeLOeil extends SoundPlayer {

        private Path fichierJoue;

        private String nomJoue;

        private boolean accepte = true;

        private boolean coupe;

        @Override
        public boolean jouer(String nom, Path fichier) {
            if (!accepte) {
                return false;
            }
            nomJoue = nom;
            fichierJoue = fichier;
            return true;
        }

        @Override
        public Optional<String> sonEnCours() {
            return Optional.ofNullable(nomJoue);
        }

        @Override
        public boolean arreterLecture() {
            coupe = true;
            nomJoue = null;
            return true;
        }
    }
}
