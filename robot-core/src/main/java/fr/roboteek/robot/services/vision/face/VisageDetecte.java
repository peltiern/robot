package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Mat;

/**
 * Visage détecté dans une image par {@link ServiceDetectionVisage}.
 * <p>
 * {@code ligneBrute} est la ligne (15 colonnes : bbox + 5 points caractéristiques + score)
 * telle que renvoyée par {@code FaceDetectorYN.detect()}, nécessaire telle quelle à
 * {@code FaceRecognizerSF.alignCrop()} pour la reconnaissance.
 */
public record VisageDetecte(int x, int y, int width, int height, float score, Mat ligneBrute) {
}
