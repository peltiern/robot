package fr.roboteek.robot.web.controller.dto;

import fr.roboteek.robot.organes.actionneurs.voix.Voix;

/** Une phrase à dire avec une voix qu'on essaie. */
public record EssaiDeVoix(String texte, Voix voix) {
}
