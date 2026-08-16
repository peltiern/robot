package fr.roboteek.robot.util.webcam;

import fr.roboteek.robot.memoire.RecognizedFace;

/**
 * Visage tel que suivi d'un cycle de reconnaissance au suivant : sa boîte englobante et
 * l'identité qu'on lui a trouvée.
 * <p>
 * L'identifiant de personne accompagne la boîte parce que la reconnaissance SFace n'est pas
 * relancée sur un visage déjà suivi (~68 ms par visage) : sans lui, l'identité serait perdue
 * dès la frame suivante, ou il faudrait la retrouver par le prénom — qui n'identifie personne.
 * <p>
 * L'instant d'identification, lui, est ce qui empêche le suivi de se transformer en verrou.
 * Il date la dernière fois que <b>SFace</b> a confirmé cette identité, et non la dernière fois
 * qu'elle a été recopiée d'un cycle au suivant : une identité héritée conserve donc la date de
 * son origine et finit par périmer. Sans cette distinction, une étiquette posée une fois se
 * recopiait indéfiniment — constaté sur le robot le 2026-08-15, où un visage ayant hérité du nom
 * d'une photo voisine le gardait tant que la personne restait dans le champ.
 *
 * @param boite                 boîte englobante, portant le prénom affiché dans le flux vidéo
 * @param idPersonne            identifiant de la {@code Personne} reconnue, {@code null} si inconnue
 * @param instantIdentification horodatage ({@code System.currentTimeMillis()}) de la dernière
 *                              confirmation par SFace, {@code 0} pour un visage inconnu
 */
public record VisageSuivi(RecognizedFace boite, String idPersonne, long instantIdentification) {

    /** Indique si le visage a été rattaché à une personne connue. */
    public boolean estIdentifie() {
        return idPersonne != null;
    }

    /** Prénom affiché, {@code null} pour un inconnu. */
    public String prenom() {
        return boite.getName();
    }

    /** Âge, en millisecondes, de la dernière confirmation par SFace. */
    public long ageIdentification(long maintenant) {
        return maintenant - instantIdentification;
    }
}
