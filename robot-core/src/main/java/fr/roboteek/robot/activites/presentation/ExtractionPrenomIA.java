package fr.roboteek.robot.activites.presentation;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Tire un prénom de ce que la reconnaissance vocale a compris.
 * <p>
 * <b>Pourquoi l'IA ici, alors que le reste de l'activité s'en passe</b> : personne ne répond
 * « Marie » à « comment tu t'appelles ? ». Vosk rend « je m'appelle nicolas », « moi c'est marie »,
 * « euh marie », « alors moi c'est jean pierre », sans ponctuation ni majuscule. Une expression
 * régulière couvre les tournures qu'on a prévues et rate les autres — c'est-à-dire précisément
 * celles qui comptent. Le coût, une seconde de latence, est ici invisible : le robot enchaîne de
 * toute façon sur une question de confirmation.
 * <p>
 * Volontairement <b>sans mémoire de conversation</b> : ce n'est pas un échange, c'est une analyse
 * de texte. L'y mêler polluerait le fil de la personne avec des consignes d'extraction.
 */
@Component
public class ExtractionPrenomIA {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionPrenomIA.class);

    private static final String PROMPT_SYSTEME = """
            Tu analyses la réponse d'une personne à qui un robot vient de demander son prénom.
            Le texte vient d'une reconnaissance vocale imparfaite : tout en minuscules, sans
            ponctuation, avec des hésitations et parfois des mots mal transcrits.
            Extrais le prénom, et lui seul, correctement orthographié et avec sa majuscule.
            Ignore les formules qui l'entourent (« je m'appelle », « moi c'est », « euh »).
            Si la phrase ne contient aucun prénom plausible, indique-le au lieu d'inventer.
            """;

    /**
     * Ce que l'IA a cru comprendre.
     *
     * @param contientUnPrenom vrai si la phrase contient bien un prénom
     * @param prenom           le prénom, correctement orthographié ; vide sinon
     */
    public record PrenomEntendu(boolean contientUnPrenom, String prenom) {
    }

    private final ChatClient chatClient;

    public ExtractionPrenomIA(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * @param texteEntendu ce que la reconnaissance vocale a rendu
     * @return le prénom, ou {@code null} s'il n'y en a pas — y compris si l'IA est injoignable :
     * ne pas savoir extraire un prénom est un cas ordinaire, que l'appelant traite en relançant
     */
    public String extraire(String texteEntendu) {
        if (StringUtils.isBlank(texteEntendu)) {
            return null;
        }
        try {
            PrenomEntendu prenomEntendu = chatClient.prompt()
                    .system(PROMPT_SYSTEME)
                    .user(texteEntendu)
                    .call()
                    .entity(PrenomEntendu.class);
            if (prenomEntendu == null || !prenomEntendu.contientUnPrenom() || StringUtils.isBlank(prenomEntendu.prenom())) {
                logger.info("Aucun prénom trouvé dans « {} »", texteEntendu);
                return null;
            }
            logger.info("Prénom extrait de « {} » : {}", texteEntendu, prenomEntendu.prenom());
            return prenomEntendu.prenom().trim();
        } catch (Exception e) {
            // Réseau coupé, quota épuisé, réponse inexploitable : le robot relancera, ce qui vaut
            // mieux que de laisser remonter l'exception et d'interrompre la rencontre.
            logger.error("Échec de l'extraction du prénom depuis « {} »", texteEntendu, e);
            return null;
        }
    }
}
