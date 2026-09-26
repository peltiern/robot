package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.son.BibliothequeDesSons;
import fr.roboteek.robot.web.controller.dto.SonEnregistre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La bibliothèque des sons vue par HTTP : lire, enregistrer, servir l'audio, supprimer. */
class SonControllerTest {

    @TempDir
    Path dossier;

    private BibliothequeDesSons bibliotheque;

    private SonController controleur;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesSons(dossier);
        controleur = new SonController(bibliotheque);
    }

    private static JsonNode recette() {
        return JsonMapper.builder().build().readTree("{\"version\":1,\"morceaux\":[]}");
    }

    private static String wavBase64() {
        byte[] octets = new byte[46];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, octets, 0, 4);
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, octets, 8, 4);
        return Base64.getEncoder().encodeToString(octets);
    }

    @Test
    void lePremierEnregistrementCreeLeSonEtLeSuivantLeRemplace() {
        var creation = controleur.enregistrer("Coucou", new SonEnregistre("Coucou", recette(), wavBase64()));
        assertEquals(HttpStatus.CREATED, creation.getStatusCode());

        var remplacement = controleur.enregistrer("Coucou", new SonEnregistre("Coucou", recette(), wavBase64()));
        assertEquals(HttpStatus.NO_CONTENT, remplacement.getStatusCode());
        assertEquals(List.of("Coucou"), controleur.noms());
    }

    /**
     * Le cas qui a échoué sur le robot le 2026-09-24 : « nouveau son 2 » était écrit, puis la
     * fabrication de son adresse levait une exception, et le Studio recevait une 500 pour un son
     * pourtant enregistré — qu'il gardait alors en attente dans le navigateur.
     */
    @Test
    void creerUnNomAEspaceRendUneAdresseEncodee() {
        var parLeNom = controleur.enregistrer("nouveau son 2", new SonEnregistre("nouveau son 2", recette(), wavBase64()));
        var parLaCreation = controleur.creer(new SonEnregistre("nouveau son 3", recette(), wavBase64()));

        assertEquals(HttpStatus.CREATED, parLeNom.getStatusCode());
        assertEquals("/api/sons/nouveau%20son%202", parLeNom.getHeaders().getLocation().toString());
        assertEquals(HttpStatus.CREATED, parLaCreation.getStatusCode());
        assertEquals("/api/sons/nouveau%20son%203", parLaCreation.getHeaders().getLocation().toString());
    }

    /** Un accent dans l'adresse de retour s'encode en UTF-8, comme le navigateur l'a envoyé. */
    @Test
    void creerUnNomAccentueRendUneAdresseEncodee() {
        var creation = controleur.enregistrer("arpège", new SonEnregistre("arpège", recette(), wavBase64()));

        assertEquals(HttpStatus.CREATED, creation.getStatusCode());
        assertEquals("/api/sons/arp%C3%A8ge", creation.getHeaders().getLocation().toString());
    }

    @Test
    void laRecetteEtLAudioSeRelisentParLeurNom() {
        controleur.enregistrer("Coucou", new SonEnregistre("Coucou", recette(), wavBase64()));

        assertEquals(recette(), controleur.recette("Coucou").getBody());
        assertEquals(HttpStatus.OK, controleur.audio("Coucou").getStatusCode());
    }

    @Test
    void unSonInconnuRepondIntrouvable() {
        assertEquals(HttpStatus.NOT_FOUND, controleur.recette("Fantome").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controleur.audio("Fantome").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controleur.supprimer("Fantome").getStatusCode());
    }

    /**
     * 400 et non 500 : un envoi qui ne convient pas n'est pas une panne du robot, et l'éditeur doit
     * pouvoir dire à quoi il tient — nom refusé, recette sans version, audio qui n'est pas un WAV.
     */
    @Test
    void unEnvoiQuiNeConvientPasEstRefuseSansRienEcrire() {
        JsonNode sansVersion = JsonMapper.builder().build().readTree("{}");

        assertEquals(HttpStatus.BAD_REQUEST,
                controleur.enregistrer("Coucou", new SonEnregistre("Coucou", sansVersion, wavBase64())).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                controleur.enregistrer("Coucou", new SonEnregistre("Coucou", recette(), Base64.getEncoder()
                        .encodeToString("pas du son".getBytes(StandardCharsets.UTF_8)))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                controleur.enregistrer("Coucou", new SonEnregistre("Coucou", recette(), "ceci n'est pas du base64 !")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                controleur.enregistrer("../evade", new SonEnregistre("../evade", recette(), wavBase64())).getStatusCode());

        assertFalse(bibliotheque.existe("Coucou"));
        assertEquals(List.of(), controleur.noms());
    }

    /**
     * Comme pour les animations : la création refuse d'écraser, ce qui protège l'« Enregistrer
     * sous » d'un nom déjà pris. C'est le remplacement qui est explicite.
     */
    @Test
    void laCreationRefuseDEcraserUnSonExistant() {
        assertEquals(HttpStatus.CREATED,
                controleur.creer(new SonEnregistre("Coucou", recette(), wavBase64())).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                controleur.creer(new SonEnregistre("Coucou", recette(), wavBase64())).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                controleur.creer(new SonEnregistre(null, recette(), wavBase64())).getStatusCode());
        assertEquals(List.of("Coucou"), controleur.noms());
    }

    @Test
    void supprimerRendLaPlace() {
        controleur.enregistrer("Coucou", new SonEnregistre("Coucou", recette(), wavBase64()));

        assertEquals(HttpStatus.NO_CONTENT, controleur.supprimer("Coucou").getStatusCode());
        assertTrue(controleur.noms().isEmpty());
    }
}
