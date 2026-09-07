package fr.roboteek.robot.web.controller.dto;

/**
 * Ce que le HUD doit savoir pour dessiner la zone morte du regard sur l'image.
 * <p>
 * Les deux centres relatifs en font partie, et ce n'est pas un détail d'affichage : {@code Regard}
 * compte ses écarts depuis l'axe optique, pas depuis le milieu de l'image. Un repère centré sur
 * l'image serait décalé d'une trentaine de pixels vers le bas, et masquerait exactement le biais
 * qu'on vient regarder.
 *
 * @param actif                  le suivi de visage est-il seulement en service
 * @param zoneMorteDegres        en deçà, le robot considère qu'il regarde déjà la personne
 * @param champHorizontalDegres  de quoi retrouver la focale en pixels, donc le rayon à dessiner
 * @param centreXRelatif         axe optique en fraction de la largeur, comme dans {@code Regard}
 * @param centreYRelatif         axe optique en fraction de la hauteur
 * @param deportDegres           de combien le panoramique vise à côté de l'axe, la webcam n'étant
 *                               que dans un des deux yeux
 */
public record ReglagesRegard(boolean actif, double zoneMorteDegres, double champHorizontalDegres,
                             double centreXRelatif, double centreYRelatif, double deportDegres) {
}
