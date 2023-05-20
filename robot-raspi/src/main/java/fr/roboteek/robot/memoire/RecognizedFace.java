package fr.roboteek.robot.memoire;

public class RecognizedFace extends DetectedObject {

    private FaceLandmarks landmarks;

    public RecognizedFace() {
    }

    public RecognizedFace(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

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
