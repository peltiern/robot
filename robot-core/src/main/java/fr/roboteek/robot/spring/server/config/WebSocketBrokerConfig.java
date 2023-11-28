package fr.roboteek.robot.spring.server.config;

import fr.roboteek.robot.spring.server.controller.RobotEventMessageConverter;
import fr.roboteek.robot.spring.server.websocket.WebsocketBroadcaster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
@EnableWebSocket
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/temperature", "/image", "/events", "/video", "/audio");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint du Websocket
        registry.addEndpoint("/wsendpoint").setAllowedOrigins("*");
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
