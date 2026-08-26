package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Ce qu'on tire d'un visage détecté dans l'image où il se trouve : son portrait, sa netteté, et
 * s'il vaut la peine d'être appris.
 * <p>
 * Les deux ensemble, parce qu'ils doivent regarder <b>exactement la même zone</b> : jugés sur des
 * cadrages différents, on mesurerait la qualité d'une image et on en enregistrerait une autre. Le
 * cadrage est donc privé, et il n'y a aucun moyen de les désaccorder.
 * <p>
 * C'est aussi le seul endroit du projet où une image devient durable : le {@code Mat} entre ici et
 * n'en ressort qu'en JPEG, quelques kilo-octets.
 */
public final class VisageDansLImage {

    /**
     * Marge autour de la boîte du visage, en proportion de sa taille. La détection cadre au plus
     * près des sourcils et du menton : sans marge, le portrait est un gros plan sur des yeux et
     * une bouche, où l'on ne reconnaît personne.
     */
    private static final double MARGE = 0.33;

    /**
     * Côté du portrait, assez grand pour que la fiche puisse l'agrandir sans que ça se voie.
     * <p>
     * Le changer n'affecte que les portraits à venir : les autres gardent leur taille jusqu'au
     * prochain apprentissage.
     */
    private static final int COTE_PORTRAIT = 384;

    private static final int QUALITE_JPEG = 82;

    /**
     * Côté auquel le visage est ramené avant la mesure de netteté. La variance du laplacien dépend
     * de la résolution : sans normalisation, le même visage donnerait un score dix fois plus élevé
     * sur une photo de téléphone que sur une image de webcam.
     * <p>
     * <b>Ne pas l'aligner sur {@link #COTE_PORTRAIT}</b> : le seuil de netteté est calibré à cette
     * taille-ci, et les changer ensemble le rendrait silencieusement faux.
     */
    private static final int COTE_DE_MESURE = 192;

    private VisageDansLImage() {
    }

    /**
     * Ce visage-là peut-il donner une empreinte qu'on gardera ?
     * <p>
     * <b>Une seule porte pour les deux chemins d'apprentissage</b> — la photo importée et la
     * caméra. Elles se sont longtemps ignorées : la photo refusait déjà le flou, la caméra
     * acceptait toute prise et en faisait cinq en moins de deux secondes, cinq quasi-copies de la
     * même pose. Une mauvaise prise donnait donc cinq mauvaises empreintes, et une empreinte
     * bâclée ne gêne pas que la personne concernée : la reconnaissance retient la meilleure
     * similarité parmi toutes les empreintes connues, et celle-là peut dépasser le seuil face à
     * quelqu'un d'<b>autre</b>. Mieux vaut pas d'empreinte du tout.
     * <p>
     * Les deux questions dans cet ordre, du moins cher au plus cher : l'asymétrie n'est qu'un
     * calcul sur les points déjà rendus par la détection, la netteté repasse sur l'image, et
     * l'empreinte qui suivra coûte ~68 ms.
     */
    public static Qualite qualite(Mat image, VisageDetecte visage) {
        if (visage.asymetrieDuNez() > robotConfig().asymetrieMaximaleDuNez()) {
            return Qualite.TROP_DE_PROFIL;
        }
        if (nettete(image, visage) < robotConfig().netteteMinimaleDuVisage()) {
            return Qualite.TROP_FLOU;
        }
        return Qualite.EXPLOITABLE;
    }

    /** Verdict de {@link #qualite}, à charge de l'appelant de le dire comme il l'entend. */
    public enum Qualite {
        EXPLOITABLE, TROP_FLOU, TROP_DE_PROFIL;

        public boolean exploitable() {
            return this == EXPLOITABLE;
        }
    }

    /**
     * Le portrait du visage, recadré et encodé.
     *
     * @param image  l'image d'origine, jamais modifiée
     * @param visage le visage à recadrer
     * @return le portrait en JPEG, ou {@code null} si le recadrage ne tombe pas dans l'image
     */
    public static byte[] portraitJpeg(Mat image, VisageDetecte visage) {
        Rect cadre = carreAutourDu(visage, image.width(), image.height());
        if (cadre == null) {
            return null;
        }
        // La sous-image partage les pixels de l'originale mais reste un objet natif à part
        // entière : sans ce release, elle n'est rendue qu'au passage du ramasse-miettes.
        Mat decoupe = new Mat(image, cadre);
        Mat portrait = new Mat();
        MatOfByte tampon = new MatOfByte();
        try {
            Imgproc.resize(decoupe, portrait, new Size(COTE_PORTRAIT, COTE_PORTRAIT));
            Imgcodecs.imencode(".jpg", portrait, tampon, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, QUALITE_JPEG));
            return tampon.toArray();
        } finally {
            decoupe.release();
            portrait.release();
            tampon.release();
        }
    }

    /**
     * Le visage est-il net ou flou ? Mesuré par la variance du laplacien : une image nette a des
     * contours marqués, donc une forte variance ; une image floue les étale, et la variance
     * s'effondre.
     * <p>
     * <b>Pourquoi s'en soucier</b> : une photo floue passe très bien la détection, et son empreinte
     * part en base comme les autres. Mais la reconnaissance retient la <b>meilleure</b> similarité
     * parmi toutes les empreintes connues : une empreinte bâclée peut dépasser le seuil face à
     * quelqu'un d'<b>autre</b>, et le robot appellerait alors Sandra « Nicolas ». Une mauvaise
     * empreinte est pire que pas d'empreinte du tout.
     *
     * @param image  l'image d'origine, jamais modifiée
     * @param visage le visage à jauger
     * @return la variance du laplacien ; plus c'est haut, plus c'est net. {@code 0} si le visage
     *         ne tombe pas dans l'image.
     */
    public static double nettete(Mat image, VisageDetecte visage) {
        Rect cadre = carreAutourDu(visage, image.width(), image.height());
        if (cadre == null) {
            return 0;
        }
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

    /**
     * Carré centré sur le visage, marge comprise, ramené dans les bords de l'image.
     * <p>
     * Carré, parce qu'une vignette carrée à l'écran déformerait un portrait rectangulaire. Ramené
     * dans les bords, parce qu'un visage au ras du cadre — ce qui arrive tout le temps, le robot
     * suit les gens du regard — déborderait, et OpenCV lèverait en pleine boucle vidéo.
     *
     * @return {@code null} si le visage est trop près du bord pour qu'il reste quelque chose
     */
    private static Rect carreAutourDu(VisageDetecte visage, int largeurImage, int hauteurImage) {
        int cote = (int) Math.round(Math.max(visage.width(), visage.height()) * (1 + 2 * MARGE));
        cote = Math.min(cote, Math.min(largeurImage, hauteurImage));
        if (cote <= 0) {
            return null;
        }
        int centreX = visage.x() + visage.width() / 2;
        int centreY = visage.y() + visage.height() / 2;
        int x = borner(centreX - cote / 2, 0, largeurImage - cote);
        int y = borner(centreY - cote / 2, 0, hauteurImage - cote);
        return new Rect(x, y, cote, cote);
    }

    private static int borner(int valeur, int minimum, int maximum) {
        return Math.max(minimum, Math.min(valeur, maximum));
    }
}
