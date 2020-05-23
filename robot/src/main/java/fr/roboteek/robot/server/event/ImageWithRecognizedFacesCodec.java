package fr.roboteek.robot.server.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.roboteek.robot.server.ImageWithDetectedObjects;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ImageWithRecognizedFacesCodec implements Encoder.Text<ImageWithDetectedObjects> {

    private Gson gson;

    @Override
    public void init(EndpointConfig config) {
        gson = new GsonBuilder().create();
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public String encode(ImageWithDetectedObjects object) {
        return gson.toJson(object);
    }

}
