package fr.roboteek.robot.spring.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;

public class RobotEventMessageConverter implements MessageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RobotEventMessageConverter.class);
    private Gson gson;

    public RobotEventMessageConverter() {
        gson = new GsonBuilder().registerTypeAdapter(RobotEvent.class, new RobotEventAdapter()).create();
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        if (RobotEvent.class == targetClass) {
            RobotEvent robotEvent = null;
            if (message.getPayload() instanceof byte[]) {
                robotEvent = gson.fromJson(new String((byte[]) message.getPayload()), RobotEvent.class);
            } else if (message.getPayload() instanceof String) {
                robotEvent = gson.fromJson((String) message.getPayload(), RobotEvent.class);
            }
            return robotEvent;
        }
        return null;
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        return null;
    }
}
