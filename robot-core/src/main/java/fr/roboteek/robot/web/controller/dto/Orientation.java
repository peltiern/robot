package fr.roboteek.robot.web.controller.dto;

/**
 * Orientation physique du mouvement d'une articulation. Sémantique (pas de la mise en forme) :
 * décrit le degré de liberté réel, à charge du client de choisir le widget adapté
 * (un axe {@link #VERTICAL} appelle naturellement un curseur vertical, etc.).
 */
public enum Orientation {
    /** Mouvement vertical (haut / bas). */
    VERTICAL,
    /** Mouvement horizontal (gauche / droite). */
    HORIZONTAL,
    /** Rotation autour d'un axe (roulis, …). */
    ROTATION
}
