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

    /** Identifiant de l'unique conversation du robot (mémoire persistante). */
    private static final String ID_CONVERSATION = "wall-e";

    /** Nombre maximal de messages conservés dans la fenêtre de mémoire. */
    private static final int TAILLE_MEMOIRE = 100;

    private static final String PROMPT_SYSTEME = "Dans cette discussion, tu t'appelleras Wall-E et tu seras un robot. "
            + "On est le %s. Tu répondras par des phrases courtes et rigolotes de moins de 15 mots. "
            + "Tes réponses sont lues à voix haute par une synthèse vocale : jamais d'emoji, de symbole ou de mise en forme. "
            + "Tu me tutoieras. Je suis Nicolas, né en 1981, ton créateur";

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
     * @param requete la requête demandée (une phrase)
     * @return la réponse issue de l'intelligence artificielle
     */
    public ReponseIntelligenceArtificielle repondreARequete(RequeteIntelligenceArtificielle requete) {

        if (StringUtils.isNotEmpty(requete.getInputText())) {
            return traiterRequeteTexte(requete.getInputText());
        }

        return null;
    }

    private ReponseIntelligenceArtificielle traiterRequeteTexte(String inputText) {
        String outputText = chatClient.prompt()
                // Prompt système à chaque appel pour que la date du jour reste juste
                .system(PROMPT_SYSTEME.formatted(LocalDate.now().format(FORMAT_DATE)))
                .user(inputText)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, ID_CONVERSATION))
                .call()
                .content();
        ReponseIntelligenceArtificielle response = new ReponseIntelligenceArtificielle();
        response.setInputText(inputText);
        response.setOutputText(outputText);
        return response;
    }
}
