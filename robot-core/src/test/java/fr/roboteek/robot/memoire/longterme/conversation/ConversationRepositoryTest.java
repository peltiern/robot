package fr.roboteek.robot.memoire.longterme.conversation;

import fr.roboteek.robot.memoire.longterme.BaseMemoireDeTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vérifie la persistance des conversations, sans appeler la moindre IA. */
class ConversationRepositoryTest {

    @TempDir
    File dossierTemp;

    private ConversationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ConversationRepository(BaseMemoireDeTest.dans(dossierTemp));
    }

    @Test
    void unFilInconnuEstVide() {
        assertTrue(repository.findByConversationId("personne-inexistante").isEmpty());
        assertTrue(repository.findConversationIds().isEmpty());
    }

    /**
     * L'ordre est le sens même d'une conversation : relue à l'envers, elle ferait répondre le
     * robot avant qu'on lui parle.
     */
    @Test
    void lesMessagesSeRelisentDansLordreOuIlsOntEteEcrits() {
        repository.saveAll("personne-1", List.of(
                new SystemMessage("Tu es Wall-E."),
                new UserMessage("Bonjour"),
                new AssistantMessage("Salut !"),
                new UserMessage("Ça va ?")));

        List<Message> relus = repository.findByConversationId("personne-1");

        assertEquals(4, relus.size());
        assertEquals(List.of("Tu es Wall-E.", "Bonjour", "Salut !", "Ça va ?"),
                relus.stream().map(Message::getText).toList());
        assertEquals(List.of("SYSTEM", "USER", "ASSISTANT", "USER"),
                relus.stream().map(message -> message.getMessageType().name()).toList());
    }

    /**
     * Spring AI transmet le fil entier à chaque tour, déjà tronqué à sa fenêtre de mémoire :
     * enregistrer doit remplacer, jamais ajouter, sinon chaque échange dupliquerait tout le passé.
     */
    @Test
    void enregistrerRemplaceLeFilEntier() {
        repository.saveAll("personne-1", List.of(new UserMessage("Bonjour"), new AssistantMessage("Salut !")));
        repository.saveAll("personne-1", List.of(new AssistantMessage("Salut !"), new UserMessage("Ça va ?")));

        List<Message> relus = repository.findByConversationId("personne-1");

        assertEquals(List.of("Salut !", "Ça va ?"), relus.stream().map(Message::getText).toList());
    }

    @Test
    void lesFilsRestentSepares() {
        repository.saveAll("personne-1", List.of(new UserMessage("Bonjour")));
        repository.saveAll("wall-e", List.of(new UserMessage("Autre chose")));

        assertEquals(List.of("Bonjour"),
                repository.findByConversationId("personne-1").stream().map(Message::getText).toList());
        assertEquals(2, repository.findConversationIds().size());
    }

    @Test
    void effacerUnFilNeTouchePasLesAutres() {
        repository.saveAll("personne-1", List.of(new UserMessage("Bonjour")));
        repository.saveAll("personne-2", List.of(new UserMessage("Coucou")));

        repository.deleteByConversationId("personne-1");

        assertTrue(repository.findByConversationId("personne-1").isEmpty());
        assertEquals(1, repository.findByConversationId("personne-2").size());
    }
}
