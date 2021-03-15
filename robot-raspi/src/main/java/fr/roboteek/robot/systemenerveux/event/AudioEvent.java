package fr.roboteek.robot.systemenerveux.event;

public class AudioEvent extends RobotEvent {

    public static final String EVENT_TYPE = "audio";

    private String audioContentBase64;
    private byte[] content;

    public AudioEvent() {
        super(EVENT_TYPE);
    }

    public String getAudioContentBase64() {
        return audioContentBase64;
    }

    public void setAudioContentBase64(String audioContentBase64) {
        this.audioContentBase64 = audioContentBase64;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
