package fr.roboteek.robot.util.webcam;

import fr.roboteek.robot.memoire.RecognizedFace;

/**
 * Visage tel que suivi d'un cycle de reconnaissance au suivant : sa boîte englobante et
 * l'identité qu'on lui a trouvée.
 * <p>
 * L'identifiant de personne accompagne la boîte parce que la reconnaissance SFace n'est pas
 * relancée sur un visage déjà suivi (~68 ms par visage) : sans lui, l'identité serait perdue
 * dès la frame suivante, ou il faudrait la retrouver par le prénom — qui n'identifie personne.
 *
 * @param boite      boîte englobante, portant le prénom affiché dans le flux vidéo
 * @param idPersonne identifiant de la {@code Personne} reconnue, {@code null} si inconnue
 */
public record VisageSuivi(RecognizedFace boite, String idPersonne) {

    /** Indique si le visage a été rattaché à une personne connue. */
    public boolean estIdentifie() {
        return idPersonne != null;
    }

    /** Prénom affiché, {@code null} pour un inconnu. */
    public String prenom() {
        return boite.getName();
    }
}
