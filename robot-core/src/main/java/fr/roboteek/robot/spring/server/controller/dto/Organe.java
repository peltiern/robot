package fr.roboteek.robot.spring.server.controller.dto;

import java.util.List;

/**
 * Représentation REST d'un organe du robot : brique de la découverte de capacités qui permet
 * à un client de se construire dynamiquement, sans connaître à l'avance les organes d'un robot
 * donné. Un organe {@link TypeOrgane#ACTIONNEUR} porte ses {@code articulations} (liste de
 * mesures vide) ; un {@link TypeOrgane#CAPTEUR} porte ses {@code mesures} (liste d'articulations
 * vide) — un organe n'a jamais les deux, mais garder les deux champs (plutôt qu'un type union)
 * simplifie la désérialisation côté clients faiblement typés.
 *
 * @param id            identifiant stable ({@code cou}, {@code yeux}, {@code materiel}, …)
 * @param libelle       libellé lisible destiné à l'affichage
 * @param type          nature de l'organe
 * @param articulations degrés de liberté pilotables (vide pour un capteur)
 * @param mesures       mesures en lecture seule (vide pour un actionneur)
 */
public record Organe(String id, String libelle, TypeOrgane type, List<Articulation> articulations, List<Mesure> mesures) {
}
