package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.OrigineMouvement;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.util.HorlogeReglable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le lecteur se vérifie sans robot : l'échantillonnage est une fonction du temps, et la
 * publication passe par le bus. Aucun Phidget n'est touché.
 */
class LecteurAnimationTest {

    private static final double VITESSE = 40;

    private static final double ACCELERATION = 200;

    private final HorlogeReglable horloge = new HorlogeReglable(Instant.parse("2026-08-30T10:00:00Z"));

    /** Butées du vrai robot, converties en relatif (voir {@link LimitesMoteur}). */
    private static Map<Axe, LimitesMoteur> limitesDuRobot() {
        return Map.of(
                Axe.COU_GAUCHE_DROITE, new LimitesMoteur(40, 200, -60, 60),
                Axe.COU_HAUT_BAS, new LimitesMoteur(10, 200, -8, 7),
                Axe.OEIL_GAUCHE, new LimitesMoteur(40, 60, -5, 20),
                Axe.OEIL_DROIT, new LimitesMoteur(40, 60, -5, 20));
    }

    private static Piste piste(Axe axe, ImageCle... imagesCles) {
        return new Piste(axe, VITESSE, ACCELERATION, List.of(imagesCles));
    }

    private LecteurAnimation lecteurCable() {
        LecteurAnimation lecteur = new LecteurAnimation(horloge);
        lecteur.limites(limitesDuRobot());
        return lecteur;
    }

    @Nested
    class Echantillonnage {

        /**
         * Le cœur du lecteur : entre deux images-clés, la position vient de la spline, pas d'une
         * interpolation linéaire — c'est ce qui fait que le robot exécute la courbe de l'éditeur.
         */
        @Test
        void laPositionSuitLaCourbeEntreDeuxImagesCles() {
            Animation animation = new Animation("Salut", 2000, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(1000, 30),
                            new ImageCle(2000, 0))), List.of());
            LecteurAnimation lecteur = lecteurCable();
            LecteurAnimation.Lecture lecture = new LecteurAnimation.Lecture(animation, 0);

