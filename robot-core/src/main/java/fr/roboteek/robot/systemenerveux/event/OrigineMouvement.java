package fr.roboteek.robot.systemenerveux.event;

/**
 * Qui a demandé un mouvement.
 * <p>
 * Le cou a plusieurs commanditaires — manette, regard, animations, HUD — qui publient tous le même
 * {@link MouvementCouEvent}. Sans les distinguer, les ordres s'empilent dans l'exécuteur et le
 * dernier arrivé gagne : une correction du regard s'intercale alors au milieu d'un mouvement
 * piloté à la main.
 * <p>
 * L'organe n'a que faire de l'origine, il exécute. Elle sert à ceux qui commandent, pour se taire
 * quand quelqu'un d'autre est plus légitime (voir {@code Regard}).
 */
public enum OrigineMouvement {

    /** Un humain conduit, manette en main. Prioritaire sur tout le reste. */
    MANETTE,

    /** Le robot tourne la tête vers un visage de lui-même. */
    REGARD,

    /** Tout le reste : animations, HUD, réflexes. Valeur par défaut. */
    AUTRE
}
