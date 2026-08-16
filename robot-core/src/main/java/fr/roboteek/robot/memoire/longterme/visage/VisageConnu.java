package fr.roboteek.robot.memoire.longterme.visage;

/**
 * Empreinte biométrique SFace rattachée à une personne, telle que stockée en base par
 * {@link VisageConnuRepository}.
 * <p>
 * Plusieurs entrées peuvent pointer vers la même personne (visages sous différents angles ou
 * éclairages) : la reconnaissance retient la meilleure similarité parmi toutes les entrées.
 * <p>
 * Ce record ne porte que l'identifiant de la personne, pas son prénom : le nom d'usage vit
 * dans {@code Personne}, dont ceci n'est que l'index biométrique.
 *
 * @param id         identifiant de l'empreinte elle-même
 * @param idPersonne identifiant de la {@code Personne} reconnue
 * @param embedding  empreinte SFace du visage
 */
public record VisageConnu(String id, String idPersonne, float[] embedding) {
}
