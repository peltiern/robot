package fr.roboteek.robot.activites.conversation.openai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import fr.roboteek.robot.activites.main.ReponseIntelligenceArtificielle;
import fr.roboteek.robot.activites.main.RequeteIntelligenceArtificielle;
import fr.roboteek.robot.configuration.ApiKeys;
import fr.roboteek.robot.memoire.conversation.openai.PersistentChatMemoryStore;
import org.apache.commons.lang3.StringUtils;

public class OpenAIConversation {

    private static final Integer MEMORY_ID = 1;

    private final PersistentChatMemoryStore store;

    private final ChatBot chatBot;

    public OpenAIConversation() {
        store = new PersistentChatMemoryStore();

        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(MEMORY_ID)
                .maxMessages(100)
                .chatMemoryStore(store)
                .build();

        chatBot = AiServices.builder(ChatBot.class)
                .chatLanguageModel(OpenAiChatModel
                        .builder()
                        .apiKey(ApiKeys.OPENAI_API_KEY)
                        .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                        .build()
                )
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    public void close() {
        store.close();
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
        ReponseIntelligenceArtificielle response = new ReponseIntelligenceArtificielle();
        response.setInputText(inputText);
        response.setOutputText(chatBot.chat(inputText));
        return response;
    }

    interface ChatBot {

        @SystemMessage("Dans cette discussion, tu t'appelleras Wall-E et tu seras un robot. On est le {{current_date}}. Tu répondras par des phrases courtes et rigolotes de moins de 15 mots. Tu me tutoieras. Je suis Nicolas, né en 1981, ton créateur")
        String chat(@UserMessage String userMessage);
    }
}
