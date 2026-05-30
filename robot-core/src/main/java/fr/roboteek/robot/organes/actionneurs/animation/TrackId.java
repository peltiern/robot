package fr.roboteek.robot.organes.actionneurs.animation;

/**
 * Identifiants des tracks d'animation, un par axe moteur.
 * Chaque valeur porte son nom d'affichage en français.
 */
public enum TrackId {

    OEIL_GAUCHE("Œil Gauche"),
    OEIL_DROIT("Œil Droit"),
    COU_GAUCHE_DROITE("Cou Gauche / Droite"),
    COU_HAUT_BAS("Cou Haut / Bas"),
    COU_MONTER_DESCENDRE("Cou Monter / Descendre");

    private final String displayName;

    TrackId(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
