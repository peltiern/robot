package fr.roboteek.robot.activites.conversation;

import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.memoire.conversation.MapDbChatMemoryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Conversation avec l'IA (Claude via Spring AI).
 * <p>
 * Remplace l'ancien {@code OpenAIConversation} (langchain4j) : le modèle est configuré
 * dans {@code application.properties} ({@code spring.ai.anthropic.*}, clé d'API lue dans
 * le {@code .env}), la mémoire de conversation persiste dans MapDB via
 * {@link MapDbChatMemoryRepository}.
 */
@Component
public class ConversationIA {

    /**
     * Identifiant de la conversation tenue avec un interlocuteur non identifié.
     * <p>
     * Valeur historique conservée : c'est la clé sous laquelle toute la mémoire MapDB a été
     * écrite jusqu'ici, en changer ferait repartir le robot de zéro.
     */
    private static final String ID_CONVERSATION_PAR_DEFAUT = "wall-e";

    /** Préfixe des conversations rattachées à une personne identifiée. */
    private static final String PREFIXE_CONVERSATION_PERSONNE = "personne-";

    /** Nombre maximal de messages conservés dans la fenêtre de mémoire. */
    private static final int TAILLE_MEMOIRE = 100;

    private static final String PROMPT_SYSTEME = "Dans cette discussion, tu t'appelleras Wall-E et tu seras un robot. "
            + "On est le %s. Tu répondras par des phrases courtes et rigolotes de moins de 15 mots. "
            + "Tes réponses sont lues à voix haute par une synthèse vocale : jamais d'emoji, de symbole ou de mise en forme. "
            + "Tu tutoieras ton interlocuteur. %s";

    /** Interlocuteur non identifié : le robot suppose que c'est son créateur, comme avant. */
    private static final String INTERLOCUTEUR_PAR_DEFAUT = "Tu parles avec Nicolas, né en 1981, ton créateur.";

    /** Interlocuteur identifié : Nicolas n'est alors plus qu'un fait à connaître. */
    private static final String INTERLOCUTEUR_IDENTIFIE = "Tu parles avec %s. Tu as été créé par Nicolas, né en 1981.";

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private final ChatClient chatClient;

    public ConversationIA(ChatClient.Builder chatClientBuilder, MapDbChatMemoryRepository depotMemoireConversation) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(depotMemoireConversation)
                .maxMessages(TAILLE_MEMOIRE)
                .build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Répond à une requête.
     *
     * @param requete      la requête demandée (une phrase)
     * @param interlocuteur prénom de la personne qui parle, {@code null} si elle n'est pas identifiée
     * @return la réponse issue de l'intelligence artificielle
     */
    public ReponseIntelligenceArtificielle repondreARequete(RequeteIntelligenceArtificielle requete, String interlocuteur) {

        if (StringUtils.isNotEmpty(requete.getInputText())) {
            return traiterRequeteTexte(requete.getInputText(), interlocuteur);
        }

        return null;
    }

    private ReponseIntelligenceArtificielle traiterRequeteTexte(String inputText, String interlocuteur) {
        String outputText = chatClient.prompt()
                // Prompt système à chaque appel pour que la date du jour reste juste
                .system(PROMPT_SYSTEME.formatted(LocalDate.now().format(FORMAT_DATE), presentationInterlocuteur(interlocuteur)))
                .user(inputText)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, idConversation(interlocuteur)))
                .call()
                .content();
        ReponseIntelligenceArtificielle response = new ReponseIntelligenceArtificielle();
        response.setInputText(inputText);
        response.setOutputText(outputText);
        return response;
    }

    /**
     * Identifiant de la conversation propre à l'interlocuteur : chacun a sa mémoire.
     * <p>
     * Cet identifiant était en dur : tout le monde partageait le même fil, et le robot pouvait
     * resservir à Marie ce que Paul venait de lui raconter.
     */
    private static String idConversation(String interlocuteur) {
        if (StringUtils.isBlank(interlocuteur)) {
            return ID_CONVERSATION_PAR_DEFAUT;
        }
        return PREFIXE_CONVERSATION_PERSONNE + interlocuteur.trim().toLowerCase(Locale.FRENCH);
    }

    /** Phrase du prompt système présentant la personne en face du robot. */
    private static String presentationInterlocuteur(String interlocuteur) {
        if (StringUtils.isBlank(interlocuteur)) {
            return INTERLOCUTEUR_PAR_DEFAUT;
        }
        return INTERLOCUTEUR_IDENTIFIE.formatted(interlocuteur.trim());
    }
}
