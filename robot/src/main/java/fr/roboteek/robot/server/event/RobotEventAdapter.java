package fr.roboteek.robot.server.event;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import fr.roboteek.robot.systemenerveux.event.ExpressionVisageEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementTeteEvent;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;

public class RobotEventAdapter implements JsonDeserializer<RobotEvent> {

	private static final String PROPERTY_EVENT_TYPE = "eventType";

	@Override
	public RobotEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		String eventType = json.getAsJsonObject().getAsJsonPrimitive(PROPERTY_EVENT_TYPE).getAsString();
		if (eventType.equals(MouvementTeteEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, MouvementTeteEvent.class);
		} else if (eventType.equals(ParoleEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, ParoleEvent.class);
		} else if (eventType.equals(ExpressionVisageEvent.EVENT_TYPE)) {
			return (RobotEvent) context.deserialize(json, ExpressionVisageEvent.class);
		}
		return null;
	}

}
