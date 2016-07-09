package fr.roboteek.robot.test;

import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.Device;
import org.openimaj.video.capture.VideoCapture;


public class PhidgetVideoFaceTracker implements VideoDisplayListener<MBFImage> {
    private VideoCapture capture;
    private VideoDisplay<MBFImage> videoFrame;
    FaceDetector<DetectedFace,FImage> faceDetector;
    
    private static int LARGEUR_WEBCAM = 640;
    private static int HAUTEUR_WEBCAM = 480;
    
    private static int LARGEUR_ZONE_TRACKING = 180;
    private static int HAUTEUR_ZONE_TRACKING = 180;
    
    private static int X1 = (LARGEUR_WEBCAM - LARGEUR_ZONE_TRACKING) / 2;
    private static int X2 = X1 + LARGEUR_ZONE_TRACKING;
    private static int Y1 = (HAUTEUR_WEBCAM - HAUTEUR_ZONE_TRACKING) / 2;
    private static int Y2 = Y1 + HAUTEUR_ZONE_TRACKING;
    
    private Rectangle zoneTracking;
    
    private Polygon flecheHaut;
    private Polygon flecheBas;
    private Polygon flecheGauche;
    private Polygon flecheDroite;
    
    
    public PhidgetVideoFaceTracker() throws Exception {
        
        
        // Initialisation de la zone de tracking et des différents éléments à afficher
        zoneTracking = new Rectangle(X1, Y1, LARGEUR_ZONE_TRACKING, HAUTEUR_ZONE_TRACKING);
        flecheBas = new Polygon(new Point2dImpl(0, 30), new Point2dImpl(-10, 10), new Point2dImpl(-5, 10), new Point2dImpl(-5, -30), new Point2dImpl(5, -30), new Point2dImpl(5, 10), new Point2dImpl(10, 10));
        flecheHaut = flecheBas.clone();
        flecheHaut.rotate(Math.PI);
        flecheGauche = flecheBas.clone();
        flecheGauche.rotate(Math.PI / 2);
        flecheDroite = flecheBas.clone();
        flecheDroite.rotate(-Math.PI / 2);
        
        flecheBas.translate(LARGEUR_WEBCAM / 2, Y2 + Y1 / 2);
        flecheHaut.translate(LARGEUR_WEBCAM / 2, Y1 / 2);
        flecheGauche.translate(X1 / 2, HAUTEUR_WEBCAM / 2);
        flecheDroite.translate(X2 + X1 / 2, HAUTEUR_WEBCAM / 2);
        
        // Récupération de la webcam
        System.setProperty(VideoCapture.DEFAULT_DEVICE_NUMBER_PROPERTY, "1");
        
        for (Device device : VideoCapture.getVideoDevices()) {
        	System.out.println(device.getIdentifierStr() + " : " + device.getNameStr());
        }
        
     // Création du détecteur de visages
        faceDetector = new HaarCascadeDetector(80);
        
        // Initialisation du flux de capture sur la webcam
        capture = new VideoCapture(LARGEUR_WEBCAM, HAUTEUR_WEBCAM);
//        capture.setFPS(10);
        
        // Création d'un affichage du flux vidéo
        videoFrame = VideoDisplay.createVideoDisplay(capture);
        videoFrame.addVideoListener(this);
        
        
       
    }
   
    public void afterUpdate(VideoDisplay<MBFImage> display) {
        // do nothing
    }
    public synchronized void beforeUpdate(MBFImage frame) {
        
        // Recherche du visage le plus grand
        double aireVisagePlusGrand = 0d;
        DetectedFace visagePlusGrand = null;
        List<DetectedFace> faces = faceDetector.detectFaces( Transforms.calculateIntensity(frame));
          for (final DetectedFace f : faces) {
            frame.drawShape(f.getShape(), 3, RGBColour.ORANGE);
            frame.drawText(String.valueOf(f.getConfidence()), f.getBounds().getTopLeft(), HersheyFont.GREEK, 25, RGBColour.BLACK);
            double aireVisage = f.getBounds().calculateArea();
            if (aireVisage > aireVisagePlusGrand) {
                aireVisagePlusGrand = aireVisage;
                visagePlusGrand = f;
            }
        }
        
        if (visagePlusGrand != null) {
            // Récupération du centre de gravité du visage
            Point2d centreVisage = visagePlusGrand.getShape().calculateCentroid();
            
            frame.drawPoint(centreVisage, RGBColour.BLUE, 15);
            
            if (zoneTracking.isInside(centreVisage)) {
                frame.drawShape(zoneTracking, 5, RGBColour.GREEN);
            } else {
                frame.drawShape(zoneTracking, 5, RGBColour.RED);
            }
            
            // Mouvement du moteur A (lacet <==> tourner la tête horizontalement (pour faire non))
            if (centreVisage.getX() < X1 || centreVisage.getX() > X2) {
                if (centreVisage.getX() < X1) {
                 // Si le centre du visage est à gauche de la zone de tracking : tourner à gauche
//                    tete.tournerAGauche();
                    frame.drawShape(flecheGauche, 3, RGBColour.YELLOW);
                } else {
                 // Si le centre du visage est à droite de la zone de tracking : tourner à droite
//                    tete.tournerADroite();
                    frame.drawShape(flecheDroite, 3, RGBColour.YELLOW);
                }
            } else {
                // Le centre du visage est dans la zone de tracking : on stoppe
//                tete.stopperTeteGaucheDroite();
            }
            
            // Mouvement du moteur B (tangage <==> tourner la tête verticalement (pour faire oui))
            if (centreVisage.getY() < Y1 || centreVisage.getY() > Y2) {
                if (centreVisage.getY() < Y1) {
                 // Si le centre du visage est au-dessus de la zone de tracking : tourner vers le haut
//                    tete.tournerEnHaut();
                    frame.drawShape(flecheHaut, 3, RGBColour.YELLOW);
                } else {
                 // Si le centre du visage est en-dessous de la zone de tracking : tourner vers le bas
//                    tete.tournerEnBas();
                    frame.drawShape(flecheBas, 3, RGBColour.YELLOW);
                }
            } else {
                // Le centre du visage est dans la zone de tracking : on stoppe
//                tete.stopperTeteHautBas();
            }
            
        } else {
            frame.drawShape(zoneTracking, 5, RGBColour.BLUE);
            // Pas de visage : on stoppe tout
//            tete.stopperTeteGaucheDroite();
//            tete.stopperTeteHautBas();
        }
    }
    
    public static void main(String[] args) throws Exception {
        new PhidgetVideoFaceTracker();
    }

} 
