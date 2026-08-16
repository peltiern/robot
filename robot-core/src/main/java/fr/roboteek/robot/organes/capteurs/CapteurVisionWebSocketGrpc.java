package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.memoire.DetectedObject;
import fr.roboteek.robot.memoire.ObjectDetectionResponse;
import fr.roboteek.robot.memoire.RecognizedFace;
import fr.roboteek.robot.memoire.VisionArtificiellePythonGrpc;
import fr.roboteek.robot.memoire.personne.Personne;
import fr.roboteek.robot.memoire.personne.PersonneRepository;
import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceDetectionVisage;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.ServiceDetectionVisage;
import fr.roboteek.robot.services.vision.face.ServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import fr.roboteek.robot.spring.server.websocket.RegistreAbonnesWebsocket;
import fr.roboteek.robot.systemenerveux.event.DemandeEnrolementEvent;
import fr.roboteek.robot.systemenerveux.event.EnrolementTermineEvent;
import fr.roboteek.robot.systemenerveux.event.VideoEvent;
import fr.roboteek.robot.systemenerveux.event.VisagePercu;
import fr.roboteek.robot.systemenerveux.event.VisagePercuEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.webcam.SuiviVisageUtils;
import fr.roboteek.robot.util.webcam.VisageSuivi;
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
import java.util.ArrayList;
import java.util.Comparator;
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
public class CapteurVisionWebSocketGrpc extends AbstractOrganeWithThread implements SmartLifecycle, OrganeSurveille {

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
     * Rayon de suivi, <b>exprimé en largeurs du visage détecté</b> : sous cette distance de
     * centroïde, un visage est considéré comme le même qu'à la frame précédente et on réutilise
     * son nom sans relancer SFace (alignCrop + feature + comparaison à toute la base coûtent
     * ~68 ms par visage, voir fr.roboteek.robot.poc.FaceRecognitionPoc).
     * Seuls les visages déjà identifiés (nom non nul) sont ainsi suivis : un visage
     * encore inconnu est retenté à chaque frame throttlée, pour lui laisser une chance
     * d'être reconnu si les conditions (angle, éclairage) s'améliorent entre-temps.
     * <p>
     * Relatif et non absolu, parce que c'est la seule échelle qui ait un sens : un visage proche
     * occupe 200 px et se déplace de plusieurs dizaines de pixels d'une frame à l'autre, un
     * visage lointain en occupe 40 et bouge d'autant moins. Les valeurs absolues essayées avant
     * (40 px, puis 80) étaient trop serrées dans un cas et trop larges dans l'autre.
     */
    private static final double RAYON_SUIVI_EN_LARGEURS_VISAGE = 0.7;

    /**
     * Écart de taille au-delà duquel deux visages proches ne sont pas le même (voir
     * {@link SuiviVisageUtils}) : ce qui sépare une photo qu'on retire d'un visage qui prend sa
     * place, c'est la taille, pas la position.
     */
    private static final double RAPPORT_TAILLE_MAX_SUIVI = 1.6;

    /**
     * Durée pendant laquelle les visages du dernier cycle restent une référence valable, même si
     * la détection n'a rien vu entre-temps.
     * <p>
     * <b>C'est ce qui permet d'enjamber les trous de détection</b>, mesurés de 100 à 250 ms sur le
     * robot. Sans elle, la mémoire du suivi était perdue à chaque clignotement, la personne
     * redevenait un visage tout neuf, et une présence d'inconnu se constituait en parallèle de
     * quelqu'un pourtant reconnu — assez pour déclencher une présentation. Constaté le 2026-08-12
     * sur Einstein, reconnu puis abordé.
     * <p>
     * Elle ne dit rien de la <b>fraîcheur</b> des identités portées par ces visages : c'est
     * {@link SuiviVisageUtils} qui s'en charge, et c'était la confusion de la première version.
     */
    private static final long REMANENCE_POSITIONS_MS = 1500;

    /**
     * Nombre d'empreintes relevées pour apprendre un visage.
     * <p>
     * Plusieurs et non une seule : une empreinte unique, prise de trois quarts ou à contre-jour,
     * et la personne n'est plus jamais reconnue. Les cycles étant espacés d'environ un tiers de
     * seconde, cinq empreintes couvrent près de deux secondes de menus changements de pose.
     */
    private static final int EMPREINTES_PAR_ENROLEMENT = 5;

    /**
     * Délai au bout duquel on renonce à compléter un enrôlement.
     * <p>
     * Indispensable : la personne peut se détourner ou partir entre la demande et la prise. Sans
     * échéance, l'activité qui attend le résultat resterait suspendue et le cerveau avec elle.
     */
    private static final long DUREE_MAX_ENROLEMENT_MS = 5000;

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

    /**
     * Visages du dernier cycle de reconnaissance, avec leur identité : base du suivi par
     * centroïde d'un cycle au suivant.
     */
    private List<VisageSuivi> derniersVisagesSuivis;

    /** Instant du dernier cycle ayant identifié quelqu'un, qui date {@link #derniersVisagesSuivis}. */
    private long instantDerniersVisagesSuivis = 0;

    /** Boîtes des mêmes visages, telles que diffusées dans le flux vidéo. */
    private List<RecognizedFace> derniersVisagesReconnus;

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

