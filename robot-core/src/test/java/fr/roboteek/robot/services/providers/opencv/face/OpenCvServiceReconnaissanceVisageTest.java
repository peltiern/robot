package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.visage.VisageConnuRepository;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test de fumée : détection + reconnaissance SFace de bout en bout, sur une base
 * de visages connus isolée (tmp), donc sans toucher à la vraie base de production.
 * Ne s'exécute que si les modèles ONNX sont présents dans {@code ROBOT_HOME/visage}
 * (voir fr.roboteek.robot.poc.FaceRecognitionPoc) — ignoré par défaut.
 */
class OpenCvServiceReconnaissanceVisageTest {

    private static final String IMAGE_AMY = "../robot-python/vision-artificielle/known-faces/0001_amy.jpg";
    private static final String IMAGE_EINSTEIN = "../robot-python/vision-artificielle/known-faces/0005_einstein.jpg";
    private static final String IMAGE_SHELDON = "../robot-python/vision-artificielle/known-faces/0002_sheldon.jpg";

    private static String cheminModeleSFace;

    @TempDir
    File dossierTemp;

    private VisageConnuRepository repository;
    private OpenCvServiceReconnaissanceVisage reconnaissance;
    private FaceRecognizerSF reconnaisseurPourEnrolement;

    @BeforeAll
    static void verifierModelesDisponibles() {
        cheminModeleSFace = Constantes.DOSSIER_VISAGE + File.separator + "face_recognition_sface_2021dec.onnx";
        assumeTrue(new File(Constantes.DOSSIER_VISAGE, "face_detection_yunet_2023mar.onnx").exists()
                        && new File(cheminModeleSFace).exists(),
                "Modèles ONNX absents de ROBOT_HOME/visage : test ignoré");
        OpenCV.loadLocally();
    }

    @BeforeEach
    void setUp() {
        repository = new VisageConnuRepository(new File(dossierTemp, "visages.db").getAbsolutePath());
        reconnaissance = new OpenCvServiceReconnaissanceVisage(cheminModeleSFace, repository);
        reconnaisseurPourEnrolement = FaceRecognizerSF.create(cheminModeleSFace, "");

        enregistrer("Amy", IMAGE_AMY);
        enregistrer("Einstein", IMAGE_EINSTEIN);
    }

    @AfterEach
    void tearDown() {
        repository.close();
    }

    @Test
    void identifieLesVisagesEnregistres() {
        assertEquals("Amy", identifierPremierVisage(IMAGE_AMY));
        assertEquals("Einstein", identifierPremierVisage(IMAGE_EINSTEIN));
    }

    @Test
    void unVisageNonEnregistreResteInconnu() {
        assertNull(identifierPremierVisage(IMAGE_SHELDON));
    }

    private void enregistrer(String nom, String cheminImage) {
        Mat image = Imgcodecs.imread(cheminImage);
        assertFalse(image.empty(), "Image de test introuvable : " + cheminImage);
        VisageDetecte visage = OpenCvServiceDetectionVisage.getInstance().detecter(image).get(0);

        Mat aligne = new Mat();
        reconnaisseurPourEnrolement.alignCrop(image, visage.ligneBrute(), aligne);
        Mat embeddingMat = new Mat();
        reconnaisseurPourEnrolement.feature(aligne, embeddingMat);
        float[] embedding = new float[embeddingMat.cols()];
        embeddingMat.get(0, 0, embedding);

        repository.ajouter(nom, embedding);
    }

    private String identifierPremierVisage(String cheminImage) {
        Mat image = Imgcodecs.imread(cheminImage);
        List<VisageDetecte> visages = OpenCvServiceDetectionVisage.getInstance().detecter(image);
        return reconnaissance.identifier(image, visages.get(0));
    }
}
