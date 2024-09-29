package fr.roboteek.robot.services.providers.google.speech.recognizer.client.dto.request;

public class RecognitionConfig {

    private AudioEncoding encoding;
    private int sampleRateHertz;
    private Integer audioChannelCount;
    private String languageCode;

    public RecognitionConfig(AudioEncoding encoding, Integer sampleRateHertz, Integer audioChannelCount, String languageCode) {
        this.encoding = encoding;
        this.sampleRateHertz = sampleRateHertz;
        this.audioChannelCount = audioChannelCount;
        this.languageCode = languageCode;
    }

    public AudioEncoding getEncoding() {
        return encoding;
    }

    public void setEncoding(AudioEncoding encoding) {
        this.encoding = encoding;
    }

    public int getSampleRateHertz() {
        return sampleRateHertz;
    }

    public void setSampleRateHertz(int sampleRateHertz) {
        this.sampleRateHertz = sampleRateHertz;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Integer getAudioChannelCount() {
        return audioChannelCount;
    }

    public void setAudioChannelCount(Integer audioChannelCount) {
        this.audioChannelCount = audioChannelCount;
    }
}
