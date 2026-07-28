package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.services.vision.face.ServiceDetectionVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Détection de visages via YuNet (OpenCV), CPU pur — modèle ONNX chargé depuis
 * {@code ${ROBOT_HOME}/visage}. Latence mesurée sur Jetson Nano 4 Go : voir
 * {@code fr.roboteek.robot.poc.FaceRecognitionPoc} (~60-150 ms/frame).
 */
public class OpenCvServiceDetectionVisage implements ServiceDetectionVisage {

    private static final String NOM_MODELE = "face_detection_yunet_2023mar.onnx";
    private static final float SEUIL_SCORE = 0.8f;
    private static final float SEUIL_NMS = 0.3f;
    private static final int TOP_K = 5000;
    /** Nombre de colonnes d'une ligne de détection YuNet : bbox(4) + 5 points(10) + score(1). */
    private static final int NB_COLONNES_DETECTION = 15;

    private static OpenCvServiceDetectionVisage instance;

    private final FaceDetectorYN detecteur;
    private Size tailleCourante;

    private OpenCvServiceDetectionVisage() {
        String cheminModele = Constantes.DOSSIER_VISAGE + File.separator + NOM_MODELE;
        tailleCourante = new Size(1, 1);
        detecteur = FaceDetectorYN.create(cheminModele, "", tailleCourante, SEUIL_SCORE, SEUIL_NMS, TOP_K);
    }

    public static synchronized OpenCvServiceDetectionVisage getInstance() {
        if (instance == null) {
            instance = new OpenCvServiceDetectionVisage();
        }
        return instance;
    }

    @Override
    public List<VisageDetecte> detecter(Mat image) {
        Size taille = new Size(image.cols(), image.rows());
        if (!taille.equals(tailleCourante)) {
            detecteur.setInputSize(taille);
            tailleCourante = taille;
        }

        Mat visages = new Mat();
        detecteur.detect(image, visages);

        List<VisageDetecte> resultat = new ArrayList<>();
        for (int i = 0; i < visages.rows(); i++) {
            Mat ligne = visages.row(i);
            float[] donnees = new float[NB_COLONNES_DETECTION];
            ligne.get(0, 0, donnees);
            resultat.add(new VisageDetecte((int) donnees[0], (int) donnees[1], (int) donnees[2], (int) donnees[3], donnees[14], ligne.clone()));
        }
        return resultat;
    }
}
