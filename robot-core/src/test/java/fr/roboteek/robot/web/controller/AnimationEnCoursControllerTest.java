package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.LecteurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.web.controller.dto.AnimationEnCours;
import fr.roboteek.robot.web.controller.dto.DemandeLecture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La lecture vue comme une ressource : la lire, la remplacer, la retirer. Ce qui s'appelait
 * {@code POST /{nom}/jouer} et {@code POST /stopper}.
 */
class AnimationEnCoursControllerTest {

    @TempDir
    Path dossier;

    private AnnotationConfigApplicationContext contexte;

    private BibliothequeDesAnimations bibliotheque;

    private AnimationEnCoursController controleur;

    private LecteurAnimation lecteur;

    @BeforeEach
    void setUp() {
        contexte = new AnnotationConfigApplicationContext(RobotEventsConfig.class, LecteurAnimation.class);
        lecteur = contexte.getBean(LecteurAnimation.class);
        bibliotheque = new BibliothequeDesAnimations(dossier);
        controleur = new AnimationEnCoursController(bibliotheque, lecteur);
    }

    @AfterEach
    void tearDown() {
        contexte.close();
    }

    private static Animation animation(String nom) {
        return new Animation(nom, 60000, List.of(
                new Piste(Axe.COU_GAUCHE_DROITE, 40, 200,
                        List.of(new ImageCle(0, 0), new ImageCle(60000, 20)))), List.of());
    }

    @Test
    void rienNeJoueAuDepart() {
        assertEquals(HttpStatus.NO_CONTENT, controleur.enCours().getStatusCode());
    }

    @Test
    void uneAnimationDeLaBibliothequeSeLanceParSonNom() {
        bibliotheque.enregistrer(animation("Salut"));

        ResponseEntity<AnimationEnCours> reponse = controleur.lancer(new DemandeLecture("Salut", null));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertEquals("Salut", reponse.getBody().nom());
        assertEquals("Salut", controleur.enCours().getBody().nom());
    }

    /**
     * L'usage principal de l'éditeur pendant qu'on écrit un geste : essayer un brouillon sans
     * l'enregistrer. L'obliger à enregistrer pour voir bouger la tête polluerait la bibliothèque.
     */
    @Test
    void uneAnimationFournieSeJoueSansEtreEnregistree() {
        ResponseEntity<AnimationEnCours> reponse =
                controleur.lancer(new DemandeLecture(null, animation("Brouillon")));

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertTrue(lecteur.enLecture());
        assertEquals(List.of(), bibliotheque.noms(), "un essai ne doit rien laisser sur le disque");
    }

    @Test
    void unNomInconnuNExistePas() {
        assertEquals(HttpStatus.NOT_FOUND,
                controleur.lancer(new DemandeLecture("Fantome", null)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                controleur.lancer(new DemandeLecture(null, null)).getStatusCode());
    }

    /**
     * Un refus doit se voir : sans ça, l'appelant croit que la tête va bouger et attend un
     * mouvement qui ne viendra jamais. 409 et non 500 — le robot va bien, il refuse.
     */
    @Test
    void unArretDUrgenceArmeFaitRefuserLaLecture() {
        bibliotheque.enregistrer(animation("Salut"));
        contexte.publishEvent(new ArretUrgenceEvent(true, "test"));

        assertEquals(HttpStatus.CONFLICT,
                controleur.lancer(new DemandeLecture("Salut", null)).getStatusCode());
        assertFalse(lecteur.enLecture());
    }

    @Test
    void retirerLaLectureLArrete() {
        bibliotheque.enregistrer(animation("Salut"));
        controleur.lancer(new DemandeLecture("Salut", null));

        assertEquals(HttpStatus.NO_CONTENT, controleur.arreter().getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controleur.enCours().getStatusCode());
    }

    /** Retirer ce qui n'existe pas donne le même état : rien ne joue. */
    @Test
    void retirerSansLectureNEstPasUneErreur() {
        assertEquals(HttpStatus.NO_CONTENT, controleur.arreter().getStatusCode());
    }
}
