package fr.roboteek.robot.systemenerveux.event;

/**
 * Qui a demandé un mouvement.
 * <p>
 * Le cou a plusieurs commanditaires — la manette, le regard, les animations, le HUD — qui
 * publient tous le même {@link MouvementCouEvent} vers le même organe. Rien ne les distinguait :
 * les ordres s'empilaient dans l'exécuteur et le dernier arrivé gagnait, si bien qu'une correction
 * du regard pouvait s'intercaler au milieu d'un mouvement piloté à la main.
 * <p>
 * L'organe, lui, n'a que faire de l'origine : il exécute. C'est du côté de ceux qui commandent
 * qu'elle sert, pour se taire quand quelqu'un d'autre est plus légitime — aujourd'hui le regard
 * devant la manette, cf. {@code Regard}.
 */
public enum OrigineMouvement {

    /** Un humain conduit, manette en main. Prioritaire sur tout le reste. */
    MANETTE,

    /** Le robot tourne la tête vers un visage de lui-même. */
    REGARD,

    /** Tout le reste : animations, HUD, réflexes. Valeur par défaut. */
    AUTRE
}
