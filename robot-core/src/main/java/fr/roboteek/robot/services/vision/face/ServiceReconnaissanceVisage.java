package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Mat;

/**
 * Interface de reconnaissance de visages : identifie un visage déjà détecté parmi
 * les visages connus enregistrés en base.
 */
public interface ServiceReconnaissanceVisage {

    /**
     * Identifie un visage détecté.
     *
     * @param image   l'image d'origine (le visage n'est pas encore recadré)
     * @param visage  le visage détecté à identifier
     * @return le nom du visage connu le plus proche, ou {@code null} si aucun visage
     * connu ne dépasse le seuil de similarité (visage inconnu)
     */
    String identifier(Mat image, VisageDetecte visage);
}
