package fr.roboteek.robot.services.vision.face;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Vérifie la mesure de netteté sur de vraies photos, et sur les mêmes rendues floues.
 * <p>
 * Pas de modèle ONNX ici : la mesure ne dépend d'aucune reconnaissance, seulement de la boîte du
 * visage — qu'on pose à la main. Ce test tourne donc partout.
 */
class NetteteDuVisageTest {

    /**
     * Le seuil retenu à l'import ({@code ApprentissageParPhoto}). Recopié plutôt qu'importé : ce
     * test doit échouer si quelqu'un le déplace sans revenir mesurer ce que ça change.
     */
    private static final double SEUIL_RETENU = 100;

    private static final String DOSSIER = "../robot-python/vision-artificielle/known-faces/";

    @BeforeAll
    static void chargerOpenCv() {
        OpenCV.loadLocally();
    }

    /**
     * Le rapport qui justifie le seuil : une photo nette doit se distinguer très largement de la
     * même photo floutée. Si l'écart était mince, aucun seuil ne serait défendable.
     * <p>
     * Mesures obtenues le 2026-08-17 : originaux à 222, 537, 1146 et 3423 ; les mêmes franchement
     * floutés à 3, 8, 15 et 52. Le seuil est posé à 100, entre les deux paquets.
     */
    @Test
    void unePhotoFloueeSeDistingueTresNettementDeLoriginal() {
        for (String nom : new String[]{"0001_amy.jpg", "0003_penny.jpg", "0005_einstein.jpg"}) {
            Mat image = lire(nom);
            assumeTrue(image != null && !image.empty(), "photo d'exemple absente : " + nom);

            double nette = NetteteDuVisage.mesurer(image, toutLeCadre(image));
            double floue = NetteteDuVisage.mesurer(flouter(image), toutLeCadre(image));

            assertTrue(nette > floue * 4,
                    () -> nom + " : nette=" + nette + " floue=" + floue + " — écart trop faible");
            assertTrue(nette > SEUIL_RETENU,
                    () -> nom + " : une vraie photo doit passer le seuil (mesuré " + nette + ")");
            assertTrue(floue < SEUIL_RETENU,
                    () -> nom + " : une photo floue doit être recalée (mesurée " + floue + ")");
        }
    }

    /** Un visage hors cadre ne casse rien : il rend zéro, donc « pas net ». */
    @Test
    void unVisageHorsDeLImageRendZero() {
        Mat image = new Mat(100, 100, org.opencv.core.CvType.CV_8UC3);

        assertTrue(NetteteDuVisage.mesurer(image, new VisageDetecte(0, 0, 0, 0, 1f, null)) == 0);
    }

    private static Mat lire(String nom) {
        File fichier = new File(DOSSIER + nom);
        return fichier.exists() ? Imgcodecs.imread(fichier.getPath()) : null;
    }

    /**
     * Flou gaussien franc : ce que donne une photo nettement bougée ou hors mise au point.
     * <p>
     * Volontairement marqué. Un flou léger sur une photo très détaillée reste au-dessus du seuil,
     * et c'est assumé — la mesure sépare l'inexploitable de l'exploitable, pas le bon du parfait.
     */
    private static Mat flouter(Mat image) {
        Mat floue = new Mat();
        Imgproc.GaussianBlur(image, floue, new Size(25, 25), 0);
        return floue;
    }

    /** Toute l'image comme s'il s'agissait du visage : on ne juge ici que la mesure. */
    private static VisageDetecte toutLeCadre(Mat image) {
        return new VisageDetecte(0, 0, image.width(), image.height(), 0.99f, null);
    }
}
