package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Découpe le portrait d'une personne dans l'image où son visage vient d'être détecté.
 * <p>
 * Le seul endroit du projet où une image devient durable : le {@code Mat} entre ici et n'en
 * ressort qu'en JPEG, quelques kilo-octets.
 */
public final class DecoupeDeVignette {

    /**
     * Côté de la vignette, assez grand pour que la fiche puisse l'agrandir sans que ça se voie.
     * <p>
     * Le changer n'affecte que les portraits à venir : les autres gardent leur taille jusqu'au
     * prochain apprentissage.
     */
    private static final int COTE = 384;

    private static final int QUALITE_JPEG = 82;

    private DecoupeDeVignette() {
    }

    /**
     * @param image  l'image d'origine, jamais modifiée
     * @param visage le visage à recadrer
     * @return le portrait en JPEG, ou {@code null} si le recadrage ne tombe pas dans l'image
     */
    public static byte[] enJpeg(Mat image, VisageDetecte visage) {
        Rect cadre = CadrageDuVisage.carreAutourDu(visage, image.width(), image.height());
        if (cadre == null) {
            return null;
        }
        // La sous-image partage les pixels de l'originale mais reste un objet natif à part
        // entière : sans ce release, elle n'est rendue qu'au passage du ramasse-miettes.
        Mat decoupe = new Mat(image, cadre);
        Mat portrait = new Mat();
        MatOfByte tampon = new MatOfByte();
        try {
            Imgproc.resize(decoupe, portrait, new Size(COTE, COTE));
            Imgcodecs.imencode(".jpg", portrait, tampon, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, QUALITE_JPEG));
            return tampon.toArray();
        } finally {
            decoupe.release();
            portrait.release();
            tampon.release();
        }
    }

}
