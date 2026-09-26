package fr.roboteek.robot.organes.actionneurs.voix;

/**
 * Une voix de base que Piper sait dire : un modèle, et pour les modèles à plusieurs voix, laquelle.
 *
 * @param fichier   le modèle, sous {@code synthese-vocale/piper/models/}
 * @param locuteur  le numéro de la voix dans un modèle qui en a plusieurs, {@code null} sinon
 * @param nom       ce que l'appli affiche : le modèle, et la voix s'il y a lieu
 */
public record ModeleVoix(String fichier, Integer locuteur, String nom) {
}
