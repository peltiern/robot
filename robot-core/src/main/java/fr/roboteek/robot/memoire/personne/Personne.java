package fr.roboteek.robot.memoire.personne;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quelqu'un que le robot connaît : mémoire longue, par opposition à la présence du moment
 * que tient {@link RegistrePresence}.
 * <p>
 * L'identité est portée par {@link #id}, pas par le prénom : deux personnes peuvent
 * s'appeler Marie, et une même personne peut voir son prénom corrigé après une
 * reconnaissance vocale approximative. C'est cet identifiant que référencent les empreintes
 * biométriques ({@code VisageConnu}) et le fil de conversation ({@code ConversationIA}).
 *
 * @param id                          identifiant stable, jamais réutilisé
 * @param prenom                      prénom tel que la personne l'a donné
 * @param derniereRencontre           date de la dernière rencontre, {@code null} si jamais rencontrée
 * @param resumeDerniereConversation  une phrase résumant le dernier échange, {@code null} si aucun
 */
public record Personne(String id, String prenom, LocalDateTime derniereRencontre,
                       String resumeDerniereConversation) implements Serializable {

    /** Crée une personne encore jamais rencontrée. */
    public static Personne nouvelle(String prenom) {
        return new Personne(UUID.randomUUID().toString(), prenom, null, null);
    }

    /** Copie datée d'une nouvelle rencontre. */
    public Personne rencontreeLe(LocalDateTime instant) {
        return new Personne(id, prenom, instant, resumeDerniereConversation);
    }

    /** Copie portant le résumé du dernier échange. */
    public Personne avecResumeDeConversation(String resume) {
        return new Personne(id, prenom, derniereRencontre, resume);
    }
}
