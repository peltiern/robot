package fr.roboteek.robot.spring.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Services "programmés" pour pousser des informations dans le Websocket.
 */
@Service
public class PushService {

    /**
     * Template permmettant l'envoi de messages dans le Websocket.
     */
    private SimpMessagingTemplate simpMessagingTemplate;

    private int index;

    private final static Logger LOGGER = LoggerFactory.getLogger(PushService.class);

    public PushService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }
}
