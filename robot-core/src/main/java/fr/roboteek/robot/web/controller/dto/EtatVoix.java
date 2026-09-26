package fr.roboteek.robot.web.controller.dto;

import fr.roboteek.robot.organes.actionneurs.voix.ReglagesVoix;

/**
 * La voix vue de l'appli.
 *
 * @param adoptee  les réglages avec lesquels le robot parle
 * @param origine  la voix Wall-E validée le 2026-08-16, pour y revenir d'un geste
 * @param reglable faux si le robot parle avec Google : les réglages ne s'appliquent qu'à Piper
 */
public record EtatVoix(ReglagesVoix adoptee, ReglagesVoix origine, boolean reglable) {
}
