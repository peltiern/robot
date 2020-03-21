package fr.roboteek.robot.decisionnel;

public class ReponseIntelligenceArtificielle {

    private String inputText;

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
}
