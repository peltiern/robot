package fr.roboteek.robot.sandbox.deeplearning;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.typography.general.GeneralFont;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ObjectDetection implements VideoDisplayListener<MBFImage> {

    /**
     * Capture vidéo.
     */
    private VideoCapture capture;
    private VideoDisplay<MBFImage> videoFrame;
    private TinyYoloV2Detector yoloDetector;
    //    private YoloV2Detector yoloDetector;
    private long idx = 0;
    private List<DetectedObject<String>> detectedObjects;


    /**
     * Largeur de la vidéo issue de la webcam.
     */
    private static int LARGEUR_WEBCAM = 640;
    /**
     * Hauteur de la vidéo issue de la webcam.
     */
    private static int HAUTEUR_WEBCAM = 480;


    public ObjectDetection() {
        super();

        yoloDetector = TinyYoloV2Detector.getInstance();
//        yoloDetector = YoloV2Detector.getInstance();

//        try {
//            final MBFImage image1 = ImageUtilities.readMBF(new File("/home/npeltier/Robot/Tests/Yolo/darknet/data/dog.jpg"));
//            detectedObjects = yoloDetector.detect(image1);
//            if (CollectionUtils.isNotEmpty(detectedObjects)) {
//                detectedObjects.stream().forEach(detectedObject -> markDetectedObject(image1, detectedObject));
//            }
//            DisplayUtilities.display(image1);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        Device webcamRobot = null;
        for (Device device : VideoCapture.getVideoDevices()) {
            System.out.println("WEBCAM = " + device);
            if (device.getNameStr() != null && device.getNameStr().toLowerCase().contains("usb")) {
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
        capture.setFPS(20);

        // Création d'un affichage du flux vidéo
        videoFrame = VideoDisplay.createVideoDisplay(capture);

        // Ajout de l'écouteur vidéo
        videoFrame.addVideoListener(this);

    }

    public void afterUpdate(VideoDisplay<MBFImage> display) {
        // do nothing
    }

    public synchronized void beforeUpdate(MBFImage frame) {
        if (idx % 20 == 0) {
            try {
                detectedObjects = yoloDetector.detect(frame);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (CollectionUtils.isNotEmpty(detectedObjects)) {
            detectedObjects.stream().forEach(detectedObject -> markDetectedObject(frame, detectedObject));
        }
        idx++;

    }

    public static void markDetectedObject(MBFImage image, DetectedObject detectedObject) {
        Rectangle bounds = detectedObject.getBounds();
        double x1 = bounds.minX();
        double x2 = bounds.maxX();
        double y1 = bounds.minY();
        double y2 = bounds.maxY();
        image.drawShape(bounds, 5, RGBColour.RED);
        if (StringUtils.isNotEmpty(detectedObject.getLabel())) {
            image.drawPolygonFilled(new Polygon(new Point2dImpl(x1, y2 - 25), new Point2dImpl(x2, y2 - 25), new Point2dImpl(x2, y2), new Point2dImpl(x1, y2)), RGBColour.RED);
            image.drawText(detectedObject.getLabel(), new Point2dImpl(x1 + 2, y2 - 2), new GeneralFont("Chandas", Font.PLAIN), 15, RGBColour.BLACK);
        }
    }

    public static void main(String[] args) {
        ObjectDetection objectDetection = new ObjectDetection();
    }
}
