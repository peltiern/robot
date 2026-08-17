package fr.roboteek.robot.services.vision.face;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie le recadrage des portraits.
 * <p>
 * Sur des images unies fabriquées ici, sans modèle ONNX ni photo : le découpage est de la
 * géométrie, il n'a besoin d'aucune reconnaissance. Ce test tourne donc partout, y compris en
 * intégration continue.
 */
class DecoupeDeVignetteTest {

    @BeforeAll
    static void chargerOpenCv() {
        OpenCV.loadLocally();
    }

    @Test
    void unVisageAuMilieuDonneUnPortraitCarre() {
        Mat image = image(640, 480);

        byte[] jpeg = DecoupeDeVignette.enJpeg(image, visage(300, 200, 80, 80));

        assertNotNull(jpeg);
        Mat relu = Imgcodecs.imdecode(new MatOfByte(jpeg), Imgcodecs.IMREAD_COLOR);
        assertEquals(relu.width(), relu.height(), "le portrait doit être carré");
        // 384 et non 192 depuis que la fiche permet d'agrandir le portrait d'un clic : en 192
        // l'agrandissement n'était qu'un étirement, et ça se voyait.
        assertEquals(384, relu.width());
    }

    /**
     * Le cas qui plante si on l'oublie : le robot suit les gens du regard, un visage se retrouve
     * donc constamment au ras du cadre. Un rectangle qui déborde ferait lever une exception à
     * OpenCV, au beau milieu de la boucle vidéo.
     */
    @Test
    void unVisageContreLeBordNeFaitPasEchouerLeDecoupage() {
        Mat image = image(640, 480);

        assertNotNull(DecoupeDeVignette.enJpeg(image, visage(0, 0, 90, 90)), "coin haut gauche");
        assertNotNull(DecoupeDeVignette.enJpeg(image, visage(550, 390, 90, 90)), "coin bas droit");
        assertNotNull(DecoupeDeVignette.enJpeg(image, visage(-10, -10, 90, 90)), "boîte qui sort de l'image");
    }

    /** Un visage plus grand que l'image ne doit pas donner un cadre plus grand qu'elle. */
    @Test
    void unVisageEnormeResteDansLImage() {
        Mat image = image(200, 160);

        assertNotNull(DecoupeDeVignette.enJpeg(image, visage(0, 0, 400, 400)));
    }

    /**
     * Quelques dizaines de kilo-octets, pas quelques centaines : ces portraits vivent en base, et
     * la liste en charge un par personne.
     */
    @Test
    void lePortraitResteLeger() {
        byte[] jpeg = DecoupeDeVignette.enJpeg(image(640, 480), visage(300, 200, 80, 80));

        assertTrue(jpeg.length < 80_000, "vignette trop lourde : " + jpeg.length + " octets");
    }

    private static Mat image(int largeur, int hauteur) {
        return new Mat(hauteur, largeur, CvType.CV_8UC3, new Scalar(90, 120, 150));
    }

    private static VisageDetecte visage(int x, int y, int largeur, int hauteur) {
        return new VisageDetecte(x, y, largeur, hauteur, 0.99f, null);
    }
}
