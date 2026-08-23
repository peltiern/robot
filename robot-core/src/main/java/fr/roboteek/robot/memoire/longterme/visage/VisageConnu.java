package fr.roboteek.robot.memoire.longterme.visage;

/**
 * Une empreinte biométrique SFace rattachée à une personne.
 * <p>
 * Plusieurs empreintes par personne, prises sous différents angles et éclairages : la
 * reconnaissance retient la meilleure similarité parmi toutes.
 */
public record VisageConnu(String id, String idPersonne, float[] embedding) {
}
