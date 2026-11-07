package fr.roboteek.robot.spring.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vérifie la requête que Spring AI envoie vraiment, sans appeler l'IA payante : un faux serveur
 * Anthropic local la capture et renvoie une réponse toute faite.
 */
class IntelligenceArtificielleConfigTest {

    private static final String REPONSE = """
            {"id": "msg_test", "type": "message", "role": "assistant", "model": "claude-haiku-5-5",
             "content": [{"type": "text", "text": "Bonjour"}],
             "stop_reason": "end_turn", "stop_sequence": null,
             "usage": {"input_tokens": 10, "output_tokens": 2}}
            """;

    private HttpServer serveur;

    private final AtomicReference<String> requeteRecue = new AtomicReference<>();

    @BeforeEach
    void demarrerLeFauxServeur() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", echange -> {
            requeteRecue.set(new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] corps = REPONSE.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().add("Content-Type", "application/json");
            echange.sendResponseHeaders(200, corps.length);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(corps);
            }
        });
        serveur.start();
    }

    @AfterEach
    void arreterLeFauxServeur() {
        serveur.stop(0);
    }

    /**
     * Le réglage du robot ne doit rien écraser : le modèle vient de {@code application.properties},
     * la réflexion coupée du réglage commun, et la requête porte les deux.
     */
    @Test
    void laRequeteGardeLeModeleEtCoupeLaReflexion() {
        AnthropicChatModel modele = AnthropicChatModel.builder()
                .options(AnthropicChatOptions.builder()
                        .model("claude-haiku-5-5")
                        .baseUrl("http://127.0.0.1:" + serveur.getAddress().getPort())
                        .apiKey("cle-de-test")
                        .build())
                .build();
        ChatClient.Builder builder = ChatClient.builder(modele);
        new IntelligenceArtificielleConfig().reflexionDesactivee().customize(builder);

        String reponse = builder.build().prompt().user("Salut").call().content();

        assertEquals("Bonjour", reponse);
        JsonNode requete = JsonMapper.builder().build().readTree(requeteRecue.get());
        assertEquals("claude-haiku-5-5", requete.path("model").asString());
        assertEquals("disabled", requete.path("thinking").path("type").asString(),
                "la réflexion doit être coupée dans la requête : " + requeteRecue.get());
    }
}
