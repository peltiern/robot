package fr.roboteek.robot.services.vision.face;

/**
 * Visage détecté dans une image par {@link ServiceDetectionVisage}.
 * <p>
 * {@code ligneBrute} est la ligne rendue par {@code FaceDetectorYN.detect()} (15 colonnes : bbox,
 * 5 points caractéristiques, score), dont {@code FaceRecognizerSF.alignCrop()} a besoin telle
 * quelle.
 * <p>
 * En {@code float[]} et non en {@code Mat} : un {@code Mat} est une poignée vers de la mémoire
 * native qu'OpenCV ne libère qu'au passage du ramasse-miettes, et la faire circuler dans un record
 * revenait à n'en confier la durée de vie à personne. C'est le service de reconnaissance qui en
 * rebâtit un, le temps de son appel.
 *
 * @param ligneBrute les 15 colonnes de la détection ; {@code null} accepté hors reconnaissance
 */
public record VisageDetecte(int x, int y, int width, int height, float score, float[] ligneBrute) {
}
