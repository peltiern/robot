package fr.roboteek.robot.memoire;

import java.util.List;

import org.apache.log4j.Logger;
import org.openimaj.image.FImage;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.ml.annotation.ScoredAnnotation;
import org.openimaj.util.pair.IndependentPair;

import fr.roboteek.robot.util.callback.AsyncCallback;

/**
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class AbstractReconnaissanceFaciale {
    
    private Logger logger = Logger.getLogger(AbstractReconnaissanceFaciale.class);

    public synchronized void reconnaitre(final FImage image, final AsyncCallback<IndependentPair<DetectedFace, ScoredAnnotation<String>>> callback) {
        final Thread threadReconnaissance = new Thread("Reconnaissance") {
            @Override
            public void run() {
                // Lancement de la reconnaissance
                logger.debug("Apprentissage de visage en cours ...");
                final IndependentPair<DetectedFace, ScoredAnnotation<String>> visagePlusGrand = reconnaitre(image);
                if (visagePlusGrand != null) {
                    System.out.println("Visage reconnu : " + visagePlusGrand.getSecondObject() + ", confidence = " + visagePlusGrand.getFirstObject().getConfidence());
                    callback.onSuccess(visagePlusGrand);
                } else {
                    callback.onSuccess(null);
                }
                logger.debug("Fin de l'apprentissage de visage");
            }
        };
        threadReconnaissance.start();
    }
    
    public synchronized void apprendreVisagesPersonne(final List<FImage> listeVisages, final String prenomPersonne, final AsyncCallback<Void> callback) {
        final Thread threadApprentissage = new Thread("Apprentissage Visage") {
            @Override
            public void run() {
                System.out.println("Apprentissage de visages en cours ...");
                apprendreVisagesPersonne(listeVisages, prenomPersonne);
                System.out.println("Fin de l'apprentissage de visages.");
                callback.onSuccess(null);
            }
        };
        threadApprentissage.start();
    }
    
    protected abstract IndependentPair<DetectedFace, ScoredAnnotation<String>> reconnaitre(final FImage image);
    
    protected abstract void apprendreVisagesPersonne(final List<FImage> listeVisages, final String prenomPersonne);
}
