package fr.roboteek.robot.memoire;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.processing.face.alignment.RotateScaleAligner;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.face.recognition.EigenFaceRecogniser;
import org.openimaj.image.processing.face.recognition.FaceRecognitionEngine;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.pair.IndependentPair;

/**
 * Classe permettant la reconnaissance faciale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ReconnaissanceFacialeEigenface extends ReconnaissanceFaciale {
    
    /** Moteur de reconnaissance. */
    private FaceRecognitionEngine<KEDetectedFace, String>  moteurReconnaissance;

    /** Fichier de sauvegarde du moteur de reconnaissance. */
    private File fichierReconnaissanceFaciale;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(ReconnaissanceFacialeEigenface.class);

    public ReconnaissanceFacialeEigenface() {
        // Récupération ou création du moteur de reconnaissance
        fichierReconnaissanceFaciale = new File(System.getProperty("robot.dir") + File.separator + "reconnaissance-faciale" + File.separator + "moteurReconnaissanceEigenFace.sav");
        if (!fichierReconnaissanceFaciale.exists()) {
            // Détecteur
            final FaceDetector<KEDetectedFace, FImage> detector = new FKEFaceDetector(new HaarCascadeDetector(80));
            // Reconnaissance
            final EigenFaceRecogniser<KEDetectedFace, String> recogniser = EigenFaceRecogniser.create(20, new RotateScaleAligner(), 1, DoubleFVComparison.EUCLIDEAN, 0.9f);
            moteurReconnaissance = FaceRecognitionEngine.create(detector, recogniser);
        } else {
            try {
                moteurReconnaissance = FaceRecognitionEngine.load(fichierReconnaissanceFaciale);
            } catch (IOException e) {
                logger.error("Erreur lors du chargement du fichier de reconnaissance", e);
            }
        }
    }

    @Override
    public IndependentPair<DetectedFace, ScoredAnnotation<String>> reconnaitre(FImage image) {
        logger.debug("Lancement de la reconnaissance : liste personnes reconnues = " + moteurReconnaissance.getRecogniser().listPeople());
        IndependentPair<DetectedFace, ScoredAnnotation<String>> visagePlusGrand = null;
        if (moteurReconnaissance.getRecogniser().listPeople().size() > 0) {
            final List<IndependentPair<KEDetectedFace, ScoredAnnotation<String>>> listeVisagesReconnus = moteurReconnaissance.recogniseBest(image);
            if (listeVisagesReconnus != null && !listeVisagesReconnus.isEmpty()) {
                // Recherche du visage le plus grand
                double aireVisagePlusGrand = 0d;
                System.out.println("Nb personnes reconnus = " + listeVisagesReconnus.size());
                for (IndependentPair<KEDetectedFace,ScoredAnnotation<String>> resultat : listeVisagesReconnus) {
                    DisplayUtilities.display(resultat.getFirstObject().getFacePatch());
                    double aireVisage = resultat.getFirstObject().getBounds().calculateArea();
                    if (aireVisage > aireVisagePlusGrand) {
                        aireVisagePlusGrand = aireVisage;
                        visagePlusGrand = new IndependentPair<DetectedFace, ScoredAnnotation<String>>(resultat.getFirstObject(), resultat.getSecondObject());
                    }
                }
            }
        }
        return visagePlusGrand;
    }

    @Override
    public void apprendreVisagesPersonne(List<FImage> listeVisages, String prenomPersonne) {
        for (FImage visage : listeVisages) {
            moteurReconnaissance.train(prenomPersonne.toLowerCase(), visage);
        }
        try {
            moteurReconnaissance.save(fichierReconnaissanceFaciale);
        } catch (IOException e) {
           logger.error("Erreur lors de la sauvegarde du fichier de reconnaissance", e);
        }
    }

}
