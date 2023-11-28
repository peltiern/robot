package fr.roboteek.robot.spring.server.controller;

import com.google.gson.*;
import fr.roboteek.robot.systemenerveux.event.*;

import java.lang.reflect.Type;

public class RobotEventAdapter implements JsonDeserializer<RobotEvent>, JsonSerializer<RobotEvent> {

    private static final String PROPERTY_EVENT_TYPE = "eventType";

    @Override
    public RobotEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String eventType = json.getAsJsonObject().getAsJsonPrimitive(PROPERTY_EVENT_TYPE).getAsString();
        if (eventType.equals(MouvementCouEvent.EVENT_TYPE)) {
            return (RobotEvent) context.deserialize(json, MouvementCouEvent.class);
        } else if (eventType.equals(MouvementYeuxEvent.EVENT_TYPE)) {
            return (RobotEvent) context.deserialize(json, MouvementYeuxEvent.class);
        } else if (eventType.equals(ParoleEvent.EVENT_TYPE)) {
            return (RobotEvent) context.deserialize(json, ParoleEvent.class);
        } else if (eventType.equals(DisplayPositionEvent.EVENT_TYPE)) {
            return (RobotEvent) context.deserialize(json, DisplayPositionEvent.class);
        } else if (eventType.equals(PlayAnimationEvent.EVENT_TYPE)) {
            return (RobotEvent) context.deserialize(json, PlayAnimationEvent.class);
        }
        return null;
    }

    @Override
    public JsonElement serialize(RobotEvent robotEvent, Type arg1, JsonSerializationContext context) {
        if (robotEvent instanceof ConversationEvent) {
            return context.serialize((ConversationEvent) robotEvent);
        } else {
            return context.serialize(robotEvent);
        }
    }

}
