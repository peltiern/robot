package fr.roboteek.robot.server.event;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.roboteek.robot.systemenerveux.event.RobotEvent;

public class RobotEventCodec implements Decoder.Text<RobotEvent>, Encoder.Text<RobotEvent> {

	@Override
	public void init(EndpointConfig config) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RobotEvent decode(String s) throws DecodeException {
		Gson gson = new GsonBuilder().registerTypeAdapter(RobotEvent.class, new RobotEventAdapter()).create();
		return gson.fromJson(s, RobotEvent.class);
	}

	@Override
	public boolean willDecode(String s) {
		return (s != null);
	}

	@Override
	public String encode(RobotEvent object) throws EncodeException {
		Gson gson = new GsonBuilder().registerTypeAdapter(RobotEvent.class, new RobotEventAdapter()).create();
		return gson.toJson(object);
	}

}
