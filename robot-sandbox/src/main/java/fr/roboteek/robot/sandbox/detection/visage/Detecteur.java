package fr.roboteek.robot.sandbox.detection.visage;

import org.bytedeco.javacpp.opencv_core.Mat;

public interface Detecteur<T> {

    /**
     * Détecte un objet ou une information dans une image
     *
     * @param matImage la matrice de l'image à analyser
     * @return les informations issues de la détection
     */
    T detecter(Mat matImage);
}
