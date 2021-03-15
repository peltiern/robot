package fr.roboteek.robot.sandbox.deeplearning;

import org.openimaj.image.MBFImage;

import java.util.List;

/**
 * Détecteur d'objets à partir d'une image.
 *
 * @param <T> type des objets détectés
 */
public abstract class ObjectDetector<T> {

    /**
     * Détecte des objets dans une image.
     *
     * @param image l'image
     * @return la liste des objets détectés.
     */
    public abstract List<DetectedObject<T>> detect(MBFImage image);

}
