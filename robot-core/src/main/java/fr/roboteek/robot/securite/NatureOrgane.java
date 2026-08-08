package fr.roboteek.robot.securite;

/**
 * Nature d'un organe : agit-il sur le monde, ou l'observe-t-il ?
 * <p>
 * <b>À ne pas confondre avec {@link OrganeSurveille#provoqueUnMouvement()}</b>, qui répond à une
 * tout autre question — « son silence doit-il couper les moteurs ? ». La manette est un
 * {@link #CAPTEUR} (elle observe l'opérateur, elle ne bouge rien elle-même) et pourtant elle est
 * surveillée, puisqu'elle est la seule à pouvoir envoyer le {@code STOPPER} qui arrête le robot.
 * Déduire l'une de l'autre, comme le faisait l'interface, rangeait la manette parmi les
 * actionneurs.
 * <p>
 * Chaque organe la déclare : aucune valeur par défaut, pour qu'un organe nouveau ait à choisir.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public enum NatureOrgane {

    /** Agit sur le monde : cou, yeux, chenilles, lecteur d'animations. */
    ACTIONNEUR,

    /** Observe : vision, micro, matériel, manette. */
    CAPTEUR
}
