package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.LecteurAnimation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.web.controller.dto.Lecture;
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

class AnimationControllerTest {

    @TempDir
    Path dossier;

    private AnnotationConfigApplicationContext contexte;

    private AnimationController controleur;

    private LecteurAnimation lecteur;

    @BeforeEach
    void setUp() {
        contexte = new AnnotationConfigApplicationContext(RobotEventsConfig.class, LecteurAnimation.class);
        lecteur = contexte.getBean(LecteurAnimation.class);
        controleur = new AnimationController(new BibliothequeDesAnimations(dossier), lecteur);
    }

    @AfterEach
    void tearDown() {
        contexte.close();
    }

    private void deposer(String nom) {
        new BibliothequeDesAnimations(dossier).enregistrer(new Animation(nom, 2000, List.of(
                new Piste(Axe.COU_GAUCHE_DROITE, 40, 200,
                        List.of(new ImageCle(0, 0), new ImageCle(2000, 20)))), List.of()));
    }

    @Test
    void lesNomsViennentDeLaBibliotheque() {
        deposer("Salut");
        deposer("Colere");

        assertEquals(List.of("Colere", "Salut"), controleur.noms());
    }

    @Test
    void uneAnimationInconnueNExistePas() {
        assertEquals(HttpStatus.NOT_FOUND, controleur.animation("Fantome").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controleur.jouer("Fantome").getStatusCode());
    }

    @Test
    void uneAnimationConnueEstLancee() {
        deposer("Salut");

        ResponseEntity<Lecture> reponse = controleur.jouer("Salut");

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertTrue(reponse.getBody().lancee());
        assertTrue(lecteur.enLecture());
    }

    /**
     * Une transition trop rapide pour le servo n'empêche pas de jouer — le mouvement sera
     * simplement en retard sur la courbe. Mais elle doit être dite, sinon on cherche la panne
     * dans le lecteur.
     */
    @Test
    void uneTransitionInfaisableEstJoueeMaisSignalee() {
        new BibliothequeDesAnimations(dossier).enregistrer(new Animation("Trop vite", 200, List.of(
                new Piste(Axe.COU_GAUCHE_DROITE, 40, 200,
                        List.of(new ImageCle(0, -40), new ImageCle(200, 40)))), List.of()));

        ResponseEntity<Lecture> reponse = controleur.jouer("Trop vite");

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertTrue(reponse.getBody().lancee(), "Un avertissement n'est pas un refus");
        assertFalse(reponse.getBody().avertissements().isEmpty(), "80° en 200 ms doit être signalé");
    }

    /**
     * Un refus doit se voir : sans ça, l'appelant croit que la tête va bouger et attend un
     * mouvement qui ne viendra jamais. 409 et non 500 — le robot va bien, il refuse.
     */
    @Test
    void unArretDUrgenceArmeFaitRefuserLaLecture() {
        deposer("Salut");
        contexte.publishEvent(new ArretUrgenceEvent(true, "test"));

        ResponseEntity<Lecture> reponse = controleur.jouer("Salut");

        assertEquals(HttpStatus.CONFLICT, reponse.getStatusCode());
        assertFalse(reponse.getBody().lancee());
        assertFalse(lecteur.enLecture());
    }

    @Test
    void stopperLibereLeLecteur() {
        deposer("Salut");
        controleur.jouer("Salut");

        controleur.stopper();

        assertFalse(lecteur.enLecture());
    }
}
