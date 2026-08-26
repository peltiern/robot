package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnu;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import fr.roboteek.robot.services.vision.face.ServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.objdetect.FaceRecognizerSF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static fr.roboteek.robot.configuration.Configurations.robotConfig;

/**
 * Reconnaissance de visages via SFace (OpenCV), CPU pur — compare l'embedding du
 * visage détecté à ceux des visages connus ({@link VisageConnuRepository}).
 * <p>
 * Seuil de similarité cosine : 0.363 (recommandation OpenCV Zoo pour SFace).
 * <p>
 * Ce n'est délibérément pas un composant Spring : le modèle ONNX peut manquer sur une
 * installation neuve, et l'échec ferait alors tomber le contexte entier au lieu de priver le
 * robot de la seule reconnaissance. L'organe de vision le charge lui-même, et se contente d'un
 * avertissement s'il n'y arrive pas.
 * <p>
 * <b>Instance unique, et les méthodes qui touchent au reconnaisseur sont donc synchronisées</b>,
 * pour la même raison que {@link OpenCvServiceDetectionVisage} : la boucle vidéo n'est plus seule
 * à s'en servir depuis que l'interface web importe des photos, et les objets dnn d'OpenCV ne
 * supportent pas deux threads à la fois.
 */
public class OpenCvServiceReconnaissanceVisage implements ServiceReconnaissanceVisage {

    private static final Logger logger = LoggerFactory.getLogger(OpenCvServiceReconnaissanceVisage.class);

    private static final String NOM_MODELE = "face_recognition_sface_2021dec.onnx";
    private static final double SEUIL_COSINE = 0.363;

    /**
     * Un refus de profil journalisé sur cent. La reconnaissance tourne quelques fois par seconde :
     * tout dire noierait le journal, ne rien dire laisse croire que SFace a cherché et n'a pas
     * trouvé, alors qu'il n'a même pas été appelé.
     */
    private static final int PERIODE_JOURNAL_PROFIL = 100;

    private static OpenCvServiceReconnaissanceVisage instance;

    private final FaceRecognizerSF reconnaisseur;
    private final VisageConnuRepository visageConnuRepository;

    /** Voir {@link #empreintesConnues()}. */
    private EmpreintesConnues empreintesConnues;

    /** Visages écartés pour cause de profil depuis le démarrage. Voir {@link #PERIODE_JOURNAL_PROFIL}. */
    private int visagesEcartesDeProfil;

    /** Permet d'injecter un modèle et une base isolés (utilisé par les tests). */
    public OpenCvServiceReconnaissanceVisage(String cheminModele, VisageConnuRepository visageConnuRepository) {
        reconnaisseur = FaceRecognizerSF.create(cheminModele, "");
        this.visageConnuRepository = visageConnuRepository;
    }

