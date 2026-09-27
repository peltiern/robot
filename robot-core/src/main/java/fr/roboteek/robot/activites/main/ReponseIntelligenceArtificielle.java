package fr.roboteek.robot.activites.main;

import fr.roboteek.robot.decisionnel.emotion.Emotion;

public class ReponseIntelligenceArtificielle {

    private String inputText;

    private String intent;

    private boolean fallback;

    private String outputText;

    private byte[] outputAudio;

    /** Ce que la phrase de l'interlocuteur a fait ressentir au robot ; neutre par défaut. */
    private Emotion emotion = Emotion.NEUTRE;

    /** De 0 à 1. */
    private double intensite;

    public Emotion getEmotion() {
        return emotion;
    }

    public void setEmotion(Emotion emotion) {
        this.emotion = emotion;
    }

    public double getIntensite() {
        return intensite;
    }

    public void setIntensite(double intensite) {
        this.intensite = intensite;
    }

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
