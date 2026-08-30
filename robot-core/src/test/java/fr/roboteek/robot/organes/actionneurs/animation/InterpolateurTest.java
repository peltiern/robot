package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpolateurTest {

    /** Une piste volontairement irrégulière : intervalles inégaux, aller-retour, plateau. */
    private static final List<ImageCle> PISTE_DE_REFERENCE = List.of(
            new ImageCle(0, 0),
            new ImageCle(300, -12.5),
            new ImageCle(1000, 20),
            new ImageCle(1200, 20),
            new ImageCle(2500, -5.25));

    @Nested
    class MemeCourbeQueLEditeur {

        /**
         * Valeurs produites par {@code catmullRom.ts}, l'implémentation de l'éditeur, exécutée
         * telle quelle sur la piste de référence. Elles sont recopiées ici et non recalculées :
         * tout l'intérêt est de comparer deux implémentations écrites séparément. Si ce test
         * casse, le robot ne joue plus la courbe que l'éditeur affiche.
         */
        @Test
        void lesDeuxImplementationsDonnentLesMemesValeurs() {
            double[][] attendu = {
                    {-100, 0},
                    {0, 0},
                    {1, -0.021221157407},
                    {150, -8.281250000000},
                    {299, -12.532764953704},
                    {300, -12.500000000000},
                    {301, -12.485589398688},
                    {650, 2.968750000000},
                    {999, 19.976673582362},
                    {1000, 20.000000000000},
                    {1100, 23.609375000000},
                    {1200, 20.000000000000},
                    {1201, 19.990266061789},
                    {1800, 8.587505689577},
                    {2499, -5.240266061789},
                    {2500, -5.250000000000},
                    {3000, -5.250000000000},
            };
            for (double[] cas : attendu) {
                long instant = (long) cas[0];
                assertEquals(cas[1], Interpolateur.valeurA(PISTE_DE_REFERENCE, instant), 1e-9,
                        "Écart avec l'éditeur à " + instant + " ms");
            }
        }

        /**
         * Le dépassement de la spline, constaté à 1100 ms entre deux images-clés à 20°. Ce n'est
         * pas un défaut à corriger — c'est la propriété qui rend la courbe fluide — mais elle
         * décide du contrôle des butées et du bornage que le lecteur devra appliquer.
         */
        @Test
        void laCourbeDepasseLaValeurDesImagesCles() {
            double valeur = Interpolateur.valeurA(PISTE_DE_REFERENCE, 1100);
            assertTrue(valeur > 20, "La spline devrait dépasser 20°, obtenu " + valeur);
        }
    }

    @Nested
    class AuxBornes {

        @Test
        void laValeurEstTenueAvantLaPremiereEtApresLaDerniere() {
            assertEquals(0, Interpolateur.valeurA(PISTE_DE_REFERENCE, -5000));
            assertEquals(-5.25, Interpolateur.valeurA(PISTE_DE_REFERENCE, 99_000));
        }

        @Test
        void unePisteVideNeCommandeRien() {
            assertEquals(0, Interpolateur.valeurA(List.of(), 500));
        }

        @Test
        void uneSeuleImageCleTientSaValeurPartout() {
            List<ImageCle> unique = List.of(new ImageCle(500, 7.5));
            assertEquals(7.5, Interpolateur.valeurA(unique, 0));
            assertEquals(7.5, Interpolateur.valeurA(unique, 500));
            assertEquals(7.5, Interpolateur.valeurA(unique, 5000));
        }

        @Test
        void laCourbePasseExactementParChaqueImageCle() {
            for (ImageCle imageCle : PISTE_DE_REFERENCE) {
                assertEquals(imageCle.valeur(), Interpolateur.valeurA(PISTE_DE_REFERENCE, imageCle.instant()), 1e-12,
                        "La courbe rate son point à " + imageCle.instant() + " ms");
            }
        }
    }

    @Nested
    class SurUneAnimation {

        @Test
        void unAxeNonAnimeEstAbsentEtNonAZero() {
            Animation animation = new Animation("essai", 1000,
                    List.of(new Piste(Axe.OEIL_GAUCHE, 40, 60, List.of(new ImageCle(0, 0), new ImageCle(1000, 10)))),
                    List.of());

            Map<Axe, Double> positions = Interpolateur.positionsA(animation, 500);

            assertTrue(positions.containsKey(Axe.OEIL_GAUCHE));
            assertFalse(positions.containsKey(Axe.COU_GAUCHE_DROITE),
                    "Un axe que l'animation ne commande pas ne doit pas recevoir de consigne");
        }

        @Test
        void unePisteSansImageCleEstIgnoree() {
            Animation animation = new Animation("essai", 1000,
                    List.of(new Piste(Axe.OEIL_DROIT, 40, 60, List.of())), List.of());

            assertTrue(Interpolateur.positionsA(animation, 500).isEmpty());
        }
    }
}
