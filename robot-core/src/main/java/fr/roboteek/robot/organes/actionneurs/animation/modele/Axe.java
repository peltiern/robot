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
     * Inclinaison de la tête. Course de <b>75°</b>, et non « une quinzaine » comme cette
     * documentation l'a affirmé jusqu'au 2026-09-08 : les quinze étaient des unités de position
     * moteur, et le Stingray-2 en fait 4,97 degrés chacune. C'est au contraire l'axe le plus
     * démultiplié du robot.
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
