package fr.roboteek.robot.web.controller.dto;

import java.util.List;

/**
 * L'animation que le robot est en train de jouer.
 *
 * @param nom            nom de l'animation lancée
 * @param avertissements ce que le vérificateur a à redire ; elle est jouée quand même, une
 *                       transition trop rapide se traduisant par un mouvement en retard sur la
 *                       courbe et non par un refus
 */
public record AnimationEnCours(String nom, List<String> avertissements) {
}
