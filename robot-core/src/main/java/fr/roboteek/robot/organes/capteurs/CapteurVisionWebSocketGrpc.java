package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.memoire.DetectedObject;
import fr.roboteek.robot.memoire.ObjectDetectionResponse;
import fr.roboteek.robot.memoire.RecognizedFace;
import fr.roboteek.robot.memoire.VisionArtificiellePythonGrpc;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceDetectionVisage;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.ServiceDetectionVisage;
import fr.roboteek.robot.services.vision.face.ServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDansLImage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import fr.roboteek.robot.web.websocket.RegistreAbonnesWebsocket;
import fr.roboteek.robot.systemenerveux.event.DemandeEnrolementEvent;
import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.memoire.courtterme.MemoireCourtTerme;
import fr.roboteek.robot.memoire.courtterme.PriseDeVisage;
import fr.roboteek.robot.memoire.courtterme.VisageSuivi;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;


/**
 * Capteur de vision : capture le flux de la webcam, le transmet au serveur Python
 * de vision artificielle (gRPC, détection d'objets / reconnaissance de visages) et
 * publie un {@link VideoEvent} par image.
 * <p>
 * <b>Désactivé par défaut</b> : {@code robot.capteurs.vision.enabled=true} dans
 * {@code robot.properties} pour l'activer. Le bean est toujours créé mais reste inerte tant que
 * le drapeau est faux.
 * <p>
 * Dégradation gracieuse : si aucune webcam n'est trouvée, l'organe reste inerte
 * sans faire échouer le démarrage ; si le serveur Python est absent, les appels
 * gRPC renvoient {@code null} et seule l'image est diffusée (sans détection).
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Component
public class CapteurVisionWebSocketGrpc extends AbstractOrganeWithThread implements SmartLifecycle, OrganeSurveille {

    private static final Logger logger = LoggerFactory.getLogger(CapteurVisionWebSocketGrpc.class);

    /** Largeur de la vidéo issue de la webcam. */
    private static final int LARGEUR_WEBCAM = 640;
    /** Hauteur de la vidéo issue de la webcam. */
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

    /** Capture vidéo. */
    private VideoCapture capture;

    /** Image en cours. */
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

    /**
     * Indique si le dernier {@link VisagePercuEvent} publié annonçait au moins un visage :
     * sert à n'émettre qu'un seul évènement quand le champ se vide (voir
     * {@link #publierVisagesPercus(List)}).
     */
    private boolean visagesPercusPrecedemment = false;

    /**
     * Dernière composition de visages tracée, pour ne journaliser que les changements
     * (quelqu'un arrive, est enfin reconnu, ou s'en va) et non chaque cycle.
     */
    private String derniereCompositionVisagesTracee = null;

    private ObjectDetectionResponse objectDetectionResponse;

    /**
     * Où passe le temps d'un tour de boucle, résumé en DEBUG toutes les
     * {@link #PERIODE_BILAN_CADENCE_MS} ms.
     * <p>
     * <b>Posé le 2026-09-11 parce que personne ne savait à quelle cadence tourne la vision.</b> Le
     * « 8 images par seconde » des commentaires n'est que le débit de la vidéo envoyée au HUD ; la
     * boucle, elle, va aussi vite que son étape la plus lente. Et le regard oscillait parce qu'une
     * image sur deux lui arrivait plus vieille que sa correction précédente — sans que rien ne dise
     * de combien.
     * <p>
     * La lecture webcam est le chiffre qui tranche. Une lecture qui <b>attend</b> (une trentaine de
     * ms à 30 images/s) rend une image fraîche. Une lecture <b>instantanée</b> rend une image déjà
     * en file : la boucle est plus lente que la caméra, et l'image a autant de tours de retard qu'il
     * y en a dans le tampon.
     * <p>
     * Ce qu'elle a répondu : 25 tours par seconde, au rythme de la caméra, et une image qui a 100 à
     * 250 ms quand le regard la lit. La vision n'était pas le goulot, c'était la temporisation du
     * regard, plus courte que le trajet de la tête. Gardée en DEBUG pour le jour où la détection
     * d'objets sera branchée : c'est elle qui remettra un appel coûteux dans chaque tour.
     */
    private static final long PERIODE_BILAN_CADENCE_MS = 10_000;
    private final Cumul tourDeBoucle = new Cumul();
    private final Cumul lectureWebcam = new Cumul();
    private final Cumul visages = new Cumul();
    private final Cumul detectionObjets = new Cumul();
    private long debutBilanCadenceMs = System.currentTimeMillis();

    /**
     * Ce que l'enrôlement en cours a vu passer, ou {@code null} hors enrôlement.
     * <p>
     * Volatile, et remplacé plutôt que remis à zéro : il est créé et relu depuis le thread de
     * l'activité qui demande l'apprentissage, alors que seule la boucle vidéo l'incrémente.
     * Publier une nouvelle instance suffit à ce que la boucle voie des compteurs neufs ; une
     * lecture légèrement en retard ne coûterait qu'une ligne de journal imprécise.
     */
    private volatile ComptesDePrises comptesDePrises;

    /** Configuration. */
    private RobotConfig robotConfig;

    /** Registre des abonnements WebSocket : sert à ne rien produire quand personne ne regarde. */
    @Autowired
    private RegistreAbonnesWebsocket registreAbonnesWebsocket;

    /**
     * Mémoire des personnes : la reconnaissance ne rend qu'un identifiant, le prénom se
     * retrouve ici.
     */
    @Autowired
    private PersonneRepository personneRepository;

    /**
     * Empreintes des visages connus, remises à la reconnaissance au chargement. Injectées plutôt
     * qu'ouvertes par le service lui-même : depuis SQLite, toute l'application partage un seul
     * accès à la mémoire longue.
     */
    @Autowired
    private VisageConnuRepository visageConnuRepository;

    /**
     * Ce que le robot a en tête : visages suivis, apprentissage en cours. Appelée en direct depuis
     * la boucle, et non par évènement — elle rend un résultat tout de suite, et l'image ne peut pas
     * voyager (voir {@link MemoireCourtTerme}).
     */
    @Autowired
    private MemoireCourtTerme memoireCourtTerme;

    /**
     * Horodatage de la dernière image publiée, pour cadencer le flux en temps réel plutôt
     * qu'en nombre de frames : la cadence de la webcam varie (luminosité, charge CPU), une
     * frame sur N ne borne donc pas le débit réellement envoyé.
     */
    private long dernierePublicationVideoMs = 0;

    /** Indique si le flux vidéo est en cours de diffusion, pour ne tracer que les transitions. */
    private boolean fluxVideoDiffuse = false;

    /**
     * Paramètres d'encodage JPEG, reconstruits uniquement quand la qualité configurée change
     * (objet natif OpenCV : ni instanciable avant {@code OpenCV.loadLocally()}, ni à recréer
     * à chaque image).
     */
    private MatOfInt parametresJpeg;

    /** Qualité JPEG ayant servi à construire {@link #parametresJpeg}. */
    private int qualiteJpegCourante = -1;

    /** Flag indiquant de stopper le thread de capture. */
    private volatile boolean stopperThread = false;

    /** Flag de démarrage de l'organe (cycle de vie Spring). */
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
            serviceReconnaissanceVisage = OpenCvServiceReconnaissanceVisage.getInstance(visageConnuRepository);
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
            long debutTour = System.nanoTime();
            if (!capture.read(image)) {
                break;
            }
            lectureWebcam.ajouter(System.nanoTime() - debutTour);
            traiterImageEnCours();
            tourDeBoucle.ajouter(System.nanoTime() - debutTour);
            // Signe de vie : une image a été lue et traitée. Une webcam qui se tait fige la boucle
            // sur read(), sans que rien d'autre ne le signale.
            battement();
            dresserLeBilanDeCadence();
        }
    }

    @Override
    public void arreter() {
        stopperThread = true;
        // Le release appartient au thread de capture, et il faut donc l'attendre. Le faire ici
        // pendant que read() est en cours fait lever OpenCV — « Unknown array type in function
        // cvarrToMat » — à chaque arrêt du robot : le descripteur est refermé sous les pieds d'une
        // lecture en vol. Sans conséquence, le robot s'arrêtait proprement juste après, mais une
        // trace d'exception dans un journal d'arrêt propre finit par être ignorée le jour où elle
        // dit quelque chose.
        // L'attente coûte au plus un tour de boucle — pas une image à 8 par seconde : ce débit-là
        // n'est que celui de la vidéo envoyée au HUD, et un tour comprend aussi la reconnaissance
        // et l'appel au serveur de détection. La seconde de patience est un garde-fou pour la
        // webcam qui se tait : au-delà on relâche quand même, parce qu'un arrêt bloqué serait pire
        // qu'une exception.
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                    // Ce que le pilote annonce, pas ce qu'il fait : -1 ou 0 veut dire « non exposé ».
                    logger.info("Webcam : {} images/s annoncées, tampon de {} image(s)",
                            capture.get(Videoio.CAP_PROP_FPS), capture.get(Videoio.CAP_PROP_BUFFERSIZE));
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
            long debutVisages = System.nanoTime();
            try {
                List<VisageDetecte> visagesDetectes = serviceDetectionVisage.detecter(image);
                // Le suivi est une mémoire court terme, pas une affaire de capteur : on lui passe
                // de quoi reconnaître — la fonction tient l'image, qui ne sort pas d'ici — et il
                // rend les visages avec leur identité.
                List<VisageSuivi> visagesSuivis = memoireCourtTerme.suivreLesVisages(
                        visagesDetectes, visage -> personneReconnue(image, visage));
                publierVisagesPercus(visagesSuivis);
                memoireCourtTerme.avancerLEnrolement(() -> priseDuVisageLePlusProche(visagesDetectes));
            } catch (RuntimeException e) {
                memoireCourtTerme.oublierLesVisages();
                if (indexFrame % 100 == 0) {
                    logger.warn("Reconnaissance de visages indisponible (le flux vidéo continue) : {}", e.getMessage());
                }
            }
            // Le regard compris : il écoute les visages perçus sur ce thread-ci, en synchrone.
            visages.ajouter(System.nanoTime() - debutVisages);
        }

