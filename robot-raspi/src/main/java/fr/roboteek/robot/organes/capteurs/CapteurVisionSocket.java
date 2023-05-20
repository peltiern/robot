package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.memoire.FacialRecognitionResponse;
import fr.roboteek.robot.memoire.ObjectDetectionResponse;
import fr.roboteek.robot.memoire.ReconnaissanceFacialePythonRest;
import fr.roboteek.robot.memoire.ReconnaissanceFacialePythonSocket;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import fr.roboteek.robot.util.commons.geometry.Rectangle;
import fr.roboteek.robot.util.webcam.WebcamUtils;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.log4j.Logger;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;


/**
 * Capteur lié à la vision.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class CapteurVisionSocket extends AbstractOrgane {

    /**
     * Capture vidéo.
     */
    private VideoCapture capture;

    /**
     * Largeur de la vidéo issue de la webcam.
     */
    private static final int LARGEUR_WEBCAM = 640;
    /**
     * Hauteur de la vidéo issue de la webcam.
     */
    private static final int HAUTEUR_WEBCAM = 480;

    private ReconnaissanceFacialePythonSocket reconnaissanceFacialePythonSocket;

    private int indexFrame = 0;

    private FacialRecognitionResponse facialRecognitionResponse;

    private ObjectDetectionResponse objectDetectionResponse;

    /**
     * Configuration.
     */
    private RobotConfig robotConfig;

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(CapteurVisionSocket.class);


    public CapteurVisionSocket() {

        robotConfig = robotConfig();

        //reconnaissanceFacialePython = new ReconnaissanceFacialePython();

        // Récupération du flux de capture de la webcam
        try {
            capture = WebcamUtils.getWebcamCaptureParNom(robotConfig().webcamName());
        } catch (IOException e) {
            logger.error("Erreur lors de la récupérartion de la webcam", e);
            throw new RuntimeException(e);
        }

       // TODO Gérer la taille et le FPS


        // TODO A décommenter pour permettre de récupérer le thread qui est lancé dans createOffscreenVideoDisplay
//        videoFrame = new VideoDisplay<MBFImage>(capture, null);
//        videoFrame.displayMode(false);
//        // Ajout de l'écouteur vidéo
//        videoFrame.addVideoListener(this);
    }

    public void initialiser() {
        // TODO A décommenter pour permettre de récupérer le thread qui est lancé dans createOffscreenVideoDisplay
//        new Thread(videoFrame).start();
    }

    public void arreter() {
        // TODO arrêter la capture
    }

    public synchronized void beforeUpdate(MBFImage frame) {

        // Recherche de visages
//        if (indexFrame % 25 == 0 || facialRecognitionResponse == null) {
//            facialRecognitionResponse = reconnaissanceFacialePython.recognizeFaces(frame);
//        } else {
//            facialRecognitionResponse = processFaceNameForDetection(reconnaissanceFacialePython.detectFaces(frame));
//        }
//        if (facialRecognitionResponse != null && !facialRecognitionResponse.isFaceFound()) {
//            facialRecognitionResponse = null;
//        }
//
//        if (indexFrame % 3 == 0 || objectDetectionResponse == null) {
//            objectDetectionResponse = reconnaissanceFacialePython.detectObjects(frame);
//        }
//        if (objectDetectionResponse != null && !objectDetectionResponse.isObjectFound()) {
//            objectDetectionResponse = null;
//        }
//
//        indexFrame++;

        // Envoi d'un évènement de détection de visages
////        final VisagesEvent event = new VisagesEvent();
////        event.setImageOrigine(image);
//        //event.setListeVisages(listeVisages);
//        //AbstractActivite.suivreVisage(event);
////        RobotEventBus.getInstance().publishAsync(event);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageUtilities.write(frame, "jpg", baos);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        VideoEvent videoEvent = new VideoEvent();
        videoEvent.setImageBase64(Base64.getEncoder().encodeToString(baos.toByteArray()));
//        if (facialRecognitionResponse != null) {
//            videoEvent.setFaceFound(facialRecognitionResponse.isFaceFound());
//            videoEvent.setFaces(facialRecognitionResponse.getFaces());
//        }
//        if (objectDetectionResponse != null) {
//            videoEvent.setObjectFound(objectDetectionResponse.isObjectFound());
//            videoEvent.setObjects(objectDetectionResponse.getObjects());
//        }
        RobotEventBus.getInstance().publishAsync(videoEvent);
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

    public static void main(String[] args) {
        CapteurVisionSocket capteurVision = new CapteurVisionSocket();
        capteurVision.initialiser();
    }

}
