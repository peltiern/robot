package fr.roboteek.robot.memoire.longterme.conversation;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès à la table {@code message} : les conversations tenues avec l'IA, pour reprendre le fil
 * d'une session à l'autre.
 * <p>
 * Branché sur {@link ChatMemoryRepository} de Spring AI, qui raisonne en fils identifiés par une
 * clé : {@code personne-<id>} pour quelqu'un de connu, {@code wall-e} pour un interlocuteur non
 * identifié, {@code chauffe} pour l'échange sans intérêt du démarrage. C'est pourquoi la table n'a
 * pas de clé étrangère vers {@code personne} : tous les fils n'appartiennent pas à quelqu'un.
 * <p>
 * Anciennement {@code MapDbChatMemoryRepository} : le nom portait la technologie de stockage, ce
 * qui n'a jamais rien appris à personne sur ce que fait cette classe.
 */
@Component
public class ConversationRepository implements ChatMemoryRepository {

    /** Préfixe des fils rattachés à une personne identifiée. */
    private static final String PREFIXE_PERSONNE = "personne-";

    private final JdbcClient jdbc;

    public ConversationRepository(DataSource sourceDeDonneesMemoire) {
        this.jdbc = JdbcClient.create(sourceDeDonneesMemoire);
    }

    /**
     * La clé du fil d'une personne, bâtie sur son seul identifiant.
     * <p>
     * Le prénom y figurait autrefois, en simple étiquette, pour s'y retrouver en ouvrant la base.
     * Il en a été retiré le 2026-08-16 : renommer quelqu'un changeait sa clé, et le robot repartait
     * d'une page blanche avec lui sans que rien ne le signale — précisément au moment où l'on
     * corrige un prénom mal compris, donc où l'on tient le plus à ce qu'il se souvienne. Se
     * retrouver dans la base est désormais l'affaire d'une jointure.
     */
    public static String idConversationDe(String idPersonne) {
        return PREFIXE_PERSONNE + idPersonne;
    }

    @Override
    public List<String> findConversationIds() {
        return jdbc.sql("SELECT DISTINCT id_conversation FROM message").query(String.class).list();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<Message> messages = new ArrayList<>();
        jdbc.sql("SELECT type, texte FROM message WHERE id_conversation = ? ORDER BY rang")
                .param(conversationId)
                .query((ligne, numeroLigne) -> reconstituer(ligne.getString("type"), ligne.getString("texte")))
                .list()
                .forEach(message -> {
                    if (message != null) {
                        messages.add(message);
                    }
                });
        return messages;
    }

    /**
     * Réécrit le fil entier, comme le veut le contrat de Spring AI : la fenêtre de mémoire lui
     * transmet la liste complète, déjà tronquée aux cent derniers messages.
     * <p>
     * Transactionnel, et il le faut : entre l'effacement et la réinsertion, le fil n'existe plus.
     * Une coupure à cet instant précis effacerait la mémoire que le robot a de quelqu'un.
     */
    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        deleteByConversationId(conversationId);
        int rang = 0;
        for (Message message : messages) {
            String type = message.getMessageType().name();
            // Les messages d'outils ne sont pas persistés : ils n'ont de sens que dans l'échange
            // qui les a produits.
            if (!type.equals("USER") && !type.equals("ASSISTANT") && !type.equals("SYSTEM")) {
                continue;
            }
            jdbc.sql("INSERT INTO message (id_conversation, rang, type, texte) VALUES (?, ?, ?, ?)")
                    .params(conversationId, rang++, type, message.getText())
                    .update();
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        jdbc.sql("DELETE FROM message WHERE id_conversation = ?").param(conversationId).update();
    }

    private static Message reconstituer(String type, String texte) {
        return switch (type) {
            case "USER" -> new UserMessage(texte);
            case "ASSISTANT" -> new AssistantMessage(texte);
            case "SYSTEM" -> new SystemMessage(texte);
            default -> null;
        };
    }
}
