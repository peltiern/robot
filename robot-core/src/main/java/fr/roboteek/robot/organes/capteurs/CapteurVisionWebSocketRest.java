package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.memoire.FacialRecognitionResponse;
import fr.roboteek.robot.memoire.ObjectDetectionResponse;
import fr.roboteek.robot.memoire.VisionArtificiellePythonGrpc;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import nu.pattern.OpenCV;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;


/**
 * Capteur lié à la vision.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class CapteurVisionWebSocketRest extends AbstractOrganeWithThread {

    /**
     * Capture vidéo.
     */
    private VideoCapture capture;

    /**
     * Image en cours.
     */
    private Mat image;

    private VisionArtificiellePythonGrpc visionArtificiellePythonGrpc;

    private int indexFrame = 0;

    private FacialRecognitionResponse facialRecognitionResponse;

    private ObjectDetectionResponse objectDetectionResponse;

    /**
     * Configuration.
     */
    private RobotConfig robotConfig;


    public CapteurVisionWebSocketRest() {
        super("VisionActivty");
    }

    @Override
    public void initialiser() {
        robotConfig = robotConfig();

        OpenCV.loadShared();

        //reconnaissanceFacialePython = new ReconnaissanceFacialePython();

        // Recherche de la webcam
        rechercherWebcam();

        image = new Mat();

        boolean captured = false;
        for (int i = 0; i < 10; ++i) {
            captured = capture.read(image);
            if (captured) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignore) {
                // ignore
            }
        }
    }

    @Override
    public void loop() {
        while (capture.isOpened()) {
            if (!capture.read(image)) {
                break;
            }

            traiterImageEnCours();
        }
    }

    public void arreter() {
        capture.release();
    }

    private FacialRecognitionResponse processFaceNameForDetection(FacialRecognitionResponse response) {
        if (response == null || facialRecognitionResponse == null) {
            return null;
        }
        if (!response.isFaceFound()) {
            return response;
        }

        // Calcul des distances de chacun des visages détectés avec les visages de la reconnaissance précédente
        response.getFaces().forEach(recognizedFace -> {
            Vector2D faceCentroid = recognizedFace.getCentroid();
            facialRecognitionResponse.getFaces().stream()
                    .filter(oldRecognizedFace -> oldRecognizedFace.getBounds().getCentroid().distance(faceCentroid) < 40)
                    .findFirst()
                    .ifPresent(nearestOldRecognizedFace -> recognizedFace.setName(nearestOldRecognizedFace.getName()));
        });

        return response;
    }

    private void rechercherWebcam() {
        // Recherche des liens symboliques de la webcam demandée
        String webcamRecherchee = "Twist";
        List<String> liensSymboliquesWebcam = null;

        try {
            // Si la webcam n'est pas la dernière parmi les webcams
            ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "v4l2-ctl --list-devices | grep -A 10 \"" + webcamRecherchee + "\" | grep -B 10 \"usb\" | grep -o \"/dev/video[0-9]*\"");
            Process process = builder.start();
            String retour = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(retour)) {
                liensSymboliquesWebcam = Arrays.asList(retour.split("\n"));
            }

            if (CollectionUtils.isEmpty(liensSymboliquesWebcam)) {
                builder = new ProcessBuilder("/bin/sh", "-c", "v4l2-ctl --list-devices | grep -A 10 \"" + webcamRecherchee + "\" | grep -o \"/dev/video[0-9]*\"");
                process = builder.start();
                retour = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(retour)) {
                    liensSymboliquesWebcam = Arrays.asList(retour.split("\n"));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        if (CollectionUtils.isNotEmpty(liensSymboliquesWebcam)) {

            capture = null;
            for (String lienSymbolique : liensSymboliquesWebcam) {
                capture = new VideoCapture(lienSymbolique);
                if (capture.isOpened()) {
                    // Webcam trouvée
                    break;
                }
            }

            if (capture == null) {
                System.err.println("Pas de caméra trouvée");
            }
        } else {
            System.err.println("Pas de caméra trouvée");
        }
    }

    private void traiterImageEnCours() {
        long debut = System.currentTimeMillis();

        // Convertir l'image en un tableau de bytes
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, mob);
        byte[] ba = mob.toArray();


        // Envoi d'un évènement Vidéo
        VideoEvent videoEvent = new VideoEvent();
        videoEvent.setImageBase64(Base64.getEncoder().encodeToString(ba));
        RobotEventBus.getInstance().publishAsync(videoEvent);
    }

    public static void main(String[] args) {
        CapteurVisionWebSocketRest capteurVision = new CapteurVisionWebSocketRest();
        capteurVision.initialiser();
        capteurVision.start();
    }

}
