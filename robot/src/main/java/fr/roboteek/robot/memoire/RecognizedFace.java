package fr.roboteek.robot.memoire;

public class RecognizedFace extends DetectedObject {

    private FaceLandmarks landmarks;

    public FaceLandmarks getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(FaceLandmarks landmarks) {
        this.landmarks = landmarks;
    }


    @Override
    public String toString() {
        return "RecognizedFace{" +
                "landmarks=" + landmarks +
                "} " + super.toString();
    }
}
