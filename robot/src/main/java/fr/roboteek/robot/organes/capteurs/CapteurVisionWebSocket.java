package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.memoire.FacialRecognitionResponse;
import fr.roboteek.robot.memoire.ReconnaissanceFacialePython;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.server.ImageWithRecognizedFaces;
import fr.roboteek.robot.server.VideoWebSocket;
import org.apache.log4j.Logger;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;


/**
 * Capteur lié à la vision.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class CapteurVisionWebSocket extends AbstractOrgane implements VideoDisplayListener<MBFImage> {

    /**
     * Capture vidéo.
     */
    private VideoCapture capture;
    private VideoDisplay<MBFImage> videoFrame;

    /**
     * Largeur de la vidéo issue de la webcam.
     */
    private static int LARGEUR_WEBCAM = 640;
    /**
     * Hauteur de la vidéo issue de la webcam.
     */
    private static int HAUTEUR_WEBCAM = 480;

    private ReconnaissanceFacialePython reconnaissanceFacialePython;

    private int indexFrame = 0;

    private FacialRecognitionResponse facialRecognitionResponse;

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(CapteurVisionWebSocket.class);


    public CapteurVisionWebSocket() {

        reconnaissanceFacialePython = new ReconnaissanceFacialePython();

        // Récupération de la webcam
        Device webcamRobot = null;
        for (Device device : VideoCapture.getVideoDevices()) {
            System.out.println("WEBCAM = " + device);
            if (device.getNameStr() != null && device.getNameStr().toLowerCase().contains("twist")) {
                System.out.println("WEBCAM ROBOT TROUVEE");
                webcamRobot = device;
                break;
            }
        }

        // Initialisation du flux de capture sur la webcam
        System.setProperty(VideoCapture.DEFAULT_DEVICE_NUMBER_PROPERTY, "0");
        try {
            if (webcamRobot != null) {
                capture = new VideoCapture(LARGEUR_WEBCAM, HAUTEUR_WEBCAM, 10, webcamRobot);
            } else {
                capture = new VideoCapture(LARGEUR_WEBCAM, HAUTEUR_WEBCAM);
            }
        } catch (VideoCaptureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        capture.setFPS(25);

        // Création d'un affichage du flux vidéo
        videoFrame = VideoDisplay.createVideoDisplay(capture);

    }

    public void initialiser() {
        // Ajout de l'écouteur vidéo
        videoFrame.addVideoListener(this);
    }

    public void arreter() {
        // Arrêt de l'afficheur vidéo
        videoFrame.close();
        // Arrêt du capteur
        capture.stopCapture();
        capture.close();
    }

    public void afterUpdate(VideoDisplay<MBFImage> display) {

    }

    public synchronized void beforeUpdate(MBFImage frame) {

        // Recherche de visages
        final FImage image = Transforms.calculateIntensity(frame);
        if (indexFrame % 10 == 0 || facialRecognitionResponse == null) {
            facialRecognitionResponse = reconnaissanceFacialePython.recognizeFaces(frame);
        } else {
            facialRecognitionResponse = processFaceNameForDetection(reconnaissanceFacialePython.detectFaces(frame));
        }
        if (facialRecognitionResponse != null && !facialRecognitionResponse.isFaceFound()) {
            facialRecognitionResponse = null;
        }
        indexFrame++;

        // Envoi d'un évènement de détection de visages
//        final VisagesEvent event = new VisagesEvent();
//        event.setImageOrigine(image);
        //event.setListeVisages(listeVisages);
        //AbstractActivite.suivreVisage(event);
//        RobotEventBus.getInstance().publishAsync(event);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageUtilities.write(frame, "jpg", baos);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        VideoWebSocket.broadcastImage(baos.toByteArray());
        ImageWithRecognizedFaces imageWithRecognizedFaces = new ImageWithRecognizedFaces();
        imageWithRecognizedFaces.setImageBase64(Base64.getEncoder().encodeToString(baos.toByteArray()));
        if (facialRecognitionResponse != null) {
            imageWithRecognizedFaces.setFaceFound(facialRecognitionResponse.isFaceFound());
            imageWithRecognizedFaces.setFaces(facialRecognitionResponse.getFaces());
        }
        VideoWebSocket.broadcastImageWithFaceInfos(imageWithRecognizedFaces);
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
            Rectangle faceBounds = recognizedFace.getBounds();
            Point2d faceCentroid = faceBounds.calculateCentroid();
            facialRecognitionResponse.getFaces().stream()
                    .filter(oldRecognizedFace -> Line2d.distance(oldRecognizedFace.getBounds().calculateCentroid(), faceCentroid) < 40)
                    .findFirst()
                    .ifPresent(nearestOldRecognizedFace -> recognizedFace.setName(nearestOldRecognizedFace.getName()));
        });

        return response;
    }

    public static void main(String[] args) {
        CapteurVisionWebSocket capteurVision = new CapteurVisionWebSocket();
        capteurVision.initialiser();
    }

}
