package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Mat;

import java.util.List;

/**
 * Interface de détection de visages dans une image.
 */
public interface ServiceDetectionVisage {

    /**
     * Détecte les visages présents dans une image.
     *
     * @param image l'image à analyser
     * @return la liste des visages détectés (vide si aucun)
     */
    List<VisageDetecte> detecter(Mat image);
}