    /**
     * Enrôlement en cours, {@code null} s'il n'y en a pas. Posé par le thread de l'évènement,
     * consommé par la boucle de capture — d'où le {@code volatile}.
     */
    private volatile Enrolement enrolementEnCours;

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
     * Mémoire des personnes : la reconnaissance ne rend qu'un identifiant, le prénom se
     * retrouve ici.
     */
    @Autowired
    private PersonneRepository personneRepository;

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
            // Signe de vie : une image a été lue et traitée. Une webcam qui se tait fige la boucle
            // sur read(), sans que rien d'autre ne le signale.
            battement();
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
                long maintenant = System.currentTimeMillis();
                List<VisageSuivi> visagesPrecedents = identitesEncoreUtilisables();
                List<VisageSuivi> visagesSuivis = new ArrayList<>();
                for (VisageDetecte visageDetecte : visagesDetectes) {
                    RecognizedFace boite = new RecognizedFace(visageDetecte.x(), visageDetecte.y(), visageDetecte.width(), visageDetecte.height());
                    VisageSuivi visagePrecedentProche = SuiviVisageUtils.trouverVisagePrecedentProche(
                            visagesPrecedents, boite, RAYON_SUIVI_EN_LARGEURS_VISAGE * visageDetecte.width(), RAPPORT_TAILLE_MAX_SUIVI);
                    visagesSuivis.add(SuiviVisageUtils.identifier(boite, visagePrecedentProche, maintenant,
                            () -> personneReconnue(image, visageDetecte)));
                }
                // Seuls les visages identifiés servent au suivi — un précédent sans identité
                // n'évite aucun calcul — et un cycle qui n'identifie personne ne doit pas effacer
                // ce qu'on savait : c'est précisément le trou de détection qu'il faut enjamber.
                List<VisageSuivi> visagesIdentifies = visagesSuivis.stream().filter(VisageSuivi::estIdentifie).toList();
                if (!visagesIdentifies.isEmpty()) {
                    derniersVisagesSuivis = visagesIdentifies;
                    instantDerniersVisagesSuivis = System.currentTimeMillis();
                }
                derniersVisagesReconnus = visagesSuivis.stream().map(VisageSuivi::boite).toList();
                publierVisagesPercus(visagesSuivis);
                poursuivreEnrolement(visagesDetectes);
            } catch (RuntimeException e) {
                derniersVisagesSuivis = null;
                instantDerniersVisagesSuivis = 0;
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
     * Identités du dernier cycle utile, tant qu'elles ne sont pas trop vieilles pour servir.
     */
    private List<VisageSuivi> identitesEncoreUtilisables() {
        if (derniersVisagesSuivis == null
                || System.currentTimeMillis() - instantDerniersVisagesSuivis > REMANENCE_POSITIONS_MS) {
            return null;
        }
        return derniersVisagesSuivis;
    }

    /**
     * Prend en charge une demande d'apprentissage de visage.
     * <p>
     * Le travail n'est pas fait ici : ce listener s'exécute sur le thread de l'émetteur, alors que
     * les empreintes se relèvent sur les images à venir, dans la boucle de capture. On y dépose
     * donc seulement l'intention, que {@link #poursuivreEnrolement} honorera cycle après cycle.
     */
    @EventListener
    public void handleDemandeEnrolementEvent(DemandeEnrolementEvent demandeEnrolementEvent) {
        String idPersonne = demandeEnrolementEvent.getIdPersonne();
        if (!running || serviceDetectionVisage == null || serviceReconnaissanceVisage == null) {
            // Répondre tout de même : celui qui attend ne doit jamais rester suspendu parce que
            // les modèles n'ont pas pu être chargés.
            logger.warn("Enrôlement impossible : la reconnaissance de visages n'est pas disponible");
            applicationEventPublisher.publishEvent(new EnrolementTermineEvent(idPersonne, 0, false));
            return;
        }
        logger.info("Enrôlement demandé pour la personne {}", idPersonne);
        enrolementEnCours = new Enrolement(idPersonne, System.currentTimeMillis() + DUREE_MAX_ENROLEMENT_MS);
    }

    /**
     * Relève une empreinte de plus, et conclut quand le compte y est ou que le temps est écoulé.
     * <p>
     * Les empreintes ne sont écrites en base qu'à la fin, en une fois : un enrôlement interrompu
     * ne doit pas laisser une personne à moitié apprise, reconnue une fois sur trois.
     */
    private void poursuivreEnrolement(List<VisageDetecte> visagesDetectes) {
        Enrolement enrolement = enrolementEnCours;
        if (enrolement == null) {
            return;
        }
        visagesDetectes.stream()
                // Le plus gros visage, donc le plus proche : c'est à celui-là qu'on parle.
                .max(Comparator.comparingLong(visage -> (long) visage.width() * visage.height()))
                .ifPresent(visage -> enrolement.empreintes.add(serviceReconnaissanceVisage.extraireEmbedding(image, visage)));

        boolean compteAtteint = enrolement.empreintes.size() >= EMPREINTES_PAR_ENROLEMENT;
        if (!compteAtteint && System.currentTimeMillis() < enrolement.echeanceMs) {
            return;
        }
        enrolementEnCours = null;
        boolean reussi = !enrolement.empreintes.isEmpty();
        if (reussi) {
            serviceReconnaissanceVisage.enrolerPersonne(enrolement.idPersonne, enrolement.empreintes);
            logger.info("Personne {} apprise : {} empreinte(s)", enrolement.idPersonne, enrolement.empreintes.size());
        } else {
            logger.warn("Enrôlement de la personne {} abandonné : aucun visage vu à temps", enrolement.idPersonne);
        }
        applicationEventPublisher.publishEvent(
                new EnrolementTermineEvent(enrolement.idPersonne, enrolement.empreintes.size(), reussi));
    }

    /** Empreintes relevées jusqu'ici pour une personne, et l'instant où l'on renonce. */
    private static final class Enrolement {

        private final String idPersonne;
        private final long echeanceMs;
        private final List<float[]> empreintes = new ArrayList<>();

        private Enrolement(String idPersonne, long echeanceMs) {
            this.idPersonne = idPersonne;
            this.echeanceMs = echeanceMs;
        }
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
}
