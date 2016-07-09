package fr.roboteek.robot.organes.capteurs;

import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.VisagesEvent;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;


/**
 * Capteur lié à la vision.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class CapteurVision extends AbstractOrgane implements VideoDisplayListener<MBFImage> {

    /** Capture vidéo.*/
    private VideoCapture capture;
    private VideoDisplay<MBFImage> videoFrame;

    /** Détecteur de visages. */
    private FaceDetector<DetectedFace,FImage> detecteurVisages;

    /** Largeur de la vidéo issue de la webcam. */
    private static int LARGEUR_WEBCAM = 640;
    /** Hauteur de la vidéo issue de la webcam. */
    private static int HAUTEUR_WEBCAM = 480;

    /** Logger. */
    private Logger logger = Logger.getLogger(CapteurVision.class);


    public CapteurVision(MBassador<RobotEvent> systemeNerveux) {
        super(systemeNerveux);

        // Création du détecteur de visages
        detecteurVisages = new HaarCascadeDetector(80);

        // Récupération de la webcam
        System.setProperty(VideoCapture.DEFAULT_DEVICE_NUMBER_PROPERTY, "1");
        // Initialisation du flux de capture sur la webcam
        try {
            capture = new VideoCapture(LARGEUR_WEBCAM, HAUTEUR_WEBCAM);
        } catch (VideoCaptureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        capture.setFPS(10);

        // Création d'un affichage du flux vidéo
        videoFrame = VideoDisplay.createVideoDisplay(capture);

    }

    @Override
    public void initialiser() {
        // Ajout de l'écouteur vidéo
        videoFrame.addVideoListener(this);
    }

    @Override
    public void arreter() {
        // Arrêt de l'afficheur vidéo
        videoFrame.close();
        // Arrêt du capteur
        capture.stopCapture();
        capture.close();
    }

    public void afterUpdate(VideoDisplay<MBFImage> display) {
        // do nothing
    }
    public synchronized void beforeUpdate(MBFImage frame) {

        // Recherche de visages
        final FImage image = Transforms.calculateIntensity(frame);
        final List<DetectedFace> listeVisages = detecteurVisages.detectFaces(image);
        //        final List<DetectedFace> listeVisages = faceTracker.trackFace(frame.flatten());
        for (final DetectedFace visage : listeVisages) {
            frame.drawShape(visage.getShape(), 3, RGBColour.ORANGE);
        }

        // Envoi d'un évènement de détection de visages
        if (listeVisages != null && !listeVisages.isEmpty()) {
            final VisagesEvent event = new VisagesEvent();
            event.setImageOrigine(image);
            event.setListeVisages(listeVisages);
            systemeNerveux.publish(event);
        }
    }

    public static void main(String[] args) {
        CapteurVision capteurVision = new CapteurVision(new MBassador<RobotEvent>());
        capteurVision.initialiser();
    }

}
