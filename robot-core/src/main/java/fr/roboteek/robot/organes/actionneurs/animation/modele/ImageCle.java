package fr.roboteek.robot.organes.actionneurs.animation.modele;

/**
 * Position voulue sur un axe à un instant donné : un point posé à la main dans la timeline.
 * Entre deux images-clés, la trajectoire est calculée (voir {@code Interpolateur}).
 *
 * @param instant      instant depuis le début de l'animation, en millisecondes
 * @param valeur       position en degrés, dans le repère relatif de l'axe (0 = position neutre)
 * @param vitesse      vitesse à tenir pour rejoindre ce point, ou {@code null} pour celle de la
 *                     piste — laissée libre presque toujours, le régime trajectoire recalculant
 *                     la vitesse à chaque échantillon
 * @param acceleration accélération, ou {@code null} pour celle de la piste
 */
public record ImageCle(long instant, double valeur, Double vitesse, Double acceleration) {

    public ImageCle(long instant, double valeur) {
        this(instant, valeur, null, null);
    }
}
