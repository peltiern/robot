package fr.roboteek.robot.memoire.longterme.rencontre;

import java.time.LocalDateTime;

/**
 * Une fois où le robot a rencontré quelqu'un ; bout à bout, ces lignes font sa timeline.
 * <p>
 * Ce que {@code Personne.derniereRencontre} ne dira jamais : cette date-là est écrasée à chaque
 * fois. Ici rien n'est écrasé, on ajoute.
 * <p>
 * Une rencontre n'est pas une perception. La perception clignote au rythme des ratés de la
 * détection ; une rencontre est rare et délibérée, annoncée par {@code RegistrePresence} une fois
 * la présence confirmée. C'est une visite qui est journalisée, pas une image.
 *
 * @param secondesDAbsence durée de l'absence qui a précédé, {@code -1} pour une première rencontre
 */
public record Rencontre(long id, String idPersonne, LocalDateTime instant, Type type, long secondesDAbsence) {

    public enum Type {
        /** On vient de faire connaissance : c'est le début de l'histoire. */
        PREMIERE,
        /** Quelqu'un qu'on connaît revient. {@link #secondesDAbsence} dit après combien de temps. */
        RETOUR
    }
}
