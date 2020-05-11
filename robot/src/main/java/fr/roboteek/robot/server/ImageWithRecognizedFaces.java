package fr.roboteek.robot.server;

import fr.roboteek.robot.memoire.RecognizedFace;

import java.io.Serializable;
import java.util.List;

public class ImageWithRecognizedFaces implements Serializable {

    private static final long serialVersionUID = 4975770080221811676L;

    private boolean faceFound;

    private List<RecognizedFace> faces;

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


    @Override
    public String toString() {
        return "ImageWithDetectedFaces{" +
                "faceFound=" + faceFound +
                ", faces=" + faces +
                '}';
    }
}
