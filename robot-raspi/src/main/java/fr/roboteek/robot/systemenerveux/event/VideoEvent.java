package fr.roboteek.robot.systemenerveux.event;

public class VideoEvent extends RobotEvent {

    public static final String EVENT_TYPE = "video";

    private String imageBase64;

    public VideoEvent() {
        super(EVENT_TYPE);
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
