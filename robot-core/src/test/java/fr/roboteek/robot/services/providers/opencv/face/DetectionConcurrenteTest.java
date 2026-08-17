package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Rejoue la panne du 2026-08-17 : deux threads dans le même détecteur, sur deux tailles d'image.
 * <p>
 * Le robot faisait tourner sa boucle vidéo en 640×480 pendant qu'un import de photo arrivait par
 * l'interface web, sur un thread HTTP et avec une image de tout autre taille. {@code FaceDetectorYN}
 * n'est pas réentrant : le backend dnn a corrompu sa table de réutilisation de tampons et OpenCV a
 * levé {@code (-215:Assertion failed) mapIt != reuseMap.end()}.
 * <p>
 * Deux tailles différentes et non une seule, à dessein : c'est ce qui déclenche en plus les
 * {@code setInputSize} concurrents, l'autre moitié du problème.
 * <p>
 * Ignoré sans les modèles ONNX — donc sur un poste de dev et en intégration continue. Il vaut pour
 * le Jetson, là où la panne s'est produite.
 */
class DetectionConcurrenteTest {

    private static final int CYCLES = 40;

    @BeforeAll
    static void verifierModeleDisponible() {
        assumeTrue(new File(Constantes.DOSSIER_VISAGE, "face_detection_yunet_2023mar.onnx").exists(),
                "Modèle YuNet absent de ROBOT_HOME/visage : test ignoré");
        OpenCV.loadLocally();
    }

    @Test
    @Timeout(120)
    void deuxThreadsPeuventDetecterEnMemeTempsSurDesTaillesDifferentes() throws InterruptedException {
        OpenCvServiceDetectionVisage detection = OpenCvServiceDetectionVisage.getInstance();
        List<Throwable> plantages = new CopyOnWriteArrayList<>();
        CountDownLatch depart = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(2);

        // La boucle vidéo du robot, et l'import de photo par l'interface web.
        Thread boucleVideo = detecterEnBoucle(detection, 640, 480, depart, fin, plantages);
        Thread importPhoto = detecterEnBoucle(detection, 1280, 960, depart, fin, plantages);
        boucleVideo.start();
        importPhoto.start();

        depart.countDown();
        fin.await();

        assertTrue(plantages.isEmpty(),
                () -> "OpenCV a lâché sous deux threads : " + plantages.getFirst());
    }

    private static Thread detecterEnBoucle(OpenCvServiceDetectionVisage detection,
                                           int largeur, int hauteur,
                                           CountDownLatch depart, CountDownLatch fin,
                                           List<Throwable> plantages) {
        return new Thread(() -> {
            // Une image unie : on ne cherche aucun visage ici, seulement à faire tourner le
            // détecteur. Ce qui est vérifié, c'est qu'il ne casse pas, pas ce qu'il trouve.
            Mat image = new Mat(hauteur, largeur, CvType.CV_8UC3, new Scalar(120, 120, 120));
            try {
                depart.await();
                for (int i = 0; i < CYCLES; i++) {
                    detection.detecter(image);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                plantages.add(e);
            } finally {
                image.release();
                fin.countDown();
            }
        }, "detection-" + largeur + "x" + hauteur);
    }
}
