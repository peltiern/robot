package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.services.vision.face.VisageDansLImage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test de fumée : détection YuNet sur de vraies photos. Ne s'exécute que si le
 * modèle ONNX est présent dans {@code ROBOT_HOME/visage} (voir
 * fr.roboteek.robot.poc.FaceRecognitionPoc) — jamais commité dans le dépôt, donc
 * ignoré par défaut sur un poste de dev / en CI.
 */
class OpenCvServiceDetectionVisageTest {

    private static final String DOSSIER_PHOTOS = "../robot-python/vision-artificielle/known-faces/";

    private static final String IMAGE_AMY = DOSSIER_PHOTOS + "0001_amy.jpg";

    @BeforeAll
    static void verifierModeleDisponible() {
        assumeTrue(new File(Constantes.DOSSIER_VISAGE, "face_detection_yunet_2023mar.onnx").exists(),
                "Modèle YuNet absent de ROBOT_HOME/visage : test ignoré");
        OpenCV.loadLocally();
    }

    /**
     * Le garde-fou de la porte de qualité : de vraies photos, prises sans précaution particulière,
     * doivent la passer. C'est le risque de ce genre de seuil — refuser tout le monde plutôt que
     * les mauvaises prises —, et il ne se voit que sur des visages réels, avec les points rendus
     * par YuNet.
     */
    @Test
    void lesPhotosDExempleFranchissentLaPorteDeQualite() {
        for (String nom : new String[]{"0001_amy.jpg", "0003_penny.jpg", "0005_einstein.jpg"}) {
            Mat image = Imgcodecs.imread(DOSSIER_PHOTOS + nom);
            assertFalse(image.empty(), "Image de test introuvable : " + nom);

            List<VisageDetecte> visages = OpenCvServiceDetectionVisage.getInstance().detecter(image);

            assertEquals(1, visages.size(), nom);
            assertEquals(VisageDansLImage.Qualite.EXPLOITABLE, VisageDansLImage.qualite(image, visages.getFirst()),
                    () -> nom + " : asymétrie " + visages.getFirst().asymetrieDuNez()
                            + ", netteté " + VisageDansLImage.nettete(image, visages.getFirst()));
        }
    }

    @Test
    void detecteUnVisageDansUnePhotoConnue() {
        Mat image = Imgcodecs.imread(IMAGE_AMY);
        assertFalse(image.empty(), "Image de test introuvable : " + IMAGE_AMY);

        List<VisageDetecte> visages = OpenCvServiceDetectionVisage.getInstance().detecter(image);

        assertEquals(1, visages.size());
        VisageDetecte visage = visages.get(0);
        assertTrue(visage.width() > 0 && visage.height() > 0);
        assertTrue(visage.score() > 0.8f);
    }
}
