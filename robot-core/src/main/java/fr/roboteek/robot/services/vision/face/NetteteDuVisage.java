package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Mesure si un visage est net ou flou, par la variance du laplacien : une image nette a des
 * contours marqués, donc une forte variance ; une image floue les étale, et la variance s'effondre.
 * <p>
 * <b>Pourquoi s'en soucier</b> : une photo floue passe très bien la détection, et son empreinte
 * part en base comme les autres. Mais la reconnaissance retient la <b>meilleure</b> similarité
 * parmi toutes les empreintes connues : une empreinte bâclée peut dépasser le seuil face à
 * quelqu'un d'<b>autre</b>, et le robot appellerait alors Sandra « Nicolas ». Une mauvaise
 * empreinte est pire que pas d'empreinte du tout.
 */
public final class NetteteDuVisage {

    /**
     * Côté auquel le visage est ramené avant la mesure. La variance du laplacien dépend de la
     * résolution : sans normalisation, le même visage donnerait un score dix fois plus élevé sur
     * une photo de téléphone que sur une image de webcam.
     * <p>
     * <b>Ne pas l'aligner sur la taille de la vignette</b> : le seuil de netteté est calibré à
     * cette taille-ci, et les changer ensemble le rendrait silencieusement faux.
     */
    private static final int COTE_DE_MESURE = 192;

    private NetteteDuVisage() {
    }

    /**
     * @param image  l'image d'origine, jamais modifiée
     * @param visage le visage à jauger
     * @return la variance du laplacien sur le visage recadré ; plus c'est haut, plus c'est net.
     *         {@code 0} si le visage ne tombe pas dans l'image.
     */
    public static double mesurer(Mat image, VisageDetecte visage) {
        Rect cadre = CadrageDuVisage.carreAutourDu(visage, image.width(), image.height());
        if (cadre == null) {
            return 0;
        }
        // La sous-image partage les pixels de l'originale mais reste un objet natif à part
        // entière : sans ce release, elle n'est rendue qu'au passage du ramasse-miettes.
        Mat decoupe = new Mat(image, cadre);
        Mat ramene = new Mat();
        Mat gris = new Mat();
        Mat laplacien = new Mat();
        MatOfDouble moyenne = new MatOfDouble();
        MatOfDouble ecartType = new MatOfDouble();
        try {
            Imgproc.resize(decoupe, ramene, new Size(COTE_DE_MESURE, COTE_DE_MESURE));
            Imgproc.cvtColor(ramene, gris, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Laplacian(gris, laplacien, CvType.CV_64F);
            Core.meanStdDev(laplacien, moyenne, ecartType);
            double ecart = ecartType.get(0, 0)[0];
            return ecart * ecart;
        } finally {
            decoupe.release();
            ramene.release();
            gris.release();
            laplacien.release();
            moyenne.release();
            ecartType.release();
        }
    }
}