    /**
     * Charge le service au premier appel, puis le rend tel quel.
     *
     * @param visageConnuRepository le dépôt, ignoré si le service est déjà chargé
     */
    public static synchronized OpenCvServiceReconnaissanceVisage getInstance(VisageConnuRepository visageConnuRepository) {
        if (instance == null) {
            instance = new OpenCvServiceReconnaissanceVisage(
                    Constantes.DOSSIER_VISAGE + File.separator + NOM_MODELE, visageConnuRepository);
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Un visage trop de profil est rendu inconnu <b>sans passer par SFace</b> : l'empreinte y
     * serait de toute façon trop éloignée de celle enrôlée, et son plus proche voisin est alors
     * aussi bien quelqu'un d'autre — c'est ainsi que Nicolas, de profil, se faisait appeler Julia.
     * Le refus est donc gratuit, il économise même les ~68 ms de l'empreinte, et le suivi encaisse
     * l'absence d'identité : il garde la dernière connue quelques secondes.
     */
    @Override
    public synchronized String identifierPersonne(Mat image, VisageDetecte visage) {
        double asymetrie = visage.asymetrieDuNez();
        if (asymetrie > robotConfig().asymetrieMaximaleDuNez()) {
            journaliserLeRefusDeProfil(asymetrie);
            return null;
        }
        EmpreintesConnues connues = empreintesConnues();
        Mat embedding = calculerEmbedding(image, visage);
        try {
            String meilleurIdPersonne = null;
            double meilleurScore = SEUIL_COSINE;
            for (int i = 0; i < connues.empreintes().size(); i++) {
                double score = reconnaisseur.match(embedding, connues.empreintes().get(i), FaceRecognizerSF.FR_COSINE);
                if (score > meilleurScore) {
                    meilleurScore = score;
                    meilleurIdPersonne = connues.idsPersonne().get(i);
                }
            }
            return meilleurIdPersonne;
        } finally {
            embedding.release();
        }
    }

    /**
     * Le premier refus, puis un sur cent. Assez pour voir la porte se fermer et à quel point elle
     * est loin du seuil, sans que le journal ne raconte chaque image.
     */
    private void journaliserLeRefusDeProfil(double asymetrie) {
        visagesEcartesDeProfil++;
        if (visagesEcartesDeProfil % PERIODE_JOURNAL_PROFIL == 1) {
            logger.info("Visage écarté sans chercher qui c'est : trop de profil (asymétrie {} pour un maximum de {}) — {} depuis le démarrage",
                    String.format("%.2f", asymetrie),
                    String.format("%.2f", robotConfig().asymetrieMaximaleDuNez()),
                    visagesEcartesDeProfil);
        }
    }

    @Override
    public synchronized float[] extraireEmbedding(Mat image, VisageDetecte visage) {
        Mat embedding = calculerEmbedding(image, visage);
        try {
            float[] valeurs = new float[embedding.cols()];
            embedding.get(0, 0, valeurs);
            return valeurs;
        } finally {
            embedding.release();
        }
    }

    @Override
    public synchronized void enrolerPersonne(String idPersonne, List<float[]> empreintes) {
        empreintes.forEach(empreinte -> visageConnuRepository.ajouter(idPersonne, empreinte));
    }

    /**
     * Les empreintes de tout le monde, prêtes à comparer, rechargées seulement quand la base a
     * changé.
     * <p>
     * Avant, chaque reconnaissance relisait toute la table et rebâtissait un {@code Mat} par
     * empreinte connue — soit, pour dix personnes apprises, cinquante lectures SQLite et cinquante
     * allocations natives par visage et par image, sur le thread de capture. La reconnaissance
     * ralentissait donc à mesure que le robot faisait connaissance.
     */
    private EmpreintesConnues empreintesConnues() {
        // La version est lue AVANT la table : si une écriture se glisse entre les deux, on retient
        // un numéro périmé et l'appel suivant rechargera pour rien. Dans l'autre ordre, on
        // retiendrait un numéro à jour sur des données anciennes, et le cache ne se rafraîchirait
        // plus jamais.
        int version = visageConnuRepository.version();
        if (empreintesConnues != null && empreintesConnues.version() == version) {
            return empreintesConnues;
        }

        libererLesEmpreintesConnues();
        List<String> idsPersonne = new ArrayList<>();
        List<Mat> empreintes = new ArrayList<>();
        for (VisageConnu visageConnu : visageConnuRepository.tousLesVisages()) {
            // Une empreinte de taille inattendue ferait lever OpenCV en pleine boucle vidéo : on
            // l'écarte du cache plutôt que de la comparer.
            if (!VisageConnuRepository.estValide(visageConnu.embedding())) {
                logger.warn("Empreinte {} de taille inattendue ({}) : écartée de la reconnaissance",
                        visageConnu.id(), visageConnu.embedding() == null ? "nulle" : visageConnu.embedding().length);
                continue;
            }
            idsPersonne.add(visageConnu.idPersonne());
            empreintes.add(matDepuisEmbedding(visageConnu.embedding()));
        }
        empreintesConnues = new EmpreintesConnues(version, List.copyOf(idsPersonne), List.copyOf(empreintes));
        logger.debug("Empreintes connues rechargées : {} (version {})", empreintes.size(), version);
        return empreintesConnues;
    }

    private void libererLesEmpreintesConnues() {
        if (empreintesConnues != null) {
            empreintesConnues.empreintes().forEach(Mat::release);
            empreintesConnues = null;
        }
    }

    /**
     * Recadre le visage sur ses points caractéristiques puis en calcule l'empreinte SFace
     * (~68 ms sur Jetson Nano, voir {@code FaceRecognitionPoc}).
     *
     * @return l'empreinte, <b>à libérer par l'appelant</b>
     */
    private Mat calculerEmbedding(Mat image, VisageDetecte visage) {
        Mat ligneBrute = new Mat(1, visage.ligneBrute().length, CvType.CV_32F);
        Mat aligne = new Mat();
        try {
            ligneBrute.put(0, 0, visage.ligneBrute());
            reconnaisseur.alignCrop(image, ligneBrute, aligne);
            Mat embedding = new Mat();
            reconnaisseur.feature(aligne, embedding);
            return embedding;
        } finally {
            ligneBrute.release();
            aligne.release();
        }
    }

    private Mat matDepuisEmbedding(float[] embedding) {
        Mat mat = new Mat(1, embedding.length, CvType.CV_32F);
        mat.put(0, 0, embedding);
        return mat;
    }

    /**
     * Les empreintes connues telles qu'on les garde entre deux reconnaissances, avec le numéro de
     * version de la table dont elles sont tirées. Les deux listes sont parallèles.
     */
    private record EmpreintesConnues(int version, List<String> idsPersonne, List<Mat> empreintes) {
    }
}
