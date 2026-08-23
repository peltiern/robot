package fr.roboteek.robot.memoire.longterme.personne;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quelqu'un que le robot connaît : mémoire longue, par opposition à la présence du moment
 * que tient {@code RegistrePresence}.
 * <p>
 * L'identité est portée par {@link #id}, pas par le prénom : deux personnes peuvent
 * s'appeler Marie, et une même personne peut voir son prénom corrigé après une
 * reconnaissance vocale approximative. C'est cet identifiant que référencent les empreintes
 * biométriques ({@code VisageConnu}), le journal des rencontres et le fil de conversation.
 * <p>
 * <b>Rien n'est stocké ici du contenu des échanges.</b> {@code ConversationRepository} tient le
 * fil de chacun, cent messages durant : le robot a déjà de quoi se souvenir, bien mieux qu'une
 * phrase de résumé. Ne pas ajouter de résumé ici sans avoir constaté que ce fil ne suffit pas.
 *
 * @param id                identifiant stable, jamais réutilisé
 * @param prenom            prénom tel que la personne l'a donné
 * @param derniereRencontre date de la dernière rencontre, {@code null} si jamais rencontrée
 */
public record Personne(String id, String prenom, LocalDateTime derniereRencontre) {

    /** Crée une personne encore jamais rencontrée. */
    public static Personne nouvelle(String prenom) {
        return new Personne(UUID.randomUUID().toString(), prenom, null);
    }

    /** Copie datée d'une nouvelle rencontre. */
    public Personne rencontreeLe(LocalDateTime instant) {
        return new Personne(id, prenom, instant);
    }

    /** Copie portant un autre prénom, quand celui retenu était mal compris. */
    public Personne renommee(String nouveauPrenom) {
        return new Personne(id, nouveauPrenom, derniereRencontre);
    }
}
