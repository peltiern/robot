package fr.roboteek.robot.memoire;

import java.util.List;

public class ObjectDetectionResponse {

    private boolean objectFound;

    private List< DetectedObject > objects;

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


    @Override
    public String toString() {
        return "ObjectDetectionResponse{" +
                "objectFound=" + objectFound +
                ", objects=" + objects +
                '}';
    }
}
