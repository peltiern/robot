package fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.request;

public class AudioConfig {
    private AudioEncoding audioEncoding;

    public AudioConfig(AudioEncoding audioEncoding) {
        this.audioEncoding = audioEncoding;
    }

    public AudioEncoding getAudioEncoding() {
        return audioEncoding;
    }

    public void setAudioEncoding(AudioEncoding audioEncoding) {
        this.audioEncoding = audioEncoding;
    }
}
