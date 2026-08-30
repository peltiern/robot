package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibliothequeDesAnimationsTest {

    @TempDir
    Path dossier;

    private BibliothequeDesAnimations bibliotheque;

    @BeforeEach
    void setUp() {
        bibliotheque = new BibliothequeDesAnimations(dossier);
    }

    private static Animation animation(String nom) {
        return new Animation(nom, 3000, List.of(
                new Piste(Axe.OEIL_GAUCHE, 40, 60, List.of(new ImageCle(0, 0), new ImageCle(1500, -12.5))),
                new Piste(Axe.COU_GAUCHE_DROITE, 100, 200, List.of(new ImageCle(0, 0, 25d, 90d)))), List.of());
    }

    @Nested
    class AllerRetour {

        @Test
        void uneAnimationEnregistreeSeRelitIdentique() {
            Animation attendue = animation("Salut");

            bibliotheque.enregistrer(attendue);

            assertEquals(Optional.of(attendue), bibliotheque.charger("Salut"));
        }

        /**
         * C'est ce qui a fait abandonner MapDB : une forme de données écrite nulle part, où un
         * champ ajouté relisait l'existant sans erreur, à null. Ici le format est un fichier, et
         * ce test est ce qui atteste qu'il tient.
         */
        @Test
        void leFichierEstDuJsonLisibleQuiPorteLesNomsDuModele() throws IOException {
            bibliotheque.enregistrer(animation("Salut"));

            String contenu = Files.readString(dossier.resolve("Salut.json"));

            assertTrue(contenu.contains("\"dureeTotale\""), contenu);
            assertTrue(contenu.contains("\"pistes\""), contenu);
            assertTrue(contenu.contains("\"imagesCles\""), contenu);
            assertTrue(contenu.contains("\"OEIL_GAUCHE\""), contenu);
            assertTrue(contenu.contains("\n"), "Le fichier doit rester relisible à la main");
        }

        @Test
        void leNomDuFichierFaitFoiSurCeluiEcritDedans() {
            bibliotheque.enregistrer(animation("Salut"));

            // Le fichier renommé à la main : c'est son nouveau nom qui compte
            assertTrue(dossier.resolve("Salut.json").toFile().renameTo(dossier.resolve("Coucou.json").toFile()));

            assertEquals("Coucou", bibliotheque.charger("Coucou").orElseThrow().nom());
        }

        @Test
        void enregistrerDeuxFoisEcraseSansDoublon() {
            bibliotheque.enregistrer(animation("Salut"));
            bibliotheque.enregistrer(new Animation("Salut", 9000, List.of(), List.of()));

            assertEquals(List.of("Salut"), bibliotheque.noms());
            assertEquals(9000, bibliotheque.charger("Salut").orElseThrow().dureeTotale());
        }
    }

    @Nested
    class Inventaire {

        @Test
        void lesNomsSontTriesSansTenirCompteDeLaCasse() {
            bibliotheque.enregistrer(animation("zeta"));
            bibliotheque.enregistrer(animation("Alpha"));
            bibliotheque.enregistrer(animation("beta"));

            assertEquals(List.of("Alpha", "beta", "zeta"), bibliotheque.noms());
        }

        @Test
        void unDossierAbsentDonneUneBibliothequeVideEtNonUneErreur() {
            BibliothequeDesAnimations absente = new BibliothequeDesAnimations(dossier.resolve("jamais-cree"));

            assertEquals(List.of(), absente.noms());
            assertFalse(absente.existe("Salut"));
            assertEquals(Optional.empty(), absente.charger("Salut"));
        }

        @Test
        void lesFichiersEtrangersSontIgnores() throws IOException {
            bibliotheque.enregistrer(animation("Salut"));
            Files.writeString(dossier.resolve("notes.txt"), "rien à voir");

            assertEquals(List.of("Salut"), bibliotheque.noms());
        }

        @Test
        void supprimerDitSiIlYAvaitQuelqueChose() {
            bibliotheque.enregistrer(animation("Salut"));

            assertTrue(bibliotheque.supprimer("Salut"));
            assertFalse(bibliotheque.supprimer("Salut"));
            assertEquals(List.of(), bibliotheque.noms());
        }
    }

    @Nested
    class CeQuiEstRefuse {

        /**
         * Le nom vient d'un chemin d'URL et sert à fabriquer un nom de fichier. Sans filtre, un
         * {@code ../} ferait écrire hors du dossier des animations.
         */
        @Test
        void unNomQuiRemonteDansLArborescenceEstRefuse() {
            Animation malveillante = new Animation("../../etc/passwd", 1000, List.of(), List.of());

            assertThrows(IllegalArgumentException.class, () -> bibliotheque.enregistrer(malveillante));
            assertEquals(Optional.empty(), bibliotheque.charger("../../etc/passwd"));
            assertFalse(bibliotheque.existe("../secret"));
            assertFalse(bibliotheque.supprimer("../secret"));
        }

        @Test
        void unNomVideOuDemesureEstRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> bibliotheque.enregistrer(new Animation("", 1000, List.of(), List.of())));
            assertThrows(IllegalArgumentException.class,
                    () -> bibliotheque.enregistrer(new Animation("x".repeat(65), 1000, List.of(), List.of())));
        }

        /** Un fichier abîmé ne doit pas emporter la bibliothèque entière. */
        @Test
        void unFichierCorrompuEstIgnoreSansEmpecherLesAutres() throws IOException {
            bibliotheque.enregistrer(animation("Bonne"));
            Files.writeString(dossier.resolve("Cassee.json"), "{ ceci n'est pas du JSON");

            assertEquals(Optional.empty(), bibliotheque.charger("Cassee"));
            assertTrue(bibliotheque.charger("Bonne").isPresent());
            assertEquals(List.of("Bonne", "Cassee"), bibliotheque.noms());
        }
    }
}
