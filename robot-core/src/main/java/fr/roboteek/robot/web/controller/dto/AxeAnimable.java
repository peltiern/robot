package fr.roboteek.robot.web.controller.dto;

/**
 * Un axe que l'éditeur peut animer, avec ce qu'il faut pour en dessiner la piste.
 * <p>
 * Même philosophie que {@link Organe} : l'éditeur se construit à partir de cette description
 * plutôt que de coder en dur les axes, leurs libellés et leurs butées. Une butée corrigée dans
 * {@code robot.properties} doit changer la timeline sans qu'on recompile le front.
 * <p>
 * Positions en degrés <b>relatifs</b>, le repère des images-clés : zéro est la posture de travail
 * du robot, et non une position moteur.
 *
 * @param id                     nom de la valeur d'énumération, tel qu'il voyage dans le JSON
 * @param libelle                ce que l'éditeur affiche
 * @param vitesseParDefaut       vitesse de travail de l'axe, en °/s
 * @param accelerationParDefaut  accélération de travail de l'axe, en °/s²
 */
public record AxeAnimable(String id, String libelle, double positionMin, double positionMax,
                          double vitesseParDefaut, double accelerationParDefaut) {
}
