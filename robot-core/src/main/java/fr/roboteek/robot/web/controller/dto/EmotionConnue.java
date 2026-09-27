package fr.roboteek.robot.web.controller.dto;

/** Une émotion telle que l'appli l'affiche : sa clé, son nom, son emoji. */
public record EmotionConnue(String cle, String libelle, String emoji) {
}
