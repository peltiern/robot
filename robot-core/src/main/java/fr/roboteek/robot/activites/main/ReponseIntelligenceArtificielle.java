package fr.roboteek.robot.activites.main;

public class ReponseIntelligenceArtificielle {

    private String inputText;

    private String intent;

    private boolean fallback;

    private String outputText;

    private byte[] outputAudio;

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }

    public byte[] getOutputAudio() {
        return outputAudio;
    }

    public void setOutputAudio(byte[] outputAudio) {
        this.outputAudio = outputAudio;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }


    @Override
    public String toString() {
        return "ReponseIntelligenceArtificielle{" +
                "inputText='" + inputText + '\'' +
                ", intent='" + intent + '\'' +
                ", fallback=" + fallback +
                ", outputText='" + outputText + '\'' +
                '}';
    }
}
