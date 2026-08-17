package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Découpe le portrait d'une personne dans l'image où son visage vient d'être détecté.
 * <p>
 * Le seul endroit du projet où une image devient durable — et il est fait pour tenir la règle
 * « l'image ne voyage pas » : le {@code Mat} entre ici et n'en ressort qu'en JPEG, quelques
 * kilo-octets. Ni l'organe de vision ni le contrôleur REST ne manipulent d'image au-delà.
 */
public final class DecoupeDeVignette {

    /**
     * Côté de la vignette.
     * <p>
     * Passé de 192 à 384 le 2026-08-17, quand la fiche a permis d'agrandir le portrait d'un clic :
     * en 192 l'agrandissement n'était qu'un étirement, et se voyait. Le coût reste dérisoire —
     * quelques dizaines de kilo-octets par personne, soit moins qu'une seule photo de téléphone
     * pour tout le répertoire.
     * <p>
     * Sans effet sur les portraits déjà enregistrés : ils garderont leur taille jusqu'au prochain
     * apprentissage, caméra ou photo.
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
        Mat portrait = new Mat();
        try {
            Imgproc.resize(new Mat(image, cadre), portrait, new org.opencv.core.Size(COTE, COTE));
            MatOfByte tampon = new MatOfByte();
            Imgcodecs.imencode(".jpg", portrait, tampon, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, QUALITE_JPEG));
            return tampon.toArray();
        } finally {
            portrait.release();
        }
    }

}
