package fr.roboteek.robot.poc;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.memoire.longterme.BaseMemoire;
import fr.roboteek.robot.memoire.longterme.personne.Personne;
import fr.roboteek.robot.memoire.longterme.personne.PersonneRepository;
import fr.roboteek.robot.memoire.longterme.visage.VisageConnuRepository;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceDetectionVisage;
import fr.roboteek.robot.services.providers.opencv.face.OpenCvServiceReconnaissanceVisage;
import fr.roboteek.robot.services.vision.face.VisageDetecte;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.sql.DataSource;

import java.io.File;
import java.util.List;

/**
 * Utilitaire jetable : enregistre un ou plusieurs visages de référence dans la mémoire longue,
 * en attendant l'import de photos par l'interface, qui le rendra inutile.
 * <p>
 * Usage : {@code java -jar robot-core-seed.jar <nom1> <image1.jpg> [<nom2> <image2.jpg> ...]}
 * (nécessite de pointer temporairement {@code mainClass} vers cette classe dans le pom,
 * comme pour {@link FaceRecognitionPoc}).
 * <p>
 * À exécuter avec le même volume que le conteneur de production (~/Robot/Programme
 * monté sur /Robot/Programme, ROBOT_HOME positionné) pour écrire dans la même base
 * que celle relue ensuite par {@code CapteurVisionWebSocketGrpc}.
 */
public class SeedVisagesConnus {

    public static void main(String[] args) {
        if (args.length < 2 || args.length % 2 != 0) {
            System.err.println("Usage: SeedVisagesConnus <prénom1> <image1.jpg> [<prénom2> <image2.jpg> ...]");
            System.exit(1);
        }

        OpenCV.loadLocally();

        // La même base que celle du robot, ouverte à part : SQLite l'accepte, le robot n'a donc
        // pas besoin d'être arrêté.
        DataSource memoire = BaseMemoire.sourceVers(
                new File(Constantes.DOSSIER_MEMOIRE, "memoire.db").getAbsolutePath());
        BaseMemoire.appliquerLeSchema(memoire);
        VisageConnuRepository repository = new VisageConnuRepository(memoire);
        PersonneRepository personneRepository = new PersonneRepository(memoire);
        String cheminModeleSFace = Constantes.DOSSIER_VISAGE + File.separator + "face_recognition_sface_2021dec.onnx";
        OpenCvServiceReconnaissanceVisage reconnaissance = new OpenCvServiceReconnaissanceVisage(cheminModeleSFace, repository);

        for (int i = 0; i < args.length; i += 2) {
            String prenom = args[i];
            String imagePath = args[i + 1];

            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                System.err.println("Image illisible, ignorée : " + imagePath);
                continue;
            }

            List<VisageDetecte> visages = OpenCvServiceDetectionVisage.getInstance().detecter(image);
            if (visages.isEmpty()) {
                System.err.println("Aucun visage détecté, ignoré : " + imagePath);
                continue;
            }

            Personne personne = personnePourPrenom(personneRepository, prenom);
            repository.ajouter(personne.id(), reconnaissance.extraireEmbedding(image, visages.get(0)));
            System.out.println("Visage enregistré : " + prenom + " (" + imagePath + ")");
        }

    }

    /**
     * Retrouve la personne portant ce prénom, ou la crée. Le rapprochement se fait sur le prénom
     * faute de mieux — c'est la seule prise qu'offre la ligne de commande — ce qui suffit à cet
     * utilitaire mais ne saurait servir de règle ailleurs : le prénom n'identifie personne.
     */
    private static Personne personnePourPrenom(PersonneRepository personneRepository, String prenom) {
        for (Personne personne : personneRepository.toutes()) {
            if (personne.prenom().equalsIgnoreCase(prenom)) {
                return personne;
            }
        }
        Personne nouvelle = Personne.nouvelle(prenom);
        personneRepository.enregistrer(nouvelle);
        return nouvelle;
    }
}