////        if (indexFrame % 3 == 0 || objectDetectionResponse == null) {
        // Détection optionnelle : elle ne doit jamais interrompre le flux vidéo
        // (serveur Python absent = null ; erreur = warn limité, puis on continue).
        long debutDetection = System.nanoTime();
        try {
            objectDetectionResponse = visionArtificiellePythonGrpc.detectObjects(ba);
        } catch (RuntimeException e) {
            objectDetectionResponse = null;
            if (indexFrame % 100 == 0) {
                logger.warn("Détection indisponible (le flux vidéo continue) : {}", e.getMessage());
            }
        }
        detectionObjets.ajouter(System.nanoTime() - debutDetection);
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
            List<RecognizedFace> boites = memoireCourtTerme.visagesVus().stream().map(VisageSuivi::boite).toList();
            videoEvent.setFaceFound(!boites.isEmpty());
            videoEvent.setFaces(boites);
            if (objectDetectionResponse != null) {
                videoEvent.setObjectFound(objectDetectionResponse.isObjectFound());
                videoEvent.setObjects(objectDetectionResponse.getObjects());
            }
            applicationEventPublisher.publishEvent(videoEvent);
            long fin = System.currentTimeMillis();
            if (CollectionUtils.isNotEmpty(boites)) {
                logger.debug("({} ms) visages : {}", fin - debut, boites.stream().map(DetectedObject::getName).collect(Collectors.joining(",")));
            }
            if (objectDetectionResponse != null && CollectionUtils.isNotEmpty(objectDetectionResponse.getObjects())) {
                logger.debug("({} ms) objets : {}", fin - debut, objectDetectionResponse.getObjects().stream().map(DetectedObject::getName).collect(Collectors.joining(",")));
            }
        }
    }

    /**
     * Prend en charge une demande d'apprentissage de visage.
     * <p>
     * L'organe ne fait ici que deux choses, et ce sont les deux seules qui le regardent : dire si
     * sa vision est disponible — lui seul le sait — et passer la main à la mémoire court terme,
     * qui tient la procédure. Le relevé, lui, se fera cycle après cycle dans la boucle de capture.
     */
    @EventListener
    public void handleDemandeEnrolementEvent(DemandeEnrolementEvent demandeEnrolementEvent) {
        if (!running || serviceDetectionVisage == null || serviceReconnaissanceVisage == null) {
            memoireCourtTerme.renoncerALEnrolement(demandeEnrolementEvent.getIdPersonne(),
                    "la reconnaissance de visages n'est pas disponible");
            return;
        }
        String idPersonne = demandeEnrolementEvent.getIdPersonne();
        comptesDePrises = new ComptesDePrises();
        memoireCourtTerme.demarrerUnEnrolement(idPersonne, prises -> retenir(idPersonne, prises));
    }

    /**
     * Dit ce que l'apprentissage a vu passer, en une ligne.
     * <p>
     * <b>Au niveau INFO, et c'est délibéré</b> : quand un enrôlement échoue, la seule question qui
     * se pose est « le robot n'a vu personne, ou il a vu et refusé ? » — et si c'est la seconde,
     * de combien les seuils ont manqué. Mettre ça en DEBUG obligerait à reconstruire l'image
     * Docker pour le savoir, alors que les seuils, eux, se règlent à chaud. Une ligne par
     * enrôlement, c'est-à-dire quelques-unes par jour.
     */
    @EventListener
    public void handleEnrolementTermineEvent(EnrolementTermineEvent enrolementTermineEvent) {
        ComptesDePrises comptes = comptesDePrises;
        comptesDePrises = null;
        if (comptes != null) {
            logger.info("Enrôlement de {} : {}", enrolementTermineEvent.getIdPersonne(), comptes.bilan(robotConfig));
        }
    }

    /**
     * Écrit en mémoire longue ce qu'un enrôlement a relevé : les empreintes, et le portrait.
     * <p>
     * Le portrait est pris parmi les mêmes images que les empreintes, et c'est tout l'intérêt de
     * le faire ici : le robot tient déjà la personne bien cadrée, il n'y a rien à lui redemander.
     * On garde la <b>dernière</b> vignette obtenue — les premières images d'un enrôlement
     * attrapent souvent quelqu'un encore en train de se tourner vers la caméra.
     */
    private void retenir(String idPersonne, List<PriseDeVisage> prises) {
        serviceReconnaissanceVisage.enrolerPersonne(idPersonne,
                prises.stream().map(PriseDeVisage::empreinte).toList());

        prises.stream()
                .map(PriseDeVisage::vignette)
                .filter(Objects::nonNull)
                .reduce((premiere, derniere) -> derniere)
                .ifPresent(vignette -> personneRepository.enregistrerVignette(idPersonne, vignette));
    }

    /**
     * Empreinte et portrait du visage auquel le robot parle — le plus gros, donc le plus proche —,
     * ou {@code null} s'il n'y a personne, ou si ce qu'on voit de lui ne vaut pas la peine d'être
     * appris. C'est le seul endroit où l'image sert à l'enrôlement, et elle ne va pas plus loin :
     * il n'en sort que 128 flottants et un JPEG.
     * <p>
     * Rendre {@code null} ne coûte rien à l'enrôlement en cours : il réessaie à l'image suivante,
     * jusqu'à son échéance.
     */
    private PriseDeVisage priseDuVisageLePlusProche(List<VisageDetecte> visagesDetectes) {
        ComptesDePrises comptes = comptesDePrises;
        if (visagesDetectes.isEmpty()) {
            if (comptes != null) {
                comptes.sansVisage++;
            }
            return null;
        }
        return visagesDetectes.stream()
                .max(Comparator.comparingLong(visage -> (long) visage.width() * visage.height()))
                .filter(this::meriteDEtreAppris)
                .map(visage -> new PriseDeVisage(serviceReconnaissanceVisage.extraireEmbedding(image, visage),
                        VisageDansLImage.portraitJpeg(image, visage)))
                .orElse(null);
    }

    /**
     * Ce qu'on voit de ce visage vaut-il d'entrer en mémoire longue ?
     * <p>
     * Le comptage est ici et non dans la porte elle-même : c'est le seul chemin où un refus est
     * muet pour l'utilisateur — la photo importée, elle, répond dans l'interface. Quand un
     * enrôlement échoue devant la caméra, ce décompte est la seule façon de savoir si les seuils
     * sont bien réglés pour cette webcam-là.
     */
    private boolean meriteDEtreAppris(VisageDetecte visage) {
        VisageDansLImage.Qualite qualite = VisageDansLImage.qualite(image, visage);
        ComptesDePrises comptes = comptesDePrises;
        if (comptes == null) {
            return qualite.exploitable();
        }
        if (qualite.exploitable()) {
            comptes.retenue();
            return true;
        }
        // Les mesures, et pas seulement le verdict : ce qui manque pour régler un seuil, c'est de
        // combien la meilleure prise l'a raté. La netteté est remesurée — une milliseconde, et
        // seulement sur ce qu'on refuse.
        comptes.refus(qualite, visage.asymetrieDuNez(), VisageDansLImage.nettete(image, visage));
        return false;
    }

    /**
     * Publie les visages perçus, <b>sans condition d'abonné WebSocket</b>, contrairement au
     * flux vidéo : cette information pilote le comportement du robot (aller saluer quelqu'un,
     * le regarder), elle doit donc parvenir au reste du système tablette éteinte.
     * <p>
     * Un évènement part à chaque cycle de reconnaissance tant qu'au moins un visage est là,
     * puis un dernier quand le champ se vide — c'est le signal du départ. Les « toujours
     * personne » qui suivent sont tus : sinon l'évènement partirait une dizaine de fois par
     * seconde sur une pièce vide, et serait en plus rediffusé sur le WebSocket.
     */
    private void publierVisagesPercus(List<VisageSuivi> visages) {
        boolean visagesPresents = !visages.isEmpty();
        if (!visagesPresents && !visagesPercusPrecedemment) {
            return;
        }
        visagesPercusPrecedemment = visagesPresents;
        tracerCompositionVisages(visages);
        List<VisagePercu> visagesPercus = visages.stream()
                .map(visage -> new VisagePercu(visage.idPersonne(), visage.prenom(),
                        visage.boite().getX(), visage.boite().getY(),
                        visage.boite().getWidth(), visage.boite().getHeight()))
                .toList();
        applicationEventPublisher.publishEvent(new VisagePercuEvent(visagesPercus, image.width(), image.height()));
    }

    /**
     * Reconnaît la personne d'un visage détecté (SFace puis résolution en base).
     * <p>
     * Une empreinte peut désigner une personne absente de la base — enregistrement à moitié
     * fait, base des personnes effacée sans la base des visages. Le visage est alors traité
     * comme inconnu : mieux vaut redemander son prénom à quelqu'un que d'entretenir une
     * identité fantôme.
     *
     * @return la personne reconnue, ou {@code null} si le visage est inconnu
     */
    private Personne personneReconnue(Mat image, VisageDetecte visageDetecte) {
        String idPersonne = serviceReconnaissanceVisage.identifierPersonne(image, visageDetecte);
        if (idPersonne == null) {
            return null;
        }
        Personne personne = personneRepository.parId(idPersonne);
        if (personne == null) {
            logger.warn("Empreinte rattachée à une personne absente de la base ({}) : visage traité comme inconnu", idPersonne);
        }
        return personne;
    }

    /**
     * Journalise qui le robot a devant lui, uniquement quand cela change : quelqu'un arrive,
     * finit par être reconnu, ou s'en va.
     * <p>
     * En DEBUG : la perception clignote trop pour tenir dans une transcription — un visage
     * immobile produit des dizaines de lignes par minute, et la reconnaissance elle-même oscille
     * près de son seuil. Ce qui mérite le niveau INFO, c'est la rencontre décidée par le registre
     * de présence, pas la matière première dont elle est tirée.
     */
    private void tracerCompositionVisages(List<VisageSuivi> visages) {
        String composition = visages.isEmpty()
                ? "plus personne"
                : visages.stream()
                        .map(visage -> visage.estIdentifie() ? visage.prenom() : "inconnu")
                        .collect(Collectors.joining(", "));
        if (!composition.equals(derniereCompositionVisagesTracee)) {
            derniereCompositionVisagesTracee = composition;
            logger.debug("Visages perçus : {}", composition);
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

    // --- Surveillance : affichage seulement ---
    // La vision ne commande aucun moteur : son silence ne coupe rien. C'est en revanche l'organe
    // qui justifie le troisième état « éteint » — désactivé par configuration ou privé de webcam,
    // il ne démarre pas, et doit se lire comme volontairement au repos, pas comme en panne.

    @Override
    public String idOrgane() {
        return "vision";
    }

    @Override
    public String libelleOrgane() {
        return "Vision";
    }

    @Override
    public NatureOrgane nature() {
        return NatureOrgane.CAPTEUR;
    }

    @Override
    public boolean enService() {
        return running;
    }

    /**
     * Ce qu'un enrôlement a vu passer, et de combien les refus ont raté les seuils.
     * <p>
     * Les extrêmes plutôt que les moyennes : pour savoir si un seuil est trop sévère, ce qui
     * compte est la <b>meilleure</b> prise que le robot ait eue sous les yeux — s'il a vu une
     * netteté de 92 pour un seuil à 100, la réponse tient dans ces deux nombres. Une moyenne, elle,
     * serait tirée vers le bas par les images où la personne était de dos.
     * <p>
     * Champs simples et non atomiques : seule la boucle vidéo les incrémente. La lecture finale
     * vient d'un autre thread et peut la précéder d'un cycle — on y perd au pire une prise dans un
     * décompte de journal.
     */
    private static final class ComptesDePrises {

        private int retenues;
        private int sansVisage;
        private int tropFlou;
        private int tropDeProfil;
        private double meilleureAsymetrie = Double.MAX_VALUE;
        private double meilleureNettete;

        private void retenue() {
            retenues++;
        }

        private void refus(VisageDansLImage.Qualite qualite, double asymetrie, double nettete) {
            switch (qualite) {
                case TROP_FLOU -> tropFlou++;
                case TROP_DE_PROFIL -> tropDeProfil++;
                case EXPLOITABLE -> throw new IllegalArgumentException("une prise exploitable n'est pas un refus");
            }
            meilleureAsymetrie = Math.min(meilleureAsymetrie, asymetrie);
            meilleureNettete = Math.max(meilleureNettete, nettete);
        }

        private String bilan(RobotConfig reglages) {
            int ecartees = tropFlou + tropDeProfil;
            String bilan = "%d prise(s) retenue(s), %d écartée(s) (%d de profil, %d floue(s)), %d image(s) sans visage"
                    .formatted(retenues, ecartees, tropDeProfil, tropFlou, sansVisage);
            if (ecartees == 0) {
                return bilan;
            }
            return bilan + " ; au mieux : asymétrie %.2f (max %.2f), netteté %d (min %d)".formatted(
                    meilleureAsymetrie, reglages.asymetrieMaximaleDuNez(),
                    Math.round(meilleureNettete), Math.round(reglages.netteteMinimaleDuVisage()));
        }
    }

    private void dresserLeBilanDeCadence() {
        long maintenant = System.currentTimeMillis();
        long ecoule = maintenant - debutBilanCadenceMs;
        if (ecoule < PERIODE_BILAN_CADENCE_MS) {
            return;
        }
        logger.debug("Vision : {} tours en {} s — tour {} ; lecture webcam {} ; visages {} ; détection d'objets {}",
                tourDeBoucle.nombre, Math.round(ecoule / 1000d),
                tourDeBoucle.resumer(), lectureWebcam.resumer(), visages.resumer(), detectionObjets.resumer());
        tourDeBoucle.remettreAZero();
        lectureWebcam.remettreAZero();
        visages.remettreAZero();
        detectionObjets.remettreAZero();
        debutBilanCadenceMs = maintenant;
    }

    /** Durées cumulées d'une étape de la boucle, le temps d'un bilan. Seul le thread de capture y touche. */
    private static final class Cumul {

        private int nombre;
        private long totalNanos;
        private long maxNanos;

        void ajouter(long dureeNanos) {
            nombre++;
            totalNanos += dureeNanos;
            maxNanos = Math.max(maxNanos, dureeNanos);
        }

        String resumer() {
            if (nombre == 0) {
                return "jamais";
            }
            return String.format("moyenne %.0f ms, max %.0f ms (%d)", totalNanos / 1e6 / nombre, maxNanos / 1e6, nombre);
        }

        void remettreAZero() {
            nombre = 0;
            totalNanos = 0;
            maxNanos = 0;
        }
    }
}
