package fr.roboteek.robot.web.controller.dto;

import fr.roboteek.robot.decisionnel.emotion.Emotion;

/** Ce que la bibliothèque de l'Atelier montre d'une animation sans l'ouvrir. */
public record ResumeAnimation(String nom, Emotion emotion) {
}
