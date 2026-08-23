package fr.roboteek.robot.services.vision.face;

import org.opencv.core.Mat;

import java.util.List;

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
     * @return l'identifiant de la {@code Personne} dont l'empreinte est la plus proche, ou
     * {@code null} si aucune ne dépasse le seuil de similarité (visage inconnu)
     */
    String identifierPersonne(Mat image, VisageDetecte visage);

    /**
     * Calcule l'empreinte biométrique d'un visage détecté, sous la forme attendue par
     * {@code VisageConnuRepository.ajouter(idPersonne, embedding)}.
     *
     * @param image  l'image d'origine (le visage n'est pas encore recadré)
     * @param visage le visage détecté dont on veut l'empreinte
     * @return l'empreinte du visage
     */
    float[] extraireEmbedding(Mat image, VisageDetecte visage);

    /**
     * Rattache des empreintes à une personne : à partir de cet instant, elle est reconnue.
     * <p>
     * L'enregistrement passe par le service et non par la base, pour que celui qui enrôle n'ait
     * pas à savoir sous quelle forme une empreinte se range.
     *
     * @param idPersonne identifiant de la personne
     * @param empreintes empreintes relevées, plusieurs valant mieux qu'une — une seule prise à
     *                   contre-jour et la personne n'est plus jamais reconnue
     */
    void enrolerPersonne(String idPersonne, List<float[]> empreintes);
}
