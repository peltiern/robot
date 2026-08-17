package fr.roboteek.robot.memoire.longterme.rencontre;

import java.time.LocalDateTime;

/**
 * Une fois où le robot a rencontré quelqu'un. Ces lignes mises bout à bout font la timeline
 * d'apparition d'une personne.
 * <p>
 * Ce que {@code Personne.derniereRencontre} ne pourra jamais dire : cette date-là est écrasée à
 * chaque fois, elle ne garde que la dernière. Ici rien n'est écrasé, on ajoute.
 * <p>
 * Une rencontre n'est pas une perception. La perception clignote au rythme des ratés de la
 * détection ; une rencontre est rare et délibérée, annoncée par {@code RegistrePresence} une fois
 * la présence confirmée et la temporisation écoulée. C'est bien une visite qui est journalisée,
 * pas une image.
 *
 * @param id               identifiant de la ligne
 * @param idPersonne       la personne rencontrée
 * @param instant          quand
 * @param type             première fois qu'on la voit, ou retour
 * @param secondesDAbsence durée de l'absence qui a précédé, {@code -1} pour une première rencontre
 */
public record Rencontre(long id, String idPersonne, LocalDateTime instant, Type type, long secondesDAbsence) {

    /** Nature de la rencontre, telle qu'elle se lit sur une timeline. */
    public enum Type {
        /** On vient de faire connaissance : c'est le début de l'histoire. */
        PREMIERE,
        /** Quelqu'un qu'on connaît revient. {@link #secondesDAbsence} dit après combien de temps. */
        RETOUR
    }
}
