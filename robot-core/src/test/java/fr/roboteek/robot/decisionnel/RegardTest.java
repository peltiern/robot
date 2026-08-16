package fr.roboteek.robot.decisionnel;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import fr.roboteek.robot.util.HorlogeReglable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie le regard : conversion pixels → angle, zone morte, temporisation et choix du visage.
 * <p>
 * Les réglages sont ceux par défaut de {@code RobotConfig} (aucun {@code robot.properties} n'est
 * lisible en test, Owner retombe donc sur les {@code @DefaultValue}) : champ de 60°, zone morte
 * de 5°, une unité de cou par degré vu en panoramique et 0,3 en inclinaison, une correction
 * par seconde, sens non inversé.
 */
class RegardTest {

    private static final int LARGEUR_IMAGE = 640;
    private static final int HAUTEUR_IMAGE = 480;

    /** Écart angulaire d'un visage collé au bord droit de l'image, avec un champ de 60°. */
    private static final double ECART_BORD_DROIT = 30.0;

    /** Échelles du cou : une unité de commande vaut un degré vu en panoramique, trois en inclinaison. */
    private static final double DEGRE_VU_EN_COMMANDE = 1.0;
    private static final double DEGRE_VU_EN_COMMANDE_INCLINAISON = 0.3;

    /**
     * Écart angulaire d'un visage collé au bord bas de l'image : l'image est moins haute que
     * large, l'angle vertical est donc plus petit à la même distance du centre.
     */
    private static final double ECART_BORD_BAS = 23.4;

