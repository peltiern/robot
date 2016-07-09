package fr.roboteek.robot.memoire;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.feature.FloatFV;
import org.openimaj.feature.FloatFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.processing.face.alignment.CLMAligner;
import org.openimaj.image.processing.face.detection.CLMDetectedFace;
import org.openimaj.image.processing.face.detection.CLMFaceDetector;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.feature.LocalLBPHistogram;
import org.openimaj.image.processing.face.feature.comparison.FaceFVComparator;
import org.openimaj.image.processing.face.feature.comparison.FacialFeatureComparator;
import org.openimaj.image.processing.face.recognition.AnnotatorFaceRecogniser;
import org.openimaj.image.processing.face.recognition.FaceRecognitionEngine;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.ml.annotation.basic.KNNAnnotator;
import org.openimaj.util.pair.IndependentPair;

/**
 * Classe permettant la reconnaissance faciale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ReconnaissanceFacialeAnnotator extends ReconnaissanceFaciale {
    
    /** Moteur de reconnaissance. */
    protected FaceRecognitionEngine<CLMDetectedFace, String>  moteurReconnaissance;

    /** Fichier de sauvegarde du moteur de reconnaissance. */
    private File fichierReconnaissanceFaciale;
    
    /** Logger. */
    private Logger logger = Logger.getLogger(ReconnaissanceFacialeAnnotator.class);

    public ReconnaissanceFacialeAnnotator() {
        // Récupération ou création du moteur de reconnaissance
        fichierReconnaissanceFaciale = new File(System.getProperty("robot.dir") + File.separator + "reconnaissance-faciale" + File.separator + "moteurReconnaissanceAnnotator.sav");
        if (!fichierReconnaissanceFaciale.exists()) {
         // Détecteur
            final CLMFaceDetector detector = new CLMFaceDetector();
            // Reconnaissance
//            final LocalLBPHistogram.Extractor<CLMDetectedFace> extractor = new LocalLBPHistogram.Extractor<CLMDetectedFace>(new CLMAligner(), 20, 20, 8, 1);
            final LocalLBPHistogram.Extractor<CLMDetectedFace> extractor = new LocalLBPHistogram.Extractor<CLMDetectedFace>(new CLMAligner());
            final FacialFeatureComparator<LocalLBPHistogram> comparator = new FaceFVComparator<LocalLBPHistogram, FloatFV>(FloatFVComparison.EUCLIDEAN);
            final KNNAnnotator<CLMDetectedFace, String, LocalLBPHistogram> knn = KNNAnnotator.create(extractor, comparator);
            AnnotatorFaceRecogniser<CLMDetectedFace, String> recogniser = AnnotatorFaceRecogniser.create(knn);
            moteurReconnaissance = FaceRecognitionEngine.create(detector, recogniser);
        } else {
            try {
                moteurReconnaissance = FaceRecognitionEngine.load(fichierReconnaissanceFaciale);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public IndependentPair<DetectedFace, ScoredAnnotation<String>> reconnaitre(FImage image) {
        logger.debug("Lancement de la reconnaissance : liste personnes reconnues = " + moteurReconnaissance.getRecogniser().listPeople());
        IndependentPair<DetectedFace, ScoredAnnotation<String>> visagePlusGrand = null;
        if (moteurReconnaissance.getRecogniser().listPeople().size() > 0) {
            final List<IndependentPair<CLMDetectedFace, ScoredAnnotation<String>>> listeVisagesReconnus = moteurReconnaissance.recogniseBest(image);
            if (listeVisagesReconnus != null && !listeVisagesReconnus.isEmpty()) {
                // Recherche du visage le plus grand
                double aireVisagePlusGrand = 0d;
                System.out.println("Nb personnes reconnus = " + listeVisagesReconnus.size());
                for (IndependentPair<CLMDetectedFace,ScoredAnnotation<String>> resultat : listeVisagesReconnus) {
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
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

}
