package fr.roboteek.robot.services.providers.google.speech.synthesizer.client.dto.request;

public class Voice {
    private String languageCode;
    private String name;

    public Voice(String languageCode, String name) {
        this.languageCode = languageCode;
        this.name = name;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
