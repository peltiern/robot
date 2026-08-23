package fr.roboteek.robot.services.vision.face;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Vérifie le portrait et la netteté, sur des images unies fabriquées ici et sur les photos
 * d'exemple du dépôt.
 * <p>
 * Aucun modèle ONNX : ni le découpage ni la mesure ne dépendent d'une reconnaissance, seulement de
 * la boîte du visage, qu'on pose à la main. Ces tests tournent donc partout, intégration continue
 * comprise.
 */
class VisageDansLImageTest {

    @BeforeAll
    static void chargerOpenCv() {
        OpenCV.loadLocally();
    }

    @Nested
    class Portrait {

        @Test
        void unVisageAuMilieuDonneUnPortraitCarre() {
            Mat image = imageUnie(640, 480);

            byte[] jpeg = VisageDansLImage.portraitJpeg(image, visage(300, 200, 80, 80));

            assertNotNull(jpeg);
            Mat relu = Imgcodecs.imdecode(new MatOfByte(jpeg), Imgcodecs.IMREAD_COLOR);
            assertEquals(relu.width(), relu.height(), "le portrait doit être carré");
            assertEquals(384, relu.width());
        }

        /**
         * Le cas qui plante si on l'oublie : le robot suit les gens du regard, un visage se
         * retrouve donc constamment au ras du cadre. Un rectangle qui déborde ferait lever une
         * exception à OpenCV, au beau milieu de la boucle vidéo.
         */
        @Test
        void unVisageContreLeBordNeFaitPasEchouerLeDecoupage() {
            Mat image = imageUnie(640, 480);

            assertNotNull(VisageDansLImage.portraitJpeg(image, visage(0, 0, 90, 90)), "coin haut gauche");
            assertNotNull(VisageDansLImage.portraitJpeg(image, visage(550, 390, 90, 90)), "coin bas droit");
            assertNotNull(VisageDansLImage.portraitJpeg(image, visage(-10, -10, 90, 90)), "boîte hors image");
        }

        /** Un visage plus grand que l'image ne doit pas donner un cadre plus grand qu'elle. */
        @Test
        void unVisageEnormeResteDansLImage() {
            assertNotNull(VisageDansLImage.portraitJpeg(imageUnie(200, 160), visage(0, 0, 400, 400)));
        }

        /**
         * Quelques dizaines de kilo-octets, pas quelques centaines : ces portraits vivent en base,
         * et la liste en charge un par personne.
         */
        @Test
        void lePortraitResteLeger() {
            byte[] jpeg = VisageDansLImage.portraitJpeg(imageUnie(640, 480), visage(300, 200, 80, 80));

            assertTrue(jpeg.length < 80_000, "vignette trop lourde : " + jpeg.length + " octets");
        }
    }

    @Nested
    class Nettete {

        /**
         * Le seuil retenu à l'import ({@code ApprentissageParPhoto}). Recopié plutôt qu'importé :
         * ce test doit échouer si quelqu'un le déplace sans revenir mesurer ce que ça change.
         */
        private static final double SEUIL_RETENU = 100;

        private static final String DOSSIER = "../robot-python/vision-artificielle/known-faces/";

        /**
         * Le rapport qui justifie le seuil : une photo nette doit se distinguer très largement de
         * la même photo floutée. Si l'écart était mince, aucun seuil ne serait défendable.
         * <p>
         * Mesures relevées : originaux à 222, 537, 1146 et 3423 ; les mêmes franchement floutés à
         * 3, 8, 15 et 52. Le seuil est posé à 100, entre les deux paquets.
         */
        @Test
        void unePhotoFloueeSeDistingueTresNettementDeLoriginal() {
            for (String nom : new String[]{"0001_amy.jpg", "0003_penny.jpg", "0005_einstein.jpg"}) {
                Mat image = lire(nom);
                assumeTrue(image != null && !image.empty(), "photo d'exemple absente : " + nom);

                double nette = VisageDansLImage.nettete(image, toutLeCadre(image));
                double floue = VisageDansLImage.nettete(flouter(image), toutLeCadre(image));

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
            Mat image = new Mat(100, 100, CvType.CV_8UC3);

            assertEquals(0, VisageDansLImage.nettete(image, visage(0, 0, 0, 0)));
        }

        private static Mat lire(String nom) {
            File fichier = new File(DOSSIER + nom);
            return fichier.exists() ? Imgcodecs.imread(fichier.getPath()) : null;
        }

        /**
         * Flou gaussien franc : ce que donne une photo nettement bougée ou hors mise au point.
         * Volontairement marqué — un flou léger sur une photo très détaillée reste au-dessus du
         * seuil, et c'est assumé : la mesure sépare l'inexploitable de l'exploitable.
         */
        private static Mat flouter(Mat image) {
            Mat floue = new Mat();
            Imgproc.GaussianBlur(image, floue, new Size(25, 25), 0);
            return floue;
        }

        /** Toute l'image comme s'il s'agissait du visage : on ne juge ici que la mesure. */
        private static VisageDetecte toutLeCadre(Mat image) {
            return visage(0, 0, image.width(), image.height());
        }
    }

    private static Mat imageUnie(int largeur, int hauteur) {
        return new Mat(hauteur, largeur, CvType.CV_8UC3, new Scalar(90, 120, 150));
    }

    private static VisageDetecte visage(int x, int y, int largeur, int hauteur) {
        return new VisageDetecte(x, y, largeur, hauteur, 0.99f, null);
    }
}
