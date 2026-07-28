package fr.roboteek.robot.memoire.visage;

import java.io.Serializable;

/**
 * Association nom / embedding SFace, telle que stockée en base par {@link VisageConnuRepository}.
 * Plusieurs entrées peuvent partager le même nom (photos sous différents angles/éclairages) :
 * la reconnaissance retient la meilleure similarité parmi toutes les entrées.
 */
public record VisageConnu(String nom, float[] embedding) implements Serializable {
}
