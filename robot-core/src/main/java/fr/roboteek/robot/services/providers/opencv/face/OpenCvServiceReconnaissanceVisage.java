package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnu;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import fr.roboteek.robot.services.vision.face.ServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;
import java.util.List;

/**
 * Reconnaissance de visages via SFace (OpenCV), CPU pur — compare l'embedding du
 * visage détecté à ceux des visages connus ({@link VisageConnuRepository}).
 * <p>
 * Seuil de similarité cosine : 0.363 (recommandation OpenCV Zoo pour SFace).
 * <p>
 * Ce n'est délibérément pas un composant Spring : le modèle ONNX peut manquer sur une
 * installation neuve, et l'échec ferait alors tomber le contexte entier au lieu de priver le
 * robot de la seule reconnaissance. L'organe de vision le charge lui-même, et se contente d'un
 * avertissement s'il n'y arrive pas.
 * <p>
 * <b>Instance unique, et les méthodes qui touchent au reconnaisseur sont donc synchronisées</b>,
 * pour la même raison que {@link OpenCvServiceDetectionVisage} : la boucle vidéo n'est plus seule
 * à s'en servir depuis que l'interface web importe des photos, et les objets dnn d'OpenCV ne
 * supportent pas deux threads à la fois.
 */
public class OpenCvServiceReconnaissanceVisage implements ServiceReconnaissanceVisage {

    private static final String NOM_MODELE = "face_recognition_sface_2021dec.onnx";
    private static final double SEUIL_COSINE = 0.363;

    private static OpenCvServiceReconnaissanceVisage instance;

    private final FaceRecognizerSF reconnaisseur;
    private final VisageConnuRepository visageConnuRepository;

    /** Permet d'injecter un modèle et une base isolés (utilisé par les tests). */
    public OpenCvServiceReconnaissanceVisage(String cheminModele, VisageConnuRepository visageConnuRepository) {
        reconnaisseur = FaceRecognizerSF.create(cheminModele, "");
        this.visageConnuRepository = visageConnuRepository;
    }

    /**
     * Charge le service au premier appel, puis le rend tel quel.
     * <p>
     * Le dépôt est passé plutôt que construit ici : depuis le passage à SQLite, il n'y a qu'un
     * dépôt de visages dans toute l'application, et c'est un bean Spring. Du temps de MapDB, ce
     * service ouvrait sa propre base et en détenait le verrou — tout le reste du programme devait
     * alors passer par lui pour écrire une empreinte.
     *
     * @param visageConnuRepository le dépôt, ignoré si le service est déjà chargé
     */
    public static synchronized OpenCvServiceReconnaissanceVisage getInstance(VisageConnuRepository visageConnuRepository) {
        if (instance == null) {
            instance = new OpenCvServiceReconnaissanceVisage(
                    Constantes.DOSSIER_VISAGE + File.separator + NOM_MODELE, visageConnuRepository);
        }
        return instance;
    }

    @Override
    public synchronized String identifierPersonne(Mat image, VisageDetecte visage) {
        Mat embedding = calculerEmbedding(image, visage);

        String meilleurIdPersonne = null;
        double meilleurScore = SEUIL_COSINE;
        for (VisageConnu visageConnu : visageConnuRepository.tousLesVisages()) {
            double score = reconnaisseur.match(embedding, matDepuisEmbedding(visageConnu.embedding()), FaceRecognizerSF.FR_COSINE);
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurIdPersonne = visageConnu.idPersonne();
            }
        }
        return meilleurIdPersonne;
    }

    @Override
    public synchronized float[] extraireEmbedding(Mat image, VisageDetecte visage) {
        Mat embedding = calculerEmbedding(image, visage);
        float[] valeurs = new float[embedding.cols()];
        embedding.get(0, 0, valeurs);
        return valeurs;
    }

    @Override
    public synchronized void enrolerPersonne(String idPersonne, List<float[]> empreintes) {
        empreintes.forEach(empreinte -> visageConnuRepository.ajouter(idPersonne, empreinte));
    }

    /**
     * Recadre le visage sur ses points caractéristiques puis en calcule l'empreinte SFace
     * (~68 ms sur Jetson Nano, voir {@code FaceRecognitionPoc}).
     */
    private Mat calculerEmbedding(Mat image, VisageDetecte visage) {
        Mat aligne = new Mat();
        reconnaisseur.alignCrop(image, visage.ligneBrute(), aligne);
        Mat embedding = new Mat();
        reconnaisseur.feature(aligne, embedding);
        return embedding;
    }

    private Mat matDepuisEmbedding(float[] embedding) {
        Mat mat = new Mat(1, embedding.length, CvType.CV_32F);
        mat.put(0, 0, embedding);
        return mat;
    }
}
