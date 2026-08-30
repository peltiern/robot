package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;

import java.util.List;

/**
 * Les avertissements d'une animation, mis en phrases pour l'extérieur.
 * <p>
 * Rassemblé ici parce que trois endroits en avaient besoin — l'enregistrement, le remplacement et
 * le lancement — et que chacun refaisait le même assemblage de vérificateur, de butées et de mise
 * en forme. Trois copies d'un texte qui aurait divergé au premier réglage.
 */
public final class Avertissements {

    private Avertissements() {
    }

    /**
     * Ce que le vérificateur a à redire, contre les butées et vitesses du robot tel qu'il est
     * réglé maintenant. La liste est vide quand l'animation est jouable telle quelle.
     */
    public static List<String> de(Animation animation) {
        return new VerificateurAnimation(LimitesMoteur.parAxe(Configurations.phidgetsConfig()))
                .controler(animation)
                .stream()
                .map(avertissement -> "%s (%d à %d ms) : %s".formatted(
                        avertissement.axe().libelle(), avertissement.instantDebut(),
                        avertissement.instantFin(), avertissement.message()))
                .toList();
    }
}
