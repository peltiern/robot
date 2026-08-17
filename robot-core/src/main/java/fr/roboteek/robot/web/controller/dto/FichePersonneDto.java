package fr.roboteek.robot.web.controller.dto;

import java.util.List;

/**
 * Une personne telle que l'interface la montre.
 * <p>
 * Deux niveaux de détail dans un seul objet : la liste ne remplit que l'entête, la fiche y ajoute
 * la timeline et le fil de conversation. Ce qui n'est pas demandé vaut {@code null}, et non une
 * liste vide — une liste vide dirait « cette personne n'a rien dit », ce qui est faux.
 *
 * @param id                 identifiant stable, celui qu'attendent les autres routes
 * @param prenom             prénom tel que la personne l'a donné
 * @param derniereRencontre  ISO-8601, {@code null} si on ne l'a jamais rencontrée
 * @param nombreDeVisages    combien d'empreintes la reconnaissent ; zéro veut dire qu'elle est
 *                           connue mais ne sera jamais reconnue
 * @param nombreDeRencontres longueur de sa timeline
 * @param aUneVignette       vrai si un portrait existe, à demander sur {@code /{id}/vignette} ;
 *                           l'image n'est jamais dans cette réponse — trente portraits en base64
 *                           dans une liste, et la tablette rame
 * @param rencontres         sa timeline, de la plus récente à la plus ancienne ; {@code null} en liste
 * @param conversation       son fil, dans l'ordre où il s'est tenu ; {@code null} en liste
 */
public record FichePersonneDto(String id,
                               String prenom,
                               String derniereRencontre,
                               int nombreDeVisages,
                               int nombreDeRencontres,
                               boolean aUneVignette,
                               List<RencontreDto> rencontres,
                               List<MessageDto> conversation) {

    /**
     * Une apparition dans la timeline.
     *
     * @param instant          ISO-8601
     * @param type             {@code PREMIERE} ou {@code RETOUR}
     * @param secondesDAbsence durée de l'absence qui a précédé, {@code -1} pour une première rencontre
     */
    public record RencontreDto(String instant, String type, long secondesDAbsence) {
    }

    /**
     * Un message du fil.
     *
     * @param role  {@code USER}, {@code ASSISTANT} ou {@code SYSTEM}
     * @param texte ce qui a été dit
     */
    public record MessageDto(String role, String texte) {
    }
}
