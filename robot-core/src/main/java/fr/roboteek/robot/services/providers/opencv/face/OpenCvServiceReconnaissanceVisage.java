package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.visage.VisageConnu;
import fr.roboteek.robot.memoire.visage.VisageConnuRepository;
import fr.roboteek.robot.services.vision.face.ServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;

/**
 * Reconnaissance de visages via SFace (OpenCV), CPU pur — compare l'embedding du
 * visage détecté à ceux des visages connus (MapDB, {@link VisageConnuRepository}).
 * <p>
 * Seuil de similarité cosine : 0.363 (recommandation OpenCV Zoo pour SFace).
 */
public class OpenCvServiceReconnaissanceVisage implements ServiceReconnaissanceVisage {

    private static final String NOM_MODELE = "face_recognition_sface_2021dec.onnx";
    private static final double SEUIL_COSINE = 0.363;

    private static OpenCvServiceReconnaissanceVisage instance;

    private final FaceRecognizerSF reconnaisseur;
    private final VisageConnuRepository visageConnuRepository;

    private OpenCvServiceReconnaissanceVisage() {
        this(Constantes.DOSSIER_VISAGE + File.separator + NOM_MODELE, new VisageConnuRepository());
    }

    /** Permet d'injecter un modèle et une base isolés (utilisé par les tests). */
    public OpenCvServiceReconnaissanceVisage(String cheminModele, VisageConnuRepository visageConnuRepository) {
        reconnaisseur = FaceRecognizerSF.create(cheminModele, "");
        this.visageConnuRepository = visageConnuRepository;
    }

    public static synchronized OpenCvServiceReconnaissanceVisage getInstance() {
        if (instance == null) {
            instance = new OpenCvServiceReconnaissanceVisage();
        }
        return instance;
    }

    @Override
    public String identifier(Mat image, VisageDetecte visage) {
        Mat aligne = new Mat();
        reconnaisseur.alignCrop(image, visage.ligneBrute(), aligne);
        Mat embedding = new Mat();
        reconnaisseur.feature(aligne, embedding);

        String meilleurNom = null;
        double meilleurScore = SEUIL_COSINE;
        for (VisageConnu visageConnu : visageConnuRepository.tousLesVisages()) {
            double score = reconnaisseur.match(embedding, matDepuisEmbedding(visageConnu.embedding()), FaceRecognizerSF.FR_COSINE);
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurNom = visageConnu.nom();
            }
        }
        return meilleurNom;
    }

    private Mat matDepuisEmbedding(float[] embedding) {
        Mat mat = new Mat(1, embedding.length, CvType.CV_32F);
        mat.put(0, 0, embedding);
        return mat;
    }
}
