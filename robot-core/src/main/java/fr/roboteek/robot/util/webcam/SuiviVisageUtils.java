package fr.roboteek.robot.util.webcam;

import fr.roboteek.robot.memoire.RecognizedFace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.geometry.euclidean.twod.Vector2D;

import java.util.List;

/**
 * Classe utilitaire pour le suivi léger de visages entre deux frames throttlées,
 * par proximité de centroïde : permet à {@code CapteurVisionWebSocketGrpc} de
 * réutiliser une identité déjà trouvée sans relancer la reconnaissance (SFace, ~68 ms/visage).
 */
public class SuiviVisageUtils {

    private SuiviVisageUtils() {
    }

    /**
     * Cherche, parmi {@code visagesPrecedents}, celui dont le centroïde est le plus
     * proche de {@code visageDetecte}, à condition que cette distance reste
     * strictement sous {@code distanceMaxPixels}.
     *
     * @return le visage précédent le plus proche, ou {@code null} si aucun n'est
     * assez proche (ou si {@code visagesPrecedents} est vide/nul)
     */
    public static VisageSuivi trouverVisagePrecedentProche(List<VisageSuivi> visagesPrecedents, RecognizedFace visageDetecte, double distanceMaxPixels) {
        if (CollectionUtils.isEmpty(visagesPrecedents)) {
            return null;
        }
        Vector2D centroideDetecte = visageDetecte.getCentroid();
        VisageSuivi plusProche = null;
        double distanceMin = distanceMaxPixels;
        for (VisageSuivi visagePrecedent : visagesPrecedents) {
            double distance = centroideDetecte.distance(visagePrecedent.boite().getCentroid());
            if (distance < distanceMin) {
                distanceMin = distance;
                plusProche = visagePrecedent;
            }
        }
        return plusProche;
    }
}
