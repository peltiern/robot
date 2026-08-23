package fr.roboteek.robot.web.controller.dto;

import java.util.List;

/**
 * Une personne telle que l'interface la montre, à deux niveaux de détail : la liste ne remplit
 * que l'entête, la fiche y ajoute la timeline et le fil de conversation.
 * <p>
 * Ce qui n'est pas demandé vaut {@code null} et non une liste vide — une liste vide dirait
 * « cette personne n'a rien dit », ce qui est faux.
 *
 * @param derniereRencontre  ISO-8601, {@code null} si on ne l'a jamais rencontrée
 * @param nombreDeVisages    combien d'empreintes la reconnaissent ; zéro veut dire connue mais
 *                           jamais reconnue
 * @param aUneVignette       vrai si un portrait existe, à demander sur {@code /{id}/vignette} :
 *                           l'image n'est jamais dans cette réponse, trente portraits en base64
 *                           dans une liste et la tablette rame
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
     * @param secondesDAbsence durée de l'absence qui a précédé, {@code -1} pour une première fois
     */
    public record RencontreDto(String instant, String type, long secondesDAbsence) {
    }

    /** Un message du fil ; {@code role} vaut {@code USER}, {@code ASSISTANT} ou {@code SYSTEM}. */
    public record MessageDto(String role, String texte) {
    }
}
