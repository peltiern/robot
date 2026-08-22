package fr.roboteek.robot.web.controller.dto;

/**
 * Représentation REST d'une mesure exposée par un organe {@link TypeOrgane#CAPTEUR}.
 * <p>
 * Pendant de {@link Articulation} côté capteurs : mêmes principes de découverte de capacités
 * (le client affiche un contrôle/graphique adapté sans coder la mesure en dur), en lecture
 * seule. {@code min}/{@code max} bornent l'échelle attendue (ex. un pourcentage 0-100), à titre
 * indicatif pour le rendu (jauge, graphique…) — une valeur peut légitimement les dépasser.
 *
 * Le sens de la mesure est déclaré, parce que la couleur en dépend. Pour presque tout ce que
 * publie le robot — charge, mémoire, température, remplissage — monter est mauvais, et
 * l'interface passe à l'ambre puis au rouge en haut d'échelle. L'espace disque libre dit
 * exactement le contraire, et une batterie dira la même chose : sans ce drapeau, un disque
 * presque plein s'afficherait en vert franc.
 *
 * @param id          identifiant stable ({@code cpuCharge}, {@code temperatureCpu}, …)
 * @param libelle     libellé lisible destiné à l'affichage
 * @param unite       unité de la valeur ({@code %}, {@code °C}, …)
 * @param min         borne basse indicative de l'échelle
 * @param max         borne haute indicative de l'échelle
 * @param valeur      valeur courante, ou {@code null} si indisponible (capteur absent/illisible)
 * @param hautEstBon  vrai quand monter est une bonne nouvelle (espace libre, charge de batterie)
 */
public record Mesure(String id, String libelle, String unite, double min, double max, Double valeur,
                     boolean hautEstBon) {

    /** Le cas courant : monter est mauvais signe. */
    public Mesure(String id, String libelle, String unite, double min, double max, Double valeur) {
        this(id, libelle, unite, min, max, valeur, false);
    }
}
