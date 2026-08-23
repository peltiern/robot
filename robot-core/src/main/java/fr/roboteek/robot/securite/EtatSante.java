package fr.roboteek.robot.securite;

/**
 * État de santé d'un organe, tel que le watchdog le juge et que l'interface l'affiche.
 * <p>
 * Trois états et non deux : un organe éteint volontairement (vision désactivée, manette absente)
 * ne doit pas se lire comme en panne, sous peine d'afficher en permanence des pastilles rouges
 * qu'on finit par ne plus regarder.
 */
public enum EtatSante {

    /** L'organe bat à la cadence attendue. */
    VIVANT,

    /** L'organe devrait battre et ne bat plus : c'est l'anomalie. */
    MUET,

    /** L'organe n'est pas en service — par configuration, ou faute de matériel. Normal. */
    ETEINT
}
