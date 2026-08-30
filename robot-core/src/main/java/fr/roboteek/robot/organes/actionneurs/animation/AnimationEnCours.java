package fr.roboteek.robot.organes.actionneurs.animation;

/**
 * Sait dire si une animation est en train de se dérouler.
 * <p>
 * Existe pour une seule raison : {@code Regard} doit s'effacer pendant une animation, et il n'a
 * besoin que de cette réponse-là. Lui donner le lecteur entier ferait dépendre le décisionnel d'un
 * actionneur pour une question de trois mots, et rendrait la mise en retrait impossible à
 * éprouver sans faire tourner un vrai lecteur avec son thread.
 * <p>
 * Le jour où l'ancien {@code AnimationPlayer} disparaîtra, il n'y aura qu'une implémentation ;
 * d'ici là, rien n'empêche de lui faire porter la même réponse.
 */
@FunctionalInterface
public interface AnimationEnCours {

    /** Vrai tant qu'une animation se joue. */
    boolean enLecture();
}
