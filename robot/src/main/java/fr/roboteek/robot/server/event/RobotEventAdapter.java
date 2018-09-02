package fr.roboteek.robot.server.event;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import fr.roboteek.robot.systemenerveux.event.ConversationEvent;
import fr.roboteek.robot.systemenerveux.event.ExpressionVisageEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementTeteEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;

public class RobotEventAdapter implements JsonDeserializer<RobotEvent>, JsonSerializer<RobotEvent> {

	private static final String PROPERTY_EVENT_TYPE = "eventType";

	@Override
	public RobotEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		String eventType = json.getAsJsonObject().getAsJsonPrimitive(PROPERTY_EVENT_TYPE).getAsString();
		if (eventType.equals(MouvementTeteEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, MouvementTeteEvent.class);
		} else if (eventType.equals(MouvementCouEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, MouvementCouEvent.class);
		} else if (eventType.equals(MouvementYeuxEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, MouvementYeuxEvent.class);
		} else if (eventType.equals(ParoleEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, ParoleEvent.class);
		} else if (eventType.equals(ExpressionVisageEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, ExpressionVisageEvent.class);
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
