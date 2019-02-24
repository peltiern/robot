package fr.roboteek.robot.sandbox.openimaj;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.*;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

import java.util.List;

public class FaceDetection implements VideoDisplayListener<MBFImage> {

    /** Capture vidéo.*/
    private VideoCapture capture;
    private VideoDisplay<MBFImage> videoFrame;

    /** Détecteur de visages. */
//    private CLMFaceDetector detecteurVisages;
    private FKEFaceDetector detecteurVisages;

    /** Largeur de la vidéo issue de la webcam. */
    private static int LARGEUR_WEBCAM = 640;
    /** Hauteur de la vidéo issue de la webcam. */
    private static int HAUTEUR_WEBCAM = 480;


    public FaceDetection() {
        super();

        // Création du détecteur de visages
//        detecteurVisages = new CLMFaceDetector();
        detecteurVisages = new FKEFaceDetector();

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
        capture.setFPS(10);

        // Création d'un affichage du flux vidéo
        videoFrame = VideoDisplay.createVideoDisplay(capture);

        // Ajout de l'écouteur vidéo
        videoFrame.addVideoListener(this);

    }

    public void afterUpdate(VideoDisplay<MBFImage> display) {
        // do nothing
    }
    public synchronized void beforeUpdate(MBFImage frame) {

        // Recherche de visages
        final FImage image = Transforms.calculateIntensity(frame);
        final List<KEDetectedFace> listeVisages = detecteurVisages.detectFaces(image);
        //        final List<DetectedFace> listeVisages = faceTracker.trackFace(frame.flatten());
        for (final KEDetectedFace visage : listeVisages) {
            frame.drawShape(visage.getBounds(), 3, RGBColour.RED);
//            System.out.println("Roll = " + visage.getRoll() + "\tPitch = " + visage.getPitch() + "\tYaw" + visage.getYaw());
        }

    }

    public static void main(String[] args) {
        FaceDetection capteurVision = new FaceDetection();
    }
}
