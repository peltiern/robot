package fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.request;

public class SynthesizeRequest {

    private Input input;
    private Voice voice;
    private AudioConfig audioConfig;

    public SynthesizeRequest(Input input, Voice voice, AudioConfig audioConfig) {
        this.input = input;
        this.voice = voice;
        this.audioConfig = audioConfig;
    }

    public Input getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input;
    }

    public Voice getVoice() {
        return voice;
    }

    public void setVoice(Voice voice) {
        this.voice = voice;
    }

    public AudioConfig getAudioConfig() {
        return audioConfig;
    }

    public void setAudioConfig(AudioConfig audioConfig) {
        this.audioConfig = audioConfig;
    }
}
