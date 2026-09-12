package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.organes.actionneurs.animation.BibliothequeDesAnimations;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import fr.roboteek.robot.organes.actionneurs.animation.modele.SonDeclenche;
import fr.roboteek.robot.organes.actionneurs.RobotSound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La bibliothèque vue par HTTP : lire, créer, remplacer, supprimer. Rien qui joue. */
class AnimationControllerTest {

    @TempDir
    Path dossier;

    private BibliothequeDesAnimations bibliotheque;

    private AnimationController controleur;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesAnimations(dossier);
        controleur = new AnimationController(bibliotheque);
    }

    private static Animation animation(String nom) {
        return new Animation(nom, 2000, List.of(
                new Piste(Axe.COU_GAUCHE_DROITE, 40, 200,
                        List.of(new ImageCle(0, 0), new ImageCle(2000, 20)))), List.of());
    }

    @Test
    void lesNomsViennentDeLaBibliotheque() {
        bibliotheque.enregistrer(animation("Salut"));
        bibliotheque.enregistrer(animation("Colere"));

        assertEquals(List.of("Colere", "Salut"), controleur.noms());
    }

    @Test
    void uneAnimationInconnueNExistePas() {
        assertEquals(HttpStatus.NOT_FOUND, controleur.animation("Fantome").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controleur.supprimer("Fantome").getStatusCode());
    }

    @Nested
    class Ecriture {

        @Test
        void creerRefuseDEcraserCeQuiExiste() {
            controleur.creer(animation("Salut"));

            assertEquals(HttpStatus.CONFLICT, controleur.creer(animation("Salut")).getStatusCode());
        }

        /**
         * Le nom de l'URL fait foi : c'est ce qui permet d'enregistrer sous un autre nom sans que
         * l'éditeur ait à modifier son brouillon, et ça reste vrai après un renommage de fichier.
         */
        @Test
        void leNomDeLUrlLEmporteSurCeluiDuCorps() {
            controleur.remplacer("Coucou", animation("Salut"));

            assertEquals(List.of("Coucou"), controleur.noms());
            assertEquals("Coucou", bibliotheque.charger("Coucou").orElseThrow().nom());
        }

        /**
         * Le fichier serait écrit, puis ignoré au prochain chargement : l'animation disparaîtrait
         * au redémarrage sans un mot. C'est arrivé avec l'éditeur resté à la version 1.
         */
        @Test
        void uneAnimationDUneAutreVersionNEstPasEcrite() {
            Animation perimee = new Animation("Salut", 2000, animation("Salut").pistes(), List.of(), 1);

            assertEquals(HttpStatus.BAD_REQUEST, controleur.remplacer("Salut", perimee).getStatusCode());
            assertEquals(HttpStatus.BAD_REQUEST, controleur.creer(perimee).getStatusCode());
            assertEquals(List.of(), controleur.noms());
        }

        /**
         * Une transition trop rapide pour le servo n'empêche pas d'enregistrer — un brouillon est
         * légitime — mais elle doit être dite, sinon l'écart entre la courbe et le mouvement reste
         * inexplicable.
         */
        @Test
        void uneTransitionInfaisableEstEnregistreeMaisSignalee() {
            Animation tropVite = new Animation("Trop vite", 200, List.of(
                    new Piste(Axe.COU_GAUCHE_DROITE, 40, 200,
                            List.of(new ImageCle(0, -40), new ImageCle(200, 40)))), List.of());

            ResponseEntity<List<String>> reponse = controleur.remplacer("Trop vite", tropVite);

            assertEquals(HttpStatus.OK, reponse.getStatusCode());
            assertTrue(bibliotheque.existe("Trop vite"), "un brouillon s'enregistre quand même");
            assertFalse(reponse.getBody().isEmpty(), "80° en 200 ms doit être signalé");
        }

        @Test
        void uneAnimationJouableNAvertitDeRien() {
            assertEquals(List.of(), controleur.remplacer("Salut", animation("Salut")).getBody());
        }

        /**
         * L'éditeur ne connaît pas encore les sons : il relit une animation, la renvoie, et les
         * effacerait sans que personne ne l'ait demandé.
         */
        @Test
        void enregistrerSansSonNEffacePasCeuxQuiExistent() {
            bibliotheque.enregistrer(new Animation("Salut", 2000, animation("Salut").pistes(),
                    List.of(new SonDeclenche(500, RobotSound.values()[0]))));

            controleur.remplacer("Salut", animation("Salut"));

            assertEquals(1, bibliotheque.charger("Salut").orElseThrow().sons().size(),
                    "la piste son doit survivre à un enregistrement qui l'ignore");
        }

        @Test
        void supprimerDitSiIlYAvaitQuelqueChose() {
            controleur.creer(animation("Salut"));

            assertEquals(HttpStatus.NO_CONTENT, controleur.supprimer("Salut").getStatusCode());
            assertEquals(List.of(), controleur.noms());
        }
    }
}
