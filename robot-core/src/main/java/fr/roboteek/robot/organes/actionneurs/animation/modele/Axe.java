package fr.roboteek.robot.organes.actionneurs.animation.modele;

/**
 * Un degré de liberté animable, c'est-à-dire une piste de la timeline de l'éditeur.
 * <p>
 * Le roulis n'y figure pas et n'y figurera pas : l'œil droit tourne en sens inverse sur le même
 * axe, ce qui interdit un roulis symétrique — fonctionnalité abandonnée le 2026-07-26.
 * <p>
 * Les noms de ces valeurs voyagent tels quels dans le JSON échangé avec l'éditeur : les changer
 * casse les animations déjà enregistrées.
 */
public enum Axe {

    OEIL_GAUCHE("Œil gauche"),

    OEIL_DROIT("Œil droit"),

    COU_GAUCHE_DROITE("Cou gauche / droite"),

    /**
     * Inclinaison de la tête. Course très courte (une quinzaine de degrés) et lente : c'est le
     * moins expressif des axes, l'amplitude verticale est dans {@link #COU_MONTER_DESCENDRE}.
     */
    COU_HAUT_BAS("Cou haut / bas"),

    COU_MONTER_DESCENDRE("Cou monter / descendre");

    private final String libelle;

    Axe(String libelle) {
        this.libelle = libelle;
    }

    /** Libellé destiné à l'affichage dans l'éditeur. */
    public String libelle() {
        return libelle;
    }
}
