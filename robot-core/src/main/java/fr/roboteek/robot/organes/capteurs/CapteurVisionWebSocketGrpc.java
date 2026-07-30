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
import fr.roboteek.robot.spring.server.websocket.RegistreAbonnesWebsocket;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.webcam.SuiviVisageUtils;
import nu.pattern.OpenCV;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     * Destination STOMP du flux vidéo (voir {@code WebSocketBrokerConfig}). Rien n'est
     * encodé ni publié tant que personne n'y est abonné.
     */
    private static final String DESTINATION_VIDEO = "/video";

    /**
     * Distance de centroïde (en pixels) sous laquelle un visage détecté est considéré
     * comme le même qu'un visage déjà identifié à la frame précédente : on réutilise
     * alors son nom sans relancer SFace (alignCrop + feature + comparaison à toute la
     * base coûtent ~68 ms par visage, voir fr.roboteek.robot.poc.FaceRecognitionPoc).
     * Seuls les visages déjà identifiés (nom non nul) sont ainsi suivis : un visage
     * encore inconnu est retenté à chaque frame throttlée, pour lui laisser une chance
     * d'être reconnu si les conditions (angle, éclairage) s'améliorent entre-temps.
     */
    private static final double DISTANCE_MAX_SUIVI_VISAGE = 40;

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
     * Registre des abonnements WebSocket : sert à ne rien produire quand personne ne regarde.
     */
    @Autowired
    private RegistreAbonnesWebsocket registreAbonnesWebsocket;

    /**
     * Horodatage de la dernière image publiée, pour cadencer le flux en temps réel plutôt
     * qu'en nombre de frames : la cadence de la webcam varie (luminosité, charge CPU), une
     * frame sur N ne borne donc pas le débit réellement envoyé.
     */
    private long dernierePublicationVideoMs = 0;

    /**
     * Indique si le flux vidéo est en cours de diffusion, pour ne tracer que les transitions.
     */
    private boolean fluxVideoDiffuse = false;

    /**
     * Paramètres d'encodage JPEG, reconstruits uniquement quand la qualité configurée change
     * (objet natif OpenCV : ni instanciable avant {@code OpenCV.loadLocally()}, ni à recréer
     * à chaque image).
     */
    private MatOfInt parametresJpeg;

    /**
     * Qualité JPEG ayant servi à construire {@link #parametresJpeg}.
     */
    private int qualiteJpegCourante = -1;

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
        Imgcodecs.imencode(".jpg", image, mob, parametresJpeg());
        byte[] ba = mob.toArray();

        // Détection + reconnaissance de visages (local, CPU) : throttlée (voir
        // FREQUENCE_RECONNAISSANCE_VISAGE), et désactivée si les modèles n'ont pas
        // pu être chargés au démarrage (dégradation gracieuse, comme pour la vidéo/objets).
        if (serviceDetectionVisage != null && indexFrame % FREQUENCE_RECONNAISSANCE_VISAGE == 0) {
            try {
                List<VisageDetecte> visagesDetectes = serviceDetectionVisage.detecter(image);
                List<RecognizedFace> visagesPrecedents = derniersVisagesReconnus;
                List<RecognizedFace> visagesReconnus = new ArrayList<>();
                for (VisageDetecte visageDetecte : visagesDetectes) {
                    RecognizedFace visageReconnu = new RecognizedFace(visageDetecte.x(), visageDetecte.y(), visageDetecte.width(), visageDetecte.height());
                    RecognizedFace visagePrecedentProche = SuiviVisageUtils.trouverVisagePrecedentProche(visagesPrecedents, visageReconnu, DISTANCE_MAX_SUIVI_VISAGE);
                    if (visagePrecedentProche != null && visagePrecedentProche.getName() != null) {
                        visageReconnu.setName(visagePrecedentProche.getName());
                    } else {
                        visageReconnu.setName(serviceReconnaissanceVisage.identifier(image, visageDetecte));
                    }
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

        if (publicationVideoAutorisee()) {
            dernierePublicationVideoMs = System.currentTimeMillis();

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

    /**
     * Indique si l'image courante doit être publiée sur le WebSocket.
     * <p>
     * Deux conditions, toutes deux nécessaires pour que le tampon d'envoi de la session ne
     * déborde plus : quelqu'un doit regarder (sinon l'encodage base64 est du pur gaspillage),
     * et l'intervalle minimal entre deux images doit être écoulé. Sans cette cadence, la
     * boucle de capture produit plus vite que le lien n'écoule, et rien en aval ne freine le
     * producteur : le tampon se remplit jusqu'à la limite et Spring ferme la session.
     * <p>
     * La détection et la reconnaissance de visages, elles, continuent de tourner à leur
     * rythme même sans abonné : elles pilotent le comportement du robot, pas l'affichage.
     */
    private boolean publicationVideoAutorisee() {
        boolean abonne = registreAbonnesWebsocket != null
                && registreAbonnesWebsocket.aAuMoinsUnAbonne(DESTINATION_VIDEO);
        if (abonne != fluxVideoDiffuse) {
            fluxVideoDiffuse = abonne;
            logger.info(abonne
                    ? "Flux vidéo démarré (au moins un client abonné à {})"
                    : "Flux vidéo en veille (plus aucun client abonné à {})", DESTINATION_VIDEO);
        }
        if (!abonne) {
            return false;
        }
        long intervalleMinimalMs = 1000L / Math.max(1, robotConfig.fpsFluxVideo());
        return System.currentTimeMillis() - dernierePublicationVideoMs >= intervalleMinimalMs;
    }

    /**
     * Paramètres d'encodage JPEG correspondant à la qualité configurée, reconstruits à la
     * volée si elle a changé (la configuration est rechargée à chaud).
     */
    private MatOfInt parametresJpeg() {
        int qualite = robotConfig.qualiteJpegFluxVideo();
        if (qualite != qualiteJpegCourante) {
            parametresJpeg = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, qualite);
            qualiteJpegCourante = qualite;
            logger.info("Qualité JPEG du flux vidéo : {}", qualite);
        }
        return parametresJpeg;
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
