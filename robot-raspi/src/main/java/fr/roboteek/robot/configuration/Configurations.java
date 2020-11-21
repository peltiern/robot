package fr.roboteek.robot.configuration;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.configuration.speech.recognizer.google.GoogleSpeechRecognizerConfig;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import org.aeonbits.owner.ConfigCache;

public class Configurations {

    private Configurations() {}

    public static RobotConfig robotConfig() {
        return ConfigCache.getOrCreate(RobotConfig.class);
    }

    public static PhidgetsConfig phidgetsConfig() {
        return ConfigCache.getOrCreate(PhidgetsConfig.class);
    }

    public static GoogleSpeechSynthesisConfig googleSpeechSynthesisConfig() {
        return ConfigCache.getOrCreate(GoogleSpeechSynthesisConfig.class);
    }

    public static GoogleSpeechRecognizerConfig googleSpeechRecognizerConfig() {
        return ConfigCache.getOrCreate(GoogleSpeechRecognizerConfig.class);
    }
}
