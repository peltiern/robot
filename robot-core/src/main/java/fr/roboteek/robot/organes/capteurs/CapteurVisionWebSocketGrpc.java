package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.memoire.DetectedObject;
import fr.roboteek.robot.memoire.FacialRecognitionResponse;
import fr.roboteek.robot.memoire.ObjectDetectionResponse;
import fr.roboteek.robot.memoire.VisionArtificiellePythonGrpc;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import nu.pattern.OpenCV;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.geometry.euclidean.twod.shape.Parallelogram;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;


/**
 * Capteur de vision : capture le flux de la webcam, le transmet au serveur Python
 * de vision artificielle (gRPC, détection d'objets / reconnaissance de visages) et
 * publie un {@link VideoEvent} par image.
 * <p>
 * Migré en bean Spring mais <b>désactivé par défaut</b> (comme l'ancien démarrage
 * qui ne l'instanciait pas). Pour l'activer : {@code robot.capteurs.vision.enabled=true}
 * dans {@code robot.properties} (lu via {@link RobotConfig}, modifiable sans rebuild).
 * Le bean est toujours créé mais reste inerte tant que le flag est faux (garde dans
 * {@link #start()}).
 * <p>
 * Dégradation gracieuse : si aucune webcam n'est trouvée, l'organe reste inerte
 * sans faire échouer le démarrage ; si le serveur Python est absent, les appels
 * gRPC renvoient {@code null} et seule l'image est diffusée (sans détection).
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Component
public class CapteurVisionWebSocketGrpc extends AbstractOrganeWithThread implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(CapteurVisionWebSocketGrpc.class);

    /**
     * Largeur de la vidéo issue de la webcam.
     */
    private static final int LARGEUR_WEBCAM = 640;
    /**
     * Hauteur de la vidéo issue de la webcam.
     */
    private static final int HAUTEUR_WEBCAM = 480;

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

    /**
     * Flag indiquant de stopper le thread de capture.
     */
    private volatile boolean stopperThread = false;

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    public CapteurVisionWebSocketGrpc() {
        super("VisionActivity");
    }

    @Override
    public void initialiser() {
        robotConfig = robotConfig();

        // loadLocally() et non loadShared() : cette dernière n'est plus supportée en
        // Java >= 12 et retombe de toute façon sur loadLocally() avec un log ERROR trompeur.
        OpenCV.loadLocally();

        visionArtificiellePythonGrpc = new VisionArtificiellePythonGrpc();

        image = new Mat();

        // Recherche de la webcam
        rechercherWebcam();

        if (capture == null || !capture.isOpened()) {
            // Pas de webcam : l'organe restera inerte (voir start()).
            return;
        }

        // Quelques lectures pour amorcer le flux (la première image tarde souvent).
        for (int i = 0; i < 10; ++i) {
            if (capture.read(image)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void loop() {
        if (capture == null) {
            return;
        }
        while (!stopperThread && capture.isOpened()) {
            if (!capture.read(image)) {
                break;
            }
            traiterImageEnCours();
        }
    }

    @Override
    public void arreter() {
        stopperThread = true;
        if (capture != null) {
            capture.release();
        }
    }

    private void rechercherWebcam() {
        // Recherche des liens symboliques de la webcam demandée
        String webcamRecherchee = robotConfig.webcamName();
        List<String> liensSymboliquesWebcam = null;

        logger.debug("Recherche de la webcam : {}", webcamRecherchee);

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
            logger.error("Erreur lors de la recherche de la webcam", e);
        }

        if (CollectionUtils.isNotEmpty(liensSymboliquesWebcam)) {
            for (String lienSymbolique : liensSymboliquesWebcam) {
                VideoCapture candidate = new VideoCapture(lienSymbolique);
                candidate.set(Videoio.CAP_PROP_FRAME_WIDTH, LARGEUR_WEBCAM);
                candidate.set(Videoio.CAP_PROP_FRAME_HEIGHT, HAUTEUR_WEBCAM);
                if (candidate.isOpened()) {
                    // Webcam trouvée
                    capture = candidate;
                    logger.info("Webcam trouvée : {}", lienSymbolique);
                    break;
                }
                candidate.release();
            }
        }

        if (capture == null || !capture.isOpened()) {
            logger.error("Pas de caméra trouvée pour '{}'", webcamRecherchee);
        }
    }

    private void traiterImageEnCours() {
        long debut = System.currentTimeMillis();

        // Convertir l'image en un tableau de bytes
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, mob);
        byte[] ba = mob.toArray();

        // Recherche de visages
        //if (indexFrame % 1 == 0 || facialRecognitionResponse == null) {
        //facialRecognitionResponse = visionArtificiellePythonGrpc.recognizeFaces(ba);
//        } else {
//            facialRecognitionResponse = processFaceNameForDetection(visionArtificiellePythonGrpc.detectFaces(ba));
//        }
//        if (facialRecognitionResponse != null && !facialRecognitionResponse.isFaceFound()) {
//            facialRecognitionResponse = null;
//        }

////        if (indexFrame % 3 == 0 || objectDetectionResponse == null) {
        // Détection optionnelle : elle ne doit jamais interrompre le flux vidéo
        // (serveur Python absent = null ; erreur = warn limité, puis on continue).
        try {
            objectDetectionResponse = visionArtificiellePythonGrpc.detectObjects(ba);
        } catch (RuntimeException e) {
            objectDetectionResponse = null;
            if (indexFrame % 100 == 0) {
                logger.warn("Détection indisponible (le flux vidéo continue) : {}", e.getMessage());
            }
        }
////        }
//        if (objectDetectionResponse != null && !objectDetectionResponse.isObjectFound()) {
//            objectDetectionResponse = null;
//        }

        indexFrame++;

        // Envoi d'un évènement Vidéo
        VideoEvent videoEvent = new VideoEvent();

        videoEvent.setImageBase64(Base64.getEncoder().encodeToString(ba));
        if (facialRecognitionResponse != null) {
            videoEvent.setFaceFound(facialRecognitionResponse.isFaceFound());
            videoEvent.setFaces(facialRecognitionResponse.getFaces());
        }
        if (objectDetectionResponse != null) {
            videoEvent.setObjectFound(objectDetectionResponse.isObjectFound());
            videoEvent.setObjects(objectDetectionResponse.getObjects());
        }
        applicationEventPublisher.publishEvent(videoEvent);
        long fin = System.currentTimeMillis();
        if (facialRecognitionResponse != null && CollectionUtils.isNotEmpty(facialRecognitionResponse.getFaces())) {
            logger.debug("({} ms) visages : {}", fin - debut, facialRecognitionResponse.getFaces().stream().map(DetectedObject::getName).collect(Collectors.joining(",")));
        }
        if (objectDetectionResponse != null && CollectionUtils.isNotEmpty(objectDetectionResponse.getObjects())) {
            logger.debug("({} ms) objets : {}", fin - debut, objectDetectionResponse.getObjects().stream().map(DetectedObject::getName).collect(Collectors.joining(",")));
        }
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
            Parallelogram faceBounds = recognizedFace.getBounds();
            Vector2D faceCentroid = faceBounds.getCentroid();
            facialRecognitionResponse.getFaces().stream()
                    .filter(oldRecognizedFace -> faceCentroid.distance(oldRecognizedFace.getBounds().getCentroid()) < 40)
                    .findFirst()
                    .ifPresent(nearestOldRecognizedFace -> recognizedFace.setName(nearestOldRecognizedFace.getName()));
        });

        return response;
    }

    @Override
    public void start() {
        if (!robotConfig().visionEnabled()) {
            logger.info("Vision désactivée (robot.capteurs.vision.enabled=false)");
            return;
        }
        initialiser();
        if (capture == null || !capture.isOpened()) {
            // Aucune webcam : on ne démarre pas le thread, l'organe reste inerte.
            logger.warn("CapteurVision non démarré (aucune webcam)");
            return;
        }
        super.start();
        running = true;
        logger.info("CapteurVision démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("CapteurVision arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.CAPTEURS;
    }
}
