package fr.roboteek.robot.web.controller.dto;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;

/**
 * Ce qu'on demande à jouer : une animation de la bibliothèque, ou une animation fournie telle
 * quelle.
 * <p>
 * Les deux formes existent parce que l'éditeur a besoin d'essayer un brouillon <b>avant</b> de
 * l'enregistrer — c'est même l'usage principal pendant qu'on écrit un geste. L'obliger à
 * enregistrer pour voir bouger la tête polluerait la bibliothèque d'essais.
 *
 * @param nom       nom d'une animation enregistrée, ou {@code null}
 * @param animation animation complète à jouer sans l'enregistrer, ou {@code null}
 */
public record DemandeLecture(String nom, Animation animation) {
}
