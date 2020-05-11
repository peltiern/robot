package fr.roboteek.robot.memoire;

import java.util.List;

public class FacialRecognitionResponse {

    private boolean faceFound;

    private List< RecognizedFace > faces;

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


    @Override
    public String toString() {
        return "FacialRecognitionResponse{" +
                "faceFound=" + faceFound +
                ", faces=" + faces +
                '}';
    }
}
