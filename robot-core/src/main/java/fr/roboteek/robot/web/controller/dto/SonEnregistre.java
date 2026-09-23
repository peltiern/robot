package fr.roboteek.robot.web.controller.dto;

import tools.jackson.databind.JsonNode;

/**
 * Un son tel que le Studio l'envoie : sa recette, et le son qu'elle a produit.
 * <p>
 * Le WAV voyage en base64 dans le JSON, et non en {@code multipart} : un son dépasse rarement
 * 100 Ko, l'éditeur l'envoie d'un seul appel avec sa recette — donc sans pouvoir laisser derrière
 * lui une recette sans son — et l'enregistrement hors ligne n'a qu'une seule chose à garder dans
 * le navigateur en attendant le retour du robot.
 *
 * @param nom     nom du son ; utilisé par la création, ignoré par le remplacement, où c'est
 *                l'URL qui fait foi — comme pour les animations
 * @param recette ce que le Studio sait refaire ; le robot la range sans la relire
 * @param wav     le fichier audio en base64
 */
public record SonEnregistre(String nom, JsonNode recette, String wav) {
}
