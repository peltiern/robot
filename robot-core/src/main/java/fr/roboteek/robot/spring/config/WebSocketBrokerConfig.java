package fr.roboteek.robot.spring.config;

import fr.roboteek.robot.web.controller.RobotEventMessageConverter;
import fr.roboteek.robot.web.websocket.WebsocketBroadcaster;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
@EnableWebSocket
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer, DisposableBean {

    /** Période des battements de cœur STOMP, dans les deux sens (ms). */
    private static final long PERIODE_BATTEMENTS_MS = 10_000;

    /**
     * Ordonnanceur dédié aux battements de cœur.
     * <p>
     * Instancié à la main, et non injecté : le bean {@code messageBrokerTaskScheduler} fourni par
     * {@code @EnableWebSocketMessageBroker} conviendrait, mais le demander depuis cette classe —
     * qui est elle-même un {@link WebSocketMessageBrokerConfigurer} — crée une dépendance
     * circulaire, refusée par défaut depuis Spring Boot 2.6. Un thread de plus est moins cher
     * qu'une configuration fragile au démarrage.
     */
    private final ThreadPoolTaskScheduler ordonnanceurBattements = creerOrdonnanceurBattements();

    private static ThreadPoolTaskScheduler creerOrdonnanceurBattements() {
        ThreadPoolTaskScheduler ordonnanceur = new ThreadPoolTaskScheduler();
        ordonnanceur.setPoolSize(1);
        ordonnanceur.setThreadNamePrefix("ws-battements-");
        ordonnanceur.setDaemon(true);
        ordonnanceur.initialize();
        return ordonnanceur;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        // Battements de cœur : sans ordonnanceur, le broker simple n'en envoie aucun, et une
        // liaison qui meurt sans se fermer (WiFi du robot qui tombe, tablette qui sort de portée)
        // reste ouverte des deux côtés. L'interface continuait alors d'afficher « en ligne » avec
        // la dernière image et les dernières mesures reçues, figées, jusqu'au rechargement de la
        // page. Avec 10 s dans les deux sens, chacun s'aperçoit de la coupure et se reconnecte.
        registry.enableSimpleBroker("/temperature", "/image", "/events", "/video", "/audio")
                .setTaskScheduler(ordonnanceurBattements)
                .setHeartbeatValue(new long[]{PERIODE_BATTEMENTS_MS, PERIODE_BATTEMENTS_MS});
    }

    @Override
    public void destroy() {
        ordonnanceurBattements.shutdown();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint du Websocket
        registry.addEndpoint("/wsendpoint").setAllowedOrigins("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Les trames vidéo (JPEG en base64) dépassent la limite par défaut de 512 Ko et coupaient
        // la session (« Buffer size ... exceeds the allowed limit 524288 »). On relève les limites.
        registration.setMessageSizeLimit(2 * 1024 * 1024);      // 2 Mo (messages entrants)
        registration.setSendBufferSizeLimit(8 * 1024 * 1024);   // 8 Mo (tampon d'envoi par session)
        registration.setSendTimeLimit(20 * 1000);               // 20 s pour vider le tampon
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new RobotEventMessageConverter());
        return true;
    }

    @Bean(name = "websocketBroadcaster")
    public WebsocketBroadcaster websocketBroadcaster(SimpMessagingTemplate simpMessagingTemplate) {
        return new WebsocketBroadcaster(simpMessagingTemplate);
    }
}
