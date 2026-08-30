package fr.roboteek.robot.web.controller.dto;

import java.util.List;

/**
 * Ce que répond le robot quand on lui demande de jouer une animation.
 * <p>
 * Les avertissements accompagnent aussi bien une lecture acceptée qu'un refus : ils décrivent
 * l'animation, pas l'issue de la demande. Une animation qui demande à un servo plus vite qu'il ne
 * sait aller sera jouée quand même — le mouvement sera simplement en retard sur la courbe — et
 * c'est cette ligne-là qui l'explique. Sans elle, on chercherait la panne dans le lecteur.
 *
 * @param lancee         faux si le lecteur a refusé : arrêt d'urgence armé, ou organe pas démarré
 * @param avertissements messages du vérificateur, vides si l'animation tient dans les butées et
 *                       dans les vitesses configurées
 */
public record Lecture(String nom, boolean lancee, List<String> avertissements) {
}
