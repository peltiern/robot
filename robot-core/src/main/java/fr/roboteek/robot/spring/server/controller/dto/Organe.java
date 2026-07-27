package fr.roboteek.robot.spring.server.controller.dto;

import java.util.List;

/**
 * Représentation REST d'un organe du robot : brique de la découverte de capacités qui permet
 * à un client de se construire dynamiquement, sans connaître à l'avance les organes d'un robot
 * donné. Un organe {@link TypeOrgane#ACTIONNEUR} porte ses {@code articulations} ; un
 * {@link TypeOrgane#CAPTEUR} en porte une liste vide (ses mesures seront exposées à terme).
 *
 * @param id            identifiant stable ({@code cou}, {@code yeux}, …)
 * @param libelle       libellé lisible destiné à l'affichage
 * @param type          nature de l'organe
 * @param articulations degrés de liberté pilotables (vide pour un capteur)
 */
public record Organe(String id, String libelle, TypeOrgane type, List<Articulation> articulations) {
}
