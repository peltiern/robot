package fr.roboteek.robot.web.controller.dto;

/**
 * Une mesure exposée par un organe {@link TypeOrgane#CAPTEUR}, en lecture seule. Pendant de
 * {@link Articulation} : le client en tire un affichage sans coder la mesure en dur.
 * <p>
 * {@code hautEstBon} existe parce que la couleur en dépend. Pour presque tout ce que publie le
 * robot — charge, mémoire, température, remplissage — monter est mauvais, et l'interface vire à
 * l'ambre puis au rouge en haut d'échelle. L'espace disque libre dit le contraire, une batterie
 * dira pareil : sans ce drapeau, un disque presque plein s'afficherait en vert franc.
 *
 * @param min    borne basse indicative de l'échelle ; une valeur peut légitimement la dépasser
 * @param max    borne haute indicative de l'échelle
 * @param valeur valeur courante, ou {@code null} si indisponible (capteur absent ou illisible)
 */
public record Mesure(String id, String libelle, String unite, double min, double max, Double valeur,
                     boolean hautEstBon) {

    /** Le cas courant : monter est mauvais signe. */
    public Mesure(String id, String libelle, String unite, double min, double max, Double valeur) {
        this(id, libelle, unite, min, max, valeur, false);
    }
}
