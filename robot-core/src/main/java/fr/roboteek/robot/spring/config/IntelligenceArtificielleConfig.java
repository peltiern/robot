package fr.roboteek.robot.spring.config;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Réglages communs à tout ce qui parle à l'IA : appliqués à chaque {@code ChatClient.Builder}
 * que Spring fournit, donc à la conversation comme à l'extraction du prénom.
 */
@Configuration
public class IntelligenceArtificielleConfig {

    /**
     * La réflexion est coupée : le robot fait de la conversation courte, où elle n'apporterait
     * qu'un délai avant de parler et des tokens facturés.
     * <p>
     * Et surtout, elle le rendrait muet. Claude Haiku 5.5 réfléchit par défaut, et Spring AI 2.0
     * range chaque bloc de réflexion dans un résultat à part, placé <b>avant</b> la réponse — vide,
     * puisque le texte de la réflexion n'est pas renvoyé. {@code .call().content()} lit le premier
     * résultat : la conversation aurait reçu une chaîne vide, l'extraction du prénom un JSON
     * illisible. Claude Haiku 4.5 ne réfléchissait que sur demande, d'où un problème invisible
     * jusqu'au changement de modèle (2026-11-07).
     * <p>
     * Ce n'est pas un réglage de {@code application.properties} : Spring AI y attend un objet du
     * SDK Anthropic, que le fichier ne sait pas écrire.
     */
    @Bean
    ChatClientCustomizer reflexionDesactivee() {
        return builder -> builder.defaultOptions(AnthropicChatOptions.builder().thinkingDisabled());
    }
}
