package fr.roboteek.robot.memoire.conversation.openai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import fr.roboteek.robot.Constantes;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Classe permettant de stocker les conversations "Langchain / Open AI" en base de données pour une utilisation ultérieure.
 */
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final DB db = DBMaker.fileDB(Constantes.DOSSIER_MEMOIRE_CONVERSATIONS + File.separator + "memoire-conversations-openai.db").transactionEnable().make();
    private final Map<Integer, String> map = db.hashMap("messages", Serializer.INTEGER, Serializer.STRING).createOrOpen();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = map.get((int) memoryId);
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        map.put((int) memoryId, json);
        db.commit();
    }

    @Override
    public void deleteMessages(Object memoryId) {
        map.remove((int) memoryId);
        db.commit();
    }

    public void close() {
        db.close();
    }
}
