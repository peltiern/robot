package fr.roboteek.robot.services.providers.opencv.face;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.longterme.BaseMemoireDeTest;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test de fumée : détection + reconnaissance SFace de bout en bout, sur une base
 * de visages connus isolée (tmp), donc sans toucher à la vraie base de production.
 * Ne s'exécute que si les modèles ONNX sont présents dans {@code ROBOT_HOME/visage}
 * (voir fr.roboteek.robot.poc.FaceRecognitionPoc) — ignoré par défaut.
 */
class OpenCvServiceReconnaissanceVisageTest {

    private static final String IMAGE_AMY = "../robot-python/vision-artificielle/known-faces/0001_amy.jpg";
    private static final String IMAGE_EINSTEIN = "../robot-python/vision-artificielle/known-faces/0005_einstein.jpg";
    private static final String IMAGE_SHELDON = "../robot-python/vision-artificielle/known-faces/0002_sheldon.jpg";

    private static String cheminModeleSFace;

    @TempDir
    File dossierTemp;

    private VisageConnuRepository repository;
    private PersonneRepository personneRepository;
    private OpenCvServiceReconnaissanceVisage reconnaissance;

    @BeforeAll
    static void verifierModelesDisponibles() {
        cheminModeleSFace = Constantes.DOSSIER_VISAGE + File.separator + "face_recognition_sface_2021dec.onnx";
        assumeTrue(new File(Constantes.DOSSIER_VISAGE, "face_detection_yunet_2023mar.onnx").exists()
                        && new File(cheminModeleSFace).exists(),
                "Modèles ONNX absents de ROBOT_HOME/visage : test ignoré");
        OpenCV.loadLocally();
    }

    @BeforeEach
    void setUp() {
        DataSource memoire = BaseMemoireDeTest.dans(dossierTemp);
        repository = new VisageConnuRepository(memoire);
        personneRepository = new PersonneRepository(memoire);
        reconnaissance = new OpenCvServiceReconnaissanceVisage(cheminModeleSFace, repository);

        enregistrer("Amy", IMAGE_AMY);
        enregistrer("Einstein", IMAGE_EINSTEIN);
    }

    @Test
    void identifieLesVisagesEnregistres() {
        assertEquals("Amy", identifiantPersonnePremierVisage(IMAGE_AMY));
        assertEquals("Einstein", identifiantPersonnePremierVisage(IMAGE_EINSTEIN));
    }

    @Test
    void unVisageNonEnregistreResteInconnu() {
        assertNull(identifiantPersonnePremierVisage(IMAGE_SHELDON));
    }

    @Test
    void lEmbeddingExtraitPermetDEnrolerUnVisage() {
        // Sheldon est inconnu au départ (voir le test précédent) ; on l'enrôle avec la seule
        // empreinte extraite du service, et il doit alors se reconnaître lui-même.
        enregistrer("Sheldon", IMAGE_SHELDON);

        assertEquals("Sheldon", identifiantPersonnePremierVisage(IMAGE_SHELDON));
        assertEquals("Amy", identifiantPersonnePremierVisage(IMAGE_AMY));
    }

    private void enregistrer(String idPersonne, String cheminImage) {
        Mat image = Imgcodecs.imread(cheminImage);
        assertFalse(image.empty(), "Image de test introuvable : " + cheminImage);
        VisageDetecte visage = OpenCvServiceDetectionVisage.getInstance().detecter(image).get(0);

        // La personne d'abord : une empreinte ne peut pas désigner quelqu'un qui n'existe pas.
        // Ici l'identifiant est le prénom, ce qui rend les assertions lisibles ; ailleurs c'est
        // un UUID, le prénom n'identifiant personne.
        personneRepository.enregistrer(new Personne(idPersonne, idPersonne, null));
        repository.ajouter(idPersonne, reconnaissance.extraireEmbedding(image, visage));
    }

    private String identifiantPersonnePremierVisage(String cheminImage) {
        Mat image = Imgcodecs.imread(cheminImage);
        List<VisageDetecte> visages = OpenCvServiceDetectionVisage.getInstance().detecter(image);
        return reconnaissance.identifierPersonne(image, visages.get(0));
    }
}
