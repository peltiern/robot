package fr.roboteek.robot.server;

import fr.roboteek.robot.memoire.DetectedObject;
import fr.roboteek.robot.memoire.RecognizedFace;

import java.io.Serializable;
import java.util.List;

public class ImageWithDetectedObjects implements Serializable {

    private static final long serialVersionUID = 4975770080221811676L;

    private boolean faceFound;

    private List<RecognizedFace> faces;

    private boolean objectFound;

    private List<DetectedObject> objects;

    private String imageBase64;

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

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
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


    @Override
    public String toString() {
        return "ImageWithDetectedObjects{" +
                "faceFound=" + faceFound +
                ", faces=" + faces +
                ", objectFound=" + objectFound +
                ", objects=" + objects +
                '}';
    }
}
