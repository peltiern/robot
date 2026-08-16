package fr.roboteek.robot.util.webcam;

import fr.roboteek.robot.memoire.RecognizedFace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Vérifie la logique de suivi par centroïde, indépendamment d'OpenCV/du matériel
 * (aucune dépendance webcam, modèles, ou Spring).
 */
class SuiviVisageUtilsTest {

    private static final double DISTANCE_MAX = 40;

    @Test
    void aucunVisagePrecedentRenvoieNull() {
        RecognizedFace detecte = new RecognizedFace(100, 100, 50, 50);

        assertNull(SuiviVisageUtils.trouverVisagePrecedentProche(null, detecte, DISTANCE_MAX));
        assertNull(SuiviVisageUtils.trouverVisagePrecedentProche(List.of(), detecte, DISTANCE_MAX));
    }

    @Test
    void unVisagePrecedentProcheEstTrouve() {
        VisageSuivi precedent = visageSuivi(100, 100, "id-amy", "Amy");
        // Centroïde décalé de quelques pixels seulement (petit mouvement entre 2 frames throttlées).
        RecognizedFace detecte = new RecognizedFace(105, 102, 50, 50);

        VisageSuivi trouve = SuiviVisageUtils.trouverVisagePrecedentProche(List.of(precedent), detecte, DISTANCE_MAX);

        assertSame(precedent, trouve);
    }

    @Test
    void unVisagePrecedentTropLoinNestPasTrouve() {
        VisageSuivi precedent = visageSuivi(100, 100, null, null);
        RecognizedFace detecte = new RecognizedFace(300, 300, 50, 50);

        assertNull(SuiviVisageUtils.trouverVisagePrecedentProche(List.of(precedent), detecte, DISTANCE_MAX));
    }

    @Test
    void exactementALaDistanceMaxNestPasRetenu() {
        // Centroïdes distants d'exactement 40 px (comparaison stricte : ne doit pas matcher).
        VisageSuivi precedent = visageSuivi(100, 75, null, null);
        RecognizedFace detecte = new RecognizedFace(60, 75, 50, 50);

        assertNull(SuiviVisageUtils.trouverVisagePrecedentProche(List.of(precedent), detecte, DISTANCE_MAX));
    }

    @Test
    void leVisagePrecedentLePlusProcheEstRetenuParmiPlusieurs() {
        VisageSuivi loin = visageSuivi(0, 0, "id-loin", "Loin");
        VisageSuivi proche = visageSuivi(102, 100, "id-proche", "Proche");
        RecognizedFace detecte = new RecognizedFace(100, 100, 50, 50);

        VisageSuivi trouve = SuiviVisageUtils.trouverVisagePrecedentProche(List.of(loin, proche), detecte, DISTANCE_MAX);

        assertSame(proche, trouve);
    }

    private static VisageSuivi visageSuivi(int x, int y, String idPersonne, String prenom) {
        RecognizedFace boite = new RecognizedFace(x, y, 50, 50);
        boite.setName(prenom);
        return new VisageSuivi(boite, idPersonne);
    }
}
