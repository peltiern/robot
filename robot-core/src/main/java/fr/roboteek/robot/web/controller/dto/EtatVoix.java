package fr.roboteek.robot.web.controller.dto;

import fr.roboteek.robot.organes.actionneurs.voix.ModeleVoix;
import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;
import fr.roboteek.robot.organes.actionneurs.voix.Voix;

import java.util.List;

/**
 * La voix vue de l'appli.
 *
 * @param adoptee         la voix avec laquelle le robot parle
 * @param origine         la coloration Wall-E validée le 2026-08-16, pour y revenir d'un geste
 * @param reglable        faux si le robot parle avec Google : la voix ne se règle que pour Piper
 * @param modeles         les voix de base déposées sur le robot
 * @param modeleParDefaut celui de {@code robot.properties}, que désigne un modèle {@code null}
 */
public record EtatVoix(Voix adoptee, ReglagesVoix origine, boolean reglable,
                       List<ModeleVoix> modeles, String modeleParDefaut) {
}
