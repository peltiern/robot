package fr.roboteek.robot.memoire.conversation;

import fr.roboteek.robot.Constantes;
import jakarta.annotation.PreDestroy;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stockage persistant (MapDB) des conversations avec l'IA, pour reprendre le fil
 * d'une session à l'autre.
 * <p>
 * Remplace l'ancien {@code PersistentChatMemoryStore} (langchain4j) : même base MapDB,
 * mais branché sur l'interface {@link ChatMemoryRepository} de Spring AI.
 * La base est fermée proprement à l'arrêt du contexte Spring ({@link #close()}).
 */
@Component
public class MapDbChatMemoryRepository implements ChatMemoryRepository {

    private final DB db;
    private final Map<String, Object> map;

    public MapDbChatMemoryRepository() {
        db = DBMaker.fileDB(Constantes.DOSSIER_MEMOIRE_CONVERSATIONS + File.separator + "memoire-conversations.db").transactionEnable().make();
        map = db.hashMap("messages", Serializer.STRING, Serializer.JAVA).createOrOpen();
    }

    @Override
    public List<String> findConversationIds() {
        return new ArrayList<>(map.keySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> findByConversationId(String conversationId) {
        ArrayList<MessageSerialise> messagesSerialises = (ArrayList<MessageSerialise>) map.get(conversationId);
        List<Message> messages = new ArrayList<>();
        if (messagesSerialises != null) {
            for (MessageSerialise messageSerialise : messagesSerialises) {
                switch (messageSerialise.type()) {
                    case "USER" -> messages.add(new UserMessage(messageSerialise.texte()));
                    case "ASSISTANT" -> messages.add(new AssistantMessage(messageSerialise.texte()));
                    case "SYSTEM" -> messages.add(new SystemMessage(messageSerialise.texte()));
                    default -> { /* messages outils : non persistés */ }
                }
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        ArrayList<MessageSerialise> messagesSerialises = new ArrayList<>();
        for (Message message : messages) {
            String type = message.getMessageType().name();
            if (type.equals("USER") || type.equals("ASSISTANT") || type.equals("SYSTEM")) {
                messagesSerialises.add(new MessageSerialise(type, message.getText()));
            }
        }
        map.put(conversationId, messagesSerialises);
        db.commit();
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        map.remove(conversationId);
        db.commit();
    }

    @PreDestroy
    public void close() {
        db.close();
    }

    /** Forme sérialisable d'un message de conversation (type + texte). */
    private record MessageSerialise(String type, String texte) implements Serializable {
    }
}
