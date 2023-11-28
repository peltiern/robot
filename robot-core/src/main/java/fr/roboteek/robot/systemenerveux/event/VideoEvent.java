package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.memoire.DetectedObject;
import fr.roboteek.robot.memoire.RecognizedFace;

import java.util.List;

public class VideoEvent extends RobotEvent {

    public static final String EVENT_TYPE = "video";

    private String imageBase64;

    private boolean faceFound;

    private List<RecognizedFace> faces;

    private boolean objectFound;

    private List<DetectedObject> objects;

    public VideoEvent() {
        super(EVENT_TYPE);
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public boolean isFaceFound() {
        return faceFound;
    }

    public void setFaceFound(boolean faceFound) {
        this.faceFound = faceFound;
    }

    public List<RecognizedFace> getFaces() {
        return faces;
    }

    public void setFaces(List<RecognizedFace> faces) {
        this.faces = faces;
    }

    public boolean isObjectFound() {
        return objectFound;
    }

    public void setObjectFound(boolean objectFound) {
        this.objectFound = objectFound;
    }

    public List<DetectedObject> getObjects() {
        return objects;
    }

    public void setObjects(List<DetectedObject> objects) {
        this.objects = objects;
    }
}
