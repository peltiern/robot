package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.VerificateurAnimation.Avertissement;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificateurAnimationTest {

    /** Les yeux du robot : course courte, accélération faible devant la vitesse. */
    private static final LimitesMoteur YEUX = new LimitesMoteur(40, 60, -24, 20);

    private final VerificateurAnimation verificateur =
            new VerificateurAnimation(Map.of(Axe.OEIL_GAUCHE, YEUX));

    private static Animation animationAvec(List<ImageCle> imagesCles) {
        return new Animation("essai", 3000,
                List.of(new Piste(Axe.OEIL_GAUCHE, 40, 60, imagesCles)), List.of());
    }

    @Nested
    class DureeMinimale {

        /**
         * Profil triangulaire : la distance est trop courte pour atteindre la vitesse de croisière.
         * C'est le cas ordinaire des yeux — 60 °/s² pour 40 °/s, la rampe fait à elle seule 13°
         * sur une course utile de 25°.
         */
        @Test
        void surUnePetiteDistanceLeMoteurNAtteintPasSaVitesse() {
            // 4° à 40 °/s et 60 °/s² : 2 * sqrt(4 / 60) = 0,516 s
            assertEquals(0.5164, VerificateurAnimation.dureeMinimale(4, 40, 60), 1e-3);
        }

        @Test
        void surUneGrandeDistanceLeProfilEstTrapezoidal() {
            // 100° : rampe 40/60 = 0,667 s, puis 100/40 = 2,5 s
            assertEquals(3.1667, VerificateurAnimation.dureeMinimale(100, 40, 60), 1e-3);
        }

        @Test
        void unMoteurSansVitesseNArriveJamais() {
            assertEquals(Double.POSITIVE_INFINITY, VerificateurAnimation.dureeMinimale(10, 0, 60));
        }
    }

    @Nested
    class CeQuiEstSignale {

        @Test
        void unePositionHorsButeeEstSignalee() {
            List<Avertissement> avertissements = verificateur.controler(
                    animationAvec(List.of(new ImageCle(0, 0), new ImageCle(1000, -40))));

            assertTrue(avertissements.stream().anyMatch(a -> a.message().contains("hors des butées")),
                    "Une image-clé à -40° sur un axe borné à -24° doit être signalée : " + avertissements);
        }

        @Test
        void uneTransitionTropRapideEstSignalee() {
            // 30° en 100 ms : hors de portée à 40 °/s
            List<Avertissement> avertissements = verificateur.controler(
                    animationAvec(List.of(new ImageCle(0, -15), new ImageCle(100, 15))));

            assertTrue(avertissements.stream().anyMatch(a -> a.message().contains("il en faut")),
                    "Attendu un avertissement de transition : " + avertissements);
        }

        /**
         * Le cas trouvé en comparant l'interpolation à celle de l'éditeur : deux images-clés à 20°
         * précédées d'une montée font passer la courbe à 23,6°, au-delà de la butée haute, sans
         * qu'aucune image-clé ne soit fautive. Sans ce contrôle, l'éditeur laisserait écrire une
         * animation qui tape la butée en silence.
         */
        @Test
        void unDepassementDeLaCourbeEstSignaleMemeSiLesImagesClesTiennent() {
            List<ImageCle> imagesCles = List.of(
                    new ImageCle(0, 0),
                    new ImageCle(300, -12.5),
                    new ImageCle(1000, 20),
                    new ImageCle(1200, 20),
                    new ImageCle(2500, -5.25));
            imagesCles.forEach(imageCle -> assertTrue(YEUX.contient(imageCle.valeur()),
                    "Le cas de test suppose des images-clés toutes dans les butées"));

            List<Avertissement> avertissements = verificateur.controler(animationAvec(imagesCles));

            assertTrue(avertissements.stream().anyMatch(a -> a.message().contains("La courbe dépasse")),
                    "Le dépassement de la spline doit être signalé : " + avertissements);
        }
    }

    @Nested
    class CeQuiEstTu {

        @Test
        void uneAnimationJouableNeProduitAucunAvertissement() {
            List<Avertissement> avertissements = verificateur.controler(
                    animationAvec(List.of(new ImageCle(0, 0), new ImageCle(2000, 10), new ImageCle(4000, 0))));

            assertEquals(List.of(), avertissements);
        }

        @Test
        void unePositionInchangeeNeDeclencheAucunControleDeTransition() {
            List<Avertissement> avertissements = verificateur.controler(
                    animationAvec(List.of(new ImageCle(0, 5), new ImageCle(1, 5))));

            assertEquals(List.of(), avertissements, "Une transition de 0° tient dans n'importe quel délai");
        }

        /**
         * Sur un robot où l'axe n'est pas réglé, il n'y a rien à quoi comparer. Se taire vaut mieux
         * qu'un avertissement par image-clé qu'on ne saurait pas interpréter.
         */
        @Test
        void unAxeSansLimitesConnuesNEstPasControle() {
            Animation animation = new Animation("essai", 3000, List.of(
                    new Piste(Axe.COU_MONTER_DESCENDRE, 100, 200, List.of(
                            new ImageCle(0, 0), new ImageCle(10, 500)))), List.of());

            assertEquals(List.of(), verificateur.controler(animation));
        }
    }

    /**
     * Les verdicts de {@code verificateur.ts}, le portage de l'éditeur, exécuté tel quel sur les mêmes
     * pistes. Recopiés et non recalculés, comme pour l'interpolation : tout l'intérêt est de comparer
     * deux implémentations écrites séparément. Si ce test casse, l'éditeur propose de corriger ce que
     * le robot jouerait bien, ou se tait sur ce qu'il ne tiendra pas.
     */
    @Nested
    class MemesVerdictsQueLEditeur {

        @Test
        void unePositionHorsButee() {
            assertEquals(List.of("horsButee 1000-1000", "tropRapide 0-1000 1667"),
                    verdicts(List.of(new ImageCle(0, 0), new ImageCle(1000, -40))));
        }

        @Test
        void unDepassementDeLaCourbe() {
            assertEquals(List.of("depassement 1090-1090", "tropRapide 0-300 913", "tropRapide 300-1000 1479"),
                    verdicts(List.of(new ImageCle(0, 0), new ImageCle(300, -12.5), new ImageCle(1000, 20),
                            new ImageCle(1200, 20), new ImageCle(2500, -5.25))));
        }

        @Test
        void uneAnimationJouable() {
            assertEquals(List.of(), verdicts(List.of(new ImageCle(0, 0), new ImageCle(2000, 10))));
        }

        /** Nature, intervalle et, pour une transition, la durée qu'il lui faudrait — ce que l'éditeur corrige. */
        private List<String> verdicts(List<ImageCle> imagesCles) {
            return verificateur.controler(animationAvec(imagesCles)).stream()
                    .map(a -> {
                        String nature = nature(a.message());
                        String verdict = nature + " " + a.instantDebut() + "-" + a.instantFin();
                        if (!nature.equals("tropRapide")) {
                            return verdict;
                        }
                        Matcher duree = Pattern.compile("il en faut (\\d+)").matcher(a.message());
                        return duree.find() ? verdict + " " + duree.group(1) : verdict;
                    })
                    .toList();
        }

        /** Le Java ne nomme pas la nature : elle se lit au début du message. */
        private static String nature(String message) {
            if (message.startsWith("Position")) {
                return "horsButee";
            }
            if (message.startsWith("La courbe")) {
                return "depassement";
            }
            return "tropRapide";
        }
    }
}
