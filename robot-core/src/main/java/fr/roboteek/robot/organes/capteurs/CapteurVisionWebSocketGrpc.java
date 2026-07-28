package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.memoire.DetectedObject;
import fr.roboteek.robot.memoire.ObjectDetectionResponse;
import fr.roboteek.robot.memoire.RecognizedFace;
import fr.roboteek.robot.memoire.VisionArtificiellePythonGrpc;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceDetectionVisage;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.ServiceDetectionVisage;
import fr.roboteek.robot.services.vision.face.ServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import nu.pattern.OpenCV;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.ArrayList;
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
     * Fréquence (en nombre de frames) à laquelle la détection + reconnaissance de
     * visages est lancée : à chaque frame le coût serait trop élevé pour la boucle
     * vidéo (~130 ms/visage mesuré sur Jetson Nano 4 Go, voir fr.roboteek.robot.poc.FaceRecognitionPoc).
     */
    private static final int FREQUENCE_RECONNAISSANCE_VISAGE = 3;

    /**
     * Fréquence (en nombre de frames) à laquelle une image est effectivement publiée
     * sur le WebSocket. Publier à chaque frame capturée sature le tampon d'envoi de
     * la session dès que le client (navigateur/réseau) ne suit pas le débit — Spring
     * ferme alors la session (« Buffer size ... exceeds the allowed limit », voir
     * {@code WebSocketBrokerConfig}), ce qui a déjà causé une première fois un
     * relèvement de la limite (512 Ko → 8 Mo) sans traiter la cause : aucune limite
     * de tampon ne suffit si le débit de production n'est pas maîtrisé.
     */
    private static final int FREQUENCE_PUBLICATION_VIDEO = 3;

    /**
     * Capture vidéo.
     */
    private VideoCapture capture;

    /**
     * Image en cours.
     */
    private Mat image;

    private VisionArtificiellePythonGrpc visionArtificiellePythonGrpc;

    /**
     * Détection/reconnaissance de visages (OpenCV local, CPU) : {@code null} si
     * l'initialisation a échoué (modèles absents), auquel cas la fonctionnalité
     * reste désactivée sans bloquer le reste de l'organe.
     */
    private ServiceDetectionVisage serviceDetectionVisage;
    private ServiceReconnaissanceVisage serviceReconnaissanceVisage;

    private int indexFrame = 0;

    private List<RecognizedFace> derniersVisagesReconnus;

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

        try {
            serviceDetectionVisage = OpenCvServiceDetectionVisage.getInstance();
            serviceReconnaissanceVisage = OpenCvServiceReconnaissanceVisage.getInstance();
        } catch (RuntimeException e) {
            logger.warn("Détection/reconnaissance de visages indisponible (modèles absents dans {} ?) : {}", Constantes.DOSSIER_VISAGE, e.getMessage());
        }

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

        // Détection + reconnaissance de visages (local, CPU) : throttlée (voir
        // FREQUENCE_RECONNAISSANCE_VISAGE), et désactivée si les modèles n'ont pas
        // pu être chargés au démarrage (dégradation gracieuse, comme pour la vidéo/objets).
        if (serviceDetectionVisage != null && indexFrame % FREQUENCE_RECONNAISSANCE_VISAGE == 0) {
            try {
                List<VisageDetecte> visagesDetectes = serviceDetectionVisage.detecter(image);
                List<RecognizedFace> visagesReconnus = new ArrayList<>();
                for (VisageDetecte visageDetecte : visagesDetectes) {
                    RecognizedFace visageReconnu = new RecognizedFace(visageDetecte.x(), visageDetecte.y(), visageDetecte.width(), visageDetecte.height());
                    visageReconnu.setName(serviceReconnaissanceVisage.identifier(image, visageDetecte));
                    visagesReconnus.add(visageReconnu);
                }
                derniersVisagesReconnus = visagesReconnus;
            } catch (RuntimeException e) {
                derniersVisagesReconnus = null;
                if (indexFrame % 100 == 0) {
                    logger.warn("Reconnaissance de visages indisponible (le flux vidéo continue) : {}", e.getMessage());
                }
            }
        }

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

        if (indexFrame % FREQUENCE_PUBLICATION_VIDEO == 0) {
            // Envoi d'un évènement Vidéo
            VideoEvent videoEvent = new VideoEvent();

            videoEvent.setImageBase64(Base64.getEncoder().encodeToString(ba));
            if (derniersVisagesReconnus != null) {
                videoEvent.setFaceFound(!derniersVisagesReconnus.isEmpty());
                videoEvent.setFaces(derniersVisagesReconnus);
            }
            if (objectDetectionResponse != null) {
                videoEvent.setObjectFound(objectDetectionResponse.isObjectFound());
                videoEvent.setObjects(objectDetectionResponse.getObjects());
            }
            applicationEventPublisher.publishEvent(videoEvent);
            long fin = System.currentTimeMillis();
            if (CollectionUtils.isNotEmpty(derniersVisagesReconnus)) {
                logger.debug("({} ms) visages : {}", fin - debut, derniersVisagesReconnus.stream().map(DetectedObject::getName).collect(Collectors.joining(",")));
            }
            if (objectDetectionResponse != null && CollectionUtils.isNotEmpty(objectDetectionResponse.getObjects())) {
                logger.debug("({} ms) objets : {}", fin - debut, objectDetectionResponse.getObjects().stream().map(DetectedObject::getName).collect(Collectors.joining(",")));
            }
        }
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
