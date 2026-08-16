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
 * <p>
 * <b>Rien n'est stocké ici du contenu des échanges.</b> Un champ {@code resumeDerniereConversation}
 * l'a été un temps, en prévision des retrouvailles ; il s'est révélé inutile et a été retiré le
 * 2026-08-15. La mémoire de conversation est tenue par personne et persistée par
 * {@code ConversationIA} (cent messages par fil), le robot a donc déjà de quoi se souvenir — bien
 * mieux qu'une phrase de résumé. Ne pas le réintroduire sans avoir constaté que la mémoire de
 * conversation ne suffit pas.
 *
 * @param id                identifiant stable, jamais réutilisé
 * @param prenom            prénom tel que la personne l'a donné
 * @param derniereRencontre date de la dernière rencontre, {@code null} si jamais rencontrée
 */
public record Personne(String id, String prenom, LocalDateTime derniereRencontre) implements Serializable {

    /** Crée une personne encore jamais rencontrée. */
    public static Personne nouvelle(String prenom) {
        return new Personne(UUID.randomUUID().toString(), prenom, null);
    }

    /** Copie datée d'une nouvelle rencontre. */
    public Personne rencontreeLe(LocalDateTime instant) {
        return new Personne(id, prenom, instant);
    }
}
