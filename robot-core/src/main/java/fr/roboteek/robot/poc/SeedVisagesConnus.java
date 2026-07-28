package fr.roboteek.robot.poc;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.visage.VisageConnuRepository;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceDetectionVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;
import java.util.List;

/**
 * Utilitaire jetable : enregistre un ou plusieurs visages de référence dans la base
 * MapDB des visages connus, en attendant un vrai mécanisme d'enrôlement (Phase 3).
 * <p>
 * Usage : {@code java -jar robot-core-seed.jar <nom1> <image1.jpg> [<nom2> <image2.jpg> ...]}
 * (nécessite de pointer temporairement {@code mainClass} vers cette classe dans le pom,
 * comme pour {@link FaceRecognitionPoc}).
 * <p>
 * À exécuter avec le même volume que le conteneur de production (~/Robot/Programme
 * monté sur /Robot/Programme, ROBOT_HOME positionné) pour écrire dans la même base
 * que celle relue ensuite par {@code CapteurVisionWebSocketGrpc}.
 */
public class SeedVisagesConnus {

    public static void main(String[] args) {
        if (args.length < 2 || args.length % 2 != 0) {
            System.err.println("Usage: SeedVisagesConnus <nom1> <image1.jpg> [<nom2> <image2.jpg> ...]");
            System.exit(1);
        }

        OpenCV.loadLocally();

        String cheminModeleSFace = Constantes.DOSSIER_VISAGE + File.separator + "face_recognition_sface_2021dec.onnx";
        FaceRecognizerSF reconnaisseur = FaceRecognizerSF.create(cheminModeleSFace, "");
        VisageConnuRepository repository = new VisageConnuRepository();

        for (int i = 0; i < args.length; i += 2) {
            String nom = args[i];
            String imagePath = args[i + 1];

            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                System.err.println("Image illisible, ignorée : " + imagePath);
                continue;
            }

            List<VisageDetecte> visages = OpenCvServiceDetectionVisage.getInstance().detecter(image);
            if (visages.isEmpty()) {
                System.err.println("Aucun visage détecté, ignoré : " + imagePath);
                continue;
            }

            Mat aligne = new Mat();
            reconnaisseur.alignCrop(image, visages.get(0).ligneBrute(), aligne);
            Mat embeddingMat = new Mat();
            reconnaisseur.feature(aligne, embeddingMat);

            float[] embedding = new float[embeddingMat.cols()];
            embeddingMat.get(0, 0, embedding);

            repository.ajouter(nom, embedding);
            System.out.println("Visage enregistré : " + nom + " (" + imagePath + ")");
        }

        repository.close();
    }
}
