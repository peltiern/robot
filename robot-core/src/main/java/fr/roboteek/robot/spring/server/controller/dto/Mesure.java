package fr.roboteek.robot.spring.server.controller.dto;

/**
 * Représentation REST d'une mesure exposée par un organe {@link TypeOrgane#CAPTEUR}.
 * <p>
 * Pendant de {@link Articulation} côté capteurs : mêmes principes de découverte de capacités
 * (le client affiche un contrôle/graphique adapté sans coder la mesure en dur), en lecture
 * seule. {@code min}/{@code max} bornent l'échelle attendue (ex. un pourcentage 0-100), à titre
 * indicatif pour le rendu (jauge, graphique…) — une valeur peut légitimement les dépasser.
 *
 * @param id      identifiant stable ({@code cpuCharge}, {@code temperatureCpu}, …)
 * @param libelle libellé lisible destiné à l'affichage
 * @param unite   unité de la valeur ({@code %}, {@code °C}, …)
 * @param min     borne basse indicative de l'échelle
 * @param max     borne haute indicative de l'échelle
 * @param valeur  valeur courante, ou {@code null} si indisponible (capteur absent/illisible)
 */
public record Mesure(String id, String libelle, String unite, double min, double max, Double valeur) {
}