            assertEquals(0, lecteur.consignes(lecture, 0).get(Axe.COU_GAUCHE_DROITE), 0.001);
            assertEquals(30, lecteur.consignes(lecture, 1000).get(Axe.COU_GAUCHE_DROITE), 0.001);
            // À mi-chemin, la spline est au-dessus de la corde (qui vaudrait 15)
            assertTrue(lecteur.consignes(lecture, 500).get(Axe.COU_GAUCHE_DROITE) > 16,
                    "La spline doit dépasser l'interpolation linéaire");
        }

        /** Un axe que l'animation ne commande pas ne doit jamais être touché, même pour le remettre à zéro. */
        @Test
        void lesAxesSansPisteNeSontPasCommandes() {
            Animation animation = new Animation("Clin d'oeil", 1000, List.of(
                    piste(Axe.OEIL_GAUCHE, new ImageCle(0, 0), new ImageCle(500, -5))), List.of());
            LecteurAnimation lecteur = lecteurCable();

            Map<Axe, Double> consignes = lecteur.consignes(new LecteurAnimation.Lecture(animation, 0), 250);

            assertEquals(List.of(Axe.OEIL_GAUCHE), List.copyOf(consignes.keySet()));
        }

        /**
         * La spline dépasse la valeur des images-clés qu'elle traverse : deux points à 20°
         * suffisent à la faire passer par 23,6°. Sans écrêtage, une animation dont toutes les
         * images-clés tiennent dans les butées irait taper la mécanique en chemin.
         */
        @Test
        void laCourbeEstEcreteeAuxButeesMemeQuandLesImagesClesTiennent() {
            Animation animation = new Animation("Dépassement", 3000, List.of(
                    piste(Axe.OEIL_GAUCHE, new ImageCle(0, 0), new ImageCle(1000, 20),
                            new ImageCle(2000, 20), new ImageCle(3000, 0))), List.of());
            LecteurAnimation lecteur = lecteurCable();
            LecteurAnimation.Lecture lecture = new LecteurAnimation.Lecture(animation, 0);

            for (long instant = 0; instant <= 3000; instant += 10) {
                Double position = lecteur.consignes(lecture, instant).get(Axe.OEIL_GAUCHE);
                if (position != null) {
                    assertTrue(position <= 20, "Position hors butée à " + instant + " ms : " + position);
                }
            }
        }
    }

    @Nested
    class Frugalite {

        /**
         * Le levier qui rend l'échelle tenable : le contrôleur ne draine qu'une soixantaine
         * d'écritures par seconde, et un canal non écrit ne coûte rien. Un axe arrêté sur un
         * palier ne doit donc plus rien consommer.
         */
        @Test
        void unAxeQuiNeBougePlusNEstPlusReecrit() {
            Animation animation = new Animation("Palier", 2000, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 10), new ImageCle(2000, 10))), List.of());
            LecteurAnimation lecteur = lecteurCable();
            LecteurAnimation.Lecture lecture = new LecteurAnimation.Lecture(animation, 0);

            assertEquals(Map.of(Axe.COU_GAUCHE_DROITE, 10.0), lecteur.consignes(lecture, 0));
            assertEquals(Map.of(), lecteur.consignes(lecture, 100), "Rien n'a bougé : rien à écrire");
            assertEquals(Map.of(), lecteur.consignes(lecture, 1000));
        }

        /** Le seuil se compte depuis la dernière consigne envoyée, pas depuis le tour précédent. */
        @Test
        void lesPetitsDeplacementsSCumulentJusquAFranchirLeSeuil() {
            // 1° sur 10 s : 0,01° par tour à 10 Hz, loin sous le seuil de 0,3
            Animation animation = new Animation("Lent", 10000, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(10000, 1))), List.of());
            LecteurAnimation lecteur = lecteurCable();
            LecteurAnimation.Lecture lecture = new LecteurAnimation.Lecture(animation, 0);

            int ecritures = 0;
            for (long instant = 0; instant <= 10000; instant += 100) {
                ecritures += lecteur.consignes(lecture, instant).size();
            }

            assertTrue(ecritures > 1, "Un mouvement lent doit tout de même finir par être écrit");
            assertTrue(ecritures < 20, "Un mouvement d'un degré ne doit pas coûter cent écritures, mais " + ecritures);
        }
    }

    @Nested
    class SurLeBus {

        private AnnotationConfigApplicationContext contexte;

        @BeforeEach
        void setUp() {
            contexte = new AnnotationConfigApplicationContext(
                    RobotEventsConfig.class, LecteurAnimation.class, MouvementsRecus.class);
        }

        @AfterEach
        void tearDown() {
            contexte.close();
        }

        @Test
        void uneAnimationPublieLeCouEtLesYeuxSeparement() throws InterruptedException {
            LecteurAnimation lecteur = contexte.getBean(LecteurAnimation.class);
            assertTrue(lecteur.isRunning(), "Le contexte Spring doit avoir démarré le lecteur");
            lecteur.limites(limitesDuRobot());

            lecteur.jouer(new Animation("Salut", 1500, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(1500, 25)),
                    piste(Axe.OEIL_GAUCHE, new ImageCle(0, 0), new ImageCle(1500, -5))), List.of()));

            MouvementsRecus recus = contexte.getBean(MouvementsRecus.class);
            long limite = System.currentTimeMillis() + 5000;
            while ((recus.cou.isEmpty() || recus.yeux.isEmpty()) && System.currentTimeMillis() < limite) {
                Thread.sleep(20);
            }

            assertFalse(recus.cou.isEmpty(), "Aucun mouvement de cou publié");
            assertFalse(recus.yeux.isEmpty(), "Aucun mouvement d'yeux publié");
            MouvementCouEvent cou = recus.cou.getFirst();
            assertEquals(VITESSE, cou.getVitessePanoramique());
            assertEquals(MouvementCouEvent.POSITION_NEUTRE, cou.getPositionInclinaison(),
                    "Un axe non animé doit rester neutre, sinon le cou le déplacerait");
        }

        /**
         * La manette l'emporte, et elle interrompt : reprendre l'animation là où elle en était
         * partirait d'une posture que la manette a changée entre-temps, et le reste du geste
         * n'aurait plus de sens.
         */
        @Test
        void unOrdreDeManetteInterromptLAnimation() {
            LecteurAnimation lecteur = contexte.getBean(LecteurAnimation.class);
            lecteur.limites(limitesDuRobot());
            lecteur.jouer(new Animation("Longue", 60000, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(60000, 40))), List.of()));

            MouvementCouEvent ordre = new MouvementCouEvent();
            ordre.setOrigine(OrigineMouvement.MANETTE);
            contexte.publishEvent(ordre);

            assertFalse(lecteur.enLecture());
        }

        /** Le lecteur publie sur le même bus : ses propres ordres ne doivent pas l'interrompre. */
        @Test
        void lesOrdresDuLecteurNeLInterrompentPas() {
            LecteurAnimation lecteur = contexte.getBean(LecteurAnimation.class);
            lecteur.limites(limitesDuRobot());
            lecteur.jouer(new Animation("Longue", 60000, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(60000, 40))), List.of()));

            MouvementCouEvent sien = new MouvementCouEvent();
            sien.setOrigine(OrigineMouvement.ANIMATION);
            contexte.publishEvent(sien);

            assertTrue(lecteur.enLecture());
        }

        /** Une animation arrêtée en urgence est perdue : la reprendre après réarmement ferait repartir la tête seule. */
        @Test
        void lArretDUrgenceAbandonneLAnimation() {
            LecteurAnimation lecteur = contexte.getBean(LecteurAnimation.class);
            lecteur.limites(limitesDuRobot());
            lecteur.jouer(new Animation("Longue", 60000, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(60000, 40))), List.of()));
            assertTrue(lecteur.enLecture());

            contexte.publishEvent(new ArretUrgenceEvent(true, "test"));

            assertFalse(lecteur.enLecture(), "L'animation doit être abandonnée, pas mise en pause");
        }
    }

    @Test
    void lAnimationSArreteQuandSaDureeEstEcoulee() throws InterruptedException {
        AnnotationConfigApplicationContext contexte = new AnnotationConfigApplicationContext(
                RobotEventsConfig.class, LecteurAnimation.class, MouvementsRecus.class);
        try {
            LecteurAnimation lecteur = contexte.getBean(LecteurAnimation.class);
            lecteur.limites(limitesDuRobot());
            lecteur.jouer(new Animation("Brève", 150, List.of(
                    piste(Axe.COU_GAUCHE_DROITE, new ImageCle(0, 0), new ImageCle(150, 10))), List.of()));

            long limite = System.currentTimeMillis() + 5000;
            while (lecteur.enLecture() && System.currentTimeMillis() < limite) {
                Thread.sleep(20);
            }
            assertFalse(lecteur.enLecture(), "Le lecteur doit se libérer à la fin de l'animation");
        } finally {
            contexte.close();
        }
    }

    @Component
    static class MouvementsRecus {

        final List<MouvementCouEvent> cou = new CopyOnWriteArrayList<>();

        final List<MouvementYeuxEvent> yeux = new CopyOnWriteArrayList<>();

        @EventListener
        public void onCou(MouvementCouEvent evenement) {
            cou.add(evenement);
        }

        @EventListener
        public void onYeux(MouvementYeuxEvent evenement) {
            yeux.add(evenement);
        }
    }
}
