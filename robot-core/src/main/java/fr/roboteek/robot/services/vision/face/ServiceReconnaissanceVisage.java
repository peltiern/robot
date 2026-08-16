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

    /**
     * Calcule l'empreinte biométrique d'un visage détecté, sous la forme attendue par
     * {@code VisageConnuRepository.ajouter(nom, embedding)}.
     * <p>
     * C'est ce qui manquait pour enrôler quelqu'un : {@link #identifier} calculait déjà cette
     * empreinte, mais la gardait pour lui — la base ne pouvait donc être peuplée que par
     * l'utilitaire jetable {@code SeedVisagesConnus}, qui refaisait le calcul de son côté.
     *
     * @param image  l'image d'origine (le visage n'est pas encore recadré)
     * @param visage le visage détecté dont on veut l'empreinte
     * @return l'empreinte du visage
     */
    float[] extraireEmbedding(Mat image, VisageDetecte visage);
}
