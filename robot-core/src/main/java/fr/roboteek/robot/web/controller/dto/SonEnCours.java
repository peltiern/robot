package fr.roboteek.robot.web.controller.dto;

/**
 * Le son que le robot est en train de jouer, et celui qu'on lui demande de jouer : même forme dans
 * les deux sens, puisqu'il n'y a rien d'autre à dire qu'un nom.
 *
 * @param nom nom du son dans la bibliothèque du Studio
 */
public record SonEnCours(String nom) {
}
