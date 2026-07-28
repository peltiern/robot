package fr.roboteek.robot.poc;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

/**
 * PoC autonome (hors Spring) : valide que YuNet (détection) + SFace (reconnaissance) tournent
 * en CPU pur sur le Jetson Nano 4 Go, avant toute intégration dans {@code CapteurVisionWebSocketGrpc}.
 * <p>
 * Usage : {@code java -jar robot-core-poc.jar <yunet.onnx> <sface.onnx> <image1.jpg> [image2.jpg]}
 * (nécessite de pointer temporairement {@code mainClass} vers cette classe dans le pom, voir
 * la configuration commentée existante du plugin {@code spring-boot-maven-plugin}).
 * <p>
 * Si une seconde image est fournie, compare les deux visages détectés (utile pour vérifier
 * qu'une même personne obtient un score de similarité élevé sur deux photos différentes).
 */
public class FaceRecognitionPoc {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: FaceRecognitionPoc <yunet.onnx> <sface.onnx> <image1.jpg> [image2.jpg]");
            System.exit(1);
        }
        String yunetPath = args[0];
        String sfacePath = args[1];
        String image1Path = args[2];
        String image2Path = args.length > 3 ? args[3] : null;

        OpenCV.loadLocally();

        Mat image1 = Imgcodecs.imread(image1Path);
        if (image1.empty()) {
            System.err.println("Image illisible : " + image1Path);
            System.exit(1);
        }

        long tCreationDebut = System.currentTimeMillis();
        FaceDetectorYN detecteur = FaceDetectorYN.create(yunetPath, "", new Size(image1.cols(), image1.rows()), 0.8f, 0.3f, 5000);
        FaceRecognizerSF reconnaisseur = FaceRecognizerSF.create(sfacePath, "");
        System.out.println("Chargement des modèles : " + (System.currentTimeMillis() - tCreationDebut) + " ms");

        Mat feature1 = detecterEtExtraire(detecteur, reconnaisseur, image1, "image1");

        if (image2Path != null && feature1 != null) {
            Mat image2 = Imgcodecs.imread(image2Path);
            if (image2.empty()) {
                System.err.println("Image illisible : " + image2Path);
                return;
            }
            detecteur.setInputSize(new Size(image2.cols(), image2.rows()));
            Mat feature2 = detecterEtExtraire(detecteur, reconnaisseur, image2, "image2");
            if (feature2 != null) {
                double scoreCosine = reconnaisseur.match(feature1, feature2, FaceRecognizerSF.FR_COSINE);
                double distanceL2 = reconnaisseur.match(feature1, feature2, FaceRecognizerSF.FR_NORM_L2);
                System.out.println("Similarité cosine (seuil OpenCV Zoo : > 0.363 = même personne) : " + scoreCosine);
                System.out.println("Distance L2 (seuil OpenCV Zoo : < 1.128 = même personne) : " + distanceL2);
            }
        }
    }

    private static Mat detecterEtExtraire(FaceDetectorYN detecteur, FaceRecognizerSF reconnaisseur, Mat image, String etiquette) {
        Mat visages = new Mat();
        long tDetectionDebut = System.currentTimeMillis();
        detecteur.detect(image, visages);
        long detectionMs = System.currentTimeMillis() - tDetectionDebut;
        System.out.println(etiquette + " — détection (" + detectionMs + " ms) : " + visages.rows() + " visage(s)");
        if (visages.rows() == 0) {
            return null;
        }

        Mat aligne = new Mat();
        reconnaisseur.alignCrop(image, visages.row(0), aligne);
        Mat feature = new Mat();
        long tExtractionDebut = System.currentTimeMillis();
        reconnaisseur.feature(aligne, feature);
        long extractionMs = System.currentTimeMillis() - tExtractionDebut;
        System.out.println(etiquette + " — extraction embedding (" + extractionMs + " ms)");
        return feature.clone();
    }
}