    private HorlogeReglable horloge;
    private List<MouvementCouEvent> mouvements;
    private Regard regard;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeReglable(Instant.parse("2026-08-12T16:00:00Z"));
        mouvements = new ArrayList<>();
        ApplicationEventPublisher publieur = evenement -> {
            if (evenement instanceof MouvementCouEvent mouvement) {
                mouvements.add(mouvement);
            }
        };
        regard = new Regard(publieur, horloge);
    }

    @Test
    void unVisageDejaEnFaceNeFaitPasBougerLaTete() {
        percevoir(visageCentreEn(LARGEUR_IMAGE / 2));

        assertTrue(mouvements.isEmpty(), "écart nul : rien à corriger");
    }

    @Test
    void unVisageAPeineDecentreResteDansLaZoneMorte() {
        // ~20 pixels sur 640 avec un champ de 60° : moins de 2 degrés.
        percevoir(visageCentreEn(LARGEUR_IMAGE / 2 + 20));

        assertTrue(mouvements.isEmpty(), "sous la zone morte de 5°");
    }

    @Test
    void leRobotTourneVersUnVisageADroite() {
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1));

        assertEquals(1, mouvements.size());
        assertEquals(ECART_BORD_DROIT * DEGRE_VU_EN_COMMANDE, mouvements.get(0).getAnglePanoramique(), 0.3,
                "tout l'écart vu, ramené en unités de cou");
        assertTrue(mouvements.get(0).getAnglePanoramique() > 0, "vers la droite de l'image");
    }

    @Test
    void leRobotTourneVersUnVisageAGauche() {
        percevoir(visageCentreEn(0));

        assertEquals(1, mouvements.size());
        assertEquals(-ECART_BORD_DROIT * DEGRE_VU_EN_COMMANDE, mouvements.get(0).getAnglePanoramique(), 0.3);
    }

    @Test
    void leRobotBaisseLaTeteVersUnVisageEnBas() {
        percevoir(visageCentreEn(LARGEUR_IMAGE / 2, HAUTEUR_IMAGE - 1));

        assertEquals(1, mouvements.size());
        assertEquals(ECART_BORD_BAS * DEGRE_VU_EN_COMMANDE_INCLINAISON, mouvements.get(0).getAngleInclinaison(), 0.3);
    }

    @Test
    void leRobotLeveLaTeteVersUnVisageEnHaut() {
        percevoir(visageCentreEn(LARGEUR_IMAGE / 2, 0));

        assertEquals(1, mouvements.size());
        assertEquals(-ECART_BORD_BAS * DEGRE_VU_EN_COMMANDE_INCLINAISON, mouvements.get(0).getAngleInclinaison(), 0.3);
    }

    @Test
    void lesDeuxAxesSontCorrigesEnUnSeulMouvement() {
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1, HAUTEUR_IMAGE - 1));

        assertEquals(1, mouvements.size(), "un seul ordre, pas un par axe");
        assertTrue(mouvements.get(0).getAnglePanoramique() > 0);
        assertTrue(mouvements.get(0).getAngleInclinaison() > 0);
    }

    @Test
    void unAxeDejaBienOrienteNEstPasCommande() {
        // Décalé horizontalement, mais à la bonne hauteur.
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1, HAUTEUR_IMAGE / 2));

        MouvementCouEvent mouvement = mouvements.get(0);
        assertTrue(mouvement.getAnglePanoramique() > 0);
        assertEquals(MouvementCouEvent.ANGLE_NEUTRE, mouvement.getAngleInclinaison(),
                "l'inclinaison est dans la zone morte : aucune consigne");
    }

    @Test
    void laPostureDuCouNEstJamaisTouchee() {
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1, HAUTEUR_IMAGE - 1));

        MouvementCouEvent mouvement = mouvements.get(0);
        assertEquals(MouvementCouEvent.POSITION_NEUTRE, mouvement.getPositionPanoramique(),
                "rotation relative, pas de position absolue");
        assertEquals(MouvementCouEvent.ANGLE_NEUTRE, mouvement.getAngleMonterDescendre(),
                "monter / descendre change la posture, pas la direction du regard");
    }

    @Test
    void lesCorrectionsSontEspaceesLeTempsQueLeServoArrive() {
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1));
        horloge.avancerDe(Duration.ofMillis(300));
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1));

        assertEquals(1, mouvements.size(), "la seconde perception tombe pendant la temporisation");
    }

    @Test
    void uneNouvelleCorrectionEstPossibleUneFoisLeDelaiEcoule() {
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1));
        horloge.avancerDe(Duration.ofMillis(1100));
        percevoir(visageCentreEn(LARGEUR_IMAGE - 1));

        assertEquals(2, mouvements.size());
    }

    @Test
    void leRobotRegardeLeVisageLePlusProche() {
        // Le petit visage est à gauche, le gros à droite : c'est vers la droite qu'on tourne.
        VisagePercu loin = new VisagePercu(null, null, 0, 200, 40, 40);
        VisagePercu proche = new VisagePercu(null, null, LARGEUR_IMAGE - 160, 100, 160, 160);

        percevoir(loin, proche);

        assertEquals(1, mouvements.size());
        assertTrue(mouvements.get(0).getAnglePanoramique() > 0, "vers le visage le plus gros");
    }

    @Test
    void unChampVideNeDeclencheRien() {
        percevoir();

        assertTrue(mouvements.isEmpty());
    }

    @Test
    void uneImageSansDimensionEstIgnoree() {
        // Évènement mal formé (dimensions à zéro) : diviser par la largeur donnerait un NaN.
        regard.handleVisagePercuEvent(new VisagePercuEvent(List.of(visageCentreEn(100)), 0, 0));

        assertTrue(mouvements.isEmpty());
    }

    private void percevoir(VisagePercu... visages) {
        regard.handleVisagePercuEvent(new VisagePercuEvent(List.of(visages), LARGEUR_IMAGE, HAUTEUR_IMAGE));
    }

    /** Visage de 100 px de côté, centré verticalement, dont le centre tombe sur l'abscisse donnée. */
    private static VisagePercu visageCentreEn(int centreX) {
        return visageCentreEn(centreX, HAUTEUR_IMAGE / 2);
    }

    /** Visage de 100 px de côté dont le centre tombe sur le point donné. */
    private static VisagePercu visageCentreEn(int centreX, int centreY) {
        return new VisagePercu(null, null, centreX - 50, centreY - 50, 100, 100);
    }
}
