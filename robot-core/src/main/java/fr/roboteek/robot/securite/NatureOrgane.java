package fr.roboteek.robot.securite;

/**
 * Nature d'un organe : agit-il sur le monde, ou l'observe-t-il ?
 * <p>
 * <b>À ne pas confondre avec {@link OrganeSurveille#provoqueUnMouvement()}</b>, qui répond à
 * « son silence doit-il couper les moteurs ? ». La manette est un {@link #CAPTEUR} — elle observe
 * l'opérateur — et pourtant elle est surveillée, étant la seule à pouvoir envoyer le
 * {@code STOPPER}. Déduire l'une de l'autre range la manette parmi les actionneurs.
 * <p>
 * Chaque organe la déclare, sans valeur par défaut : un organe nouveau doit choisir.
 */
public enum NatureOrgane {

    /** Agit sur le monde : cou, yeux, chenilles, lecteur d'animations. */
    ACTIONNEUR,

    /** Observe : vision, micro, matériel, manette. */
    CAPTEUR
}
