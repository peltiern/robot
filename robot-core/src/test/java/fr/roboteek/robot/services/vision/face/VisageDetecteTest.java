package fr.roboteek.robot.services.vision.face;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie la mesure de profil, sur des points caractéristiques posés à la main.
 * <p>
 * Aucune image ni aucun modèle : {@code asymetrieDuNez} n'est qu'un calcul sur les quinze
 * flottants rendus par la détection. Les points sont donc placés ici, à des positions qu'on peut
 * lire — un écart d'yeux de 60 px, et un nez qu'on décale d'autant de pixels qu'on veut.
 */
class VisageDetecteTest {

    /** Le seuil par défaut ({@code robot.capteurs.vision.visage.asymetrie.maximale}). */
    private static final double SEUIL = 0.35;

    private static final double OEIL_GAUCHE_X = 100;
    private static final double OEIL_DROIT_X = 160;
    private static final double YEUX_Y = 100;
    private static final double MILIEU_DES_YEUX_X = 130;
    private static final double ECART_DES_YEUX = 60;
    private static final double NEZ_Y = 130;

    @Test
    void deFaceLeNezEstAuMilieuDesYeux() {
        assertEquals(0, visage(MILIEU_DES_YEUX_X, NEZ_Y, 0).asymetrieDuNez(), 1e-6);
    }

    /** Un nez décalé de 9 px pour 60 px d'écart d'yeux : 0,15, et ça passe la porte. */
    @Test
    void unTroisQuartsResteSousLeSeuil() {
        double asymetrie = visage(MILIEU_DES_YEUX_X + 9, NEZ_Y, 0).asymetrieDuNez();

        assertEquals(9 / ECART_DES_YEUX, asymetrie, 1e-6);
        assertTrue(asymetrie < SEUIL, "un trois-quarts doit rester reconnaissable : " + asymetrie);
    }

    /** Le décalage compte des deux côtés : se tourner à gauche ou à droite, c'est pareil. */
    @Test
    void leProfilEstRefuseDesDeuxCotes() {
        assertTrue(visage(MILIEU_DES_YEUX_X + 24, NEZ_Y, 0).asymetrieDuNez() > SEUIL, "tourné à droite");
        assertTrue(visage(MILIEU_DES_YEUX_X - 24, NEZ_Y, 0).asymetrieDuNez() > SEUIL, "tourné à gauche");
    }

    /**
     * <b>Le test qui justifie la projection</b> : une tête penchée n'est pas une tête tournée.
     * Comparer les abscisses, comme on serait tenté de le faire, ferait grimper la mesure avec le
     * seul roulis — le robot cesserait de reconnaître quelqu'un qui incline la tête, ce qui arrive
     * bien plus souvent qu'un vrai profil.
     */
    @Test
    void unVisagePencheNestPasUnProfil() {
        for (int roulis : new int[]{15, 30, 45, -30}) {
            double asymetrie = visage(MILIEU_DES_YEUX_X, NEZ_Y, roulis).asymetrieDuNez();

            assertEquals(0, asymetrie, 1e-5, "roulis de " + roulis + "° pris pour un profil");
        }
    }

    /** Et un profil reste un profil, même la tête penchée. */
    @Test
    void unProfilPencheResteUnProfil() {
        assertTrue(visage(MILIEU_DES_YEUX_X + 24, NEZ_Y, 30).asymetrieDuNez() > SEUIL);
    }

    /**
     * Sans points, on ne juge pas : la porte est là pour écarter un profil avéré, pas pour refuser
     * ce qu'elle ne sait pas juger.
     */
    @Test
    void sansPointsCaracteristiquesLeVisagePasse() {
        assertEquals(0, new VisageDetecte(0, 0, 80, 80, 0.99f, null).asymetrieDuNez());
        assertEquals(0, new VisageDetecte(0, 0, 80, 80, 0.99f, new float[4]).asymetrieDuNez());
    }

    /** Deux yeux au même endroit : aucune division par zéro, et rien de reproché au visage. */
    @Test
    void desYeuxConfondusNeFontPasExploser() {
        float[] ligne = new float[15];
        ligne[4] = 100; ligne[5] = 100;
        ligne[6] = 100; ligne[7] = 100;
        ligne[8] = 130; ligne[9] = 130;

        assertEquals(0, new VisageDetecte(0, 0, 80, 80, 0.99f, ligne).asymetrieDuNez());
    }

    /**
     * Une détection dont les points sont tournés d'un roulis donné autour du milieu des yeux —
     * exactement ce que rend YuNet sur quelqu'un qui penche la tête.
     */
    private static VisageDetecte visage(double nezX, double nezY, double roulisDegres) {
        double angle = Math.toRadians(roulisDegres);
        float[] ligne = new float[15];
        ligne[0] = 90; ligne[1] = 80; ligne[2] = 80; ligne[3] = 90;
        placer(ligne, 4, OEIL_GAUCHE_X, YEUX_Y, angle);
        placer(ligne, 6, OEIL_DROIT_X, YEUX_Y, angle);
        placer(ligne, 8, nezX, nezY, angle);
        // Les coins de la bouche ne servent qu'au recadrage : posés sous le nez, sans influence ici.
        placer(ligne, 10, nezX - 15, nezY + 25, angle);
        placer(ligne, 12, nezX + 15, nezY + 25, angle);
        ligne[14] = 0.99f;
        return new VisageDetecte(90, 80, 80, 90, 0.99f, ligne);
    }

    private static void placer(float[] ligne, int colonne, double x, double y, double angle) {
        double dx = x - MILIEU_DES_YEUX_X;
        double dy = y - YEUX_Y;
        ligne[colonne] = (float) (MILIEU_DES_YEUX_X + dx * Math.cos(angle) - dy * Math.sin(angle));
        ligne[colonne + 1] = (float) (YEUX_Y + dx * Math.sin(angle) + dy * Math.cos(angle));
    }
}
