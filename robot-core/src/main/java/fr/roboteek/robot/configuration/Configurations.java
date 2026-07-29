package fr.roboteek.robot.configuration;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.configuration.speech.SpeechProviderConfig;
import fr.roboteek.robot.configuration.speech.recognizer.google.GoogleSpeechRecognizerConfig;
import fr.roboteek.robot.configuration.speech.recognizer.vosk.VoskSpeechRecognizerConfig;
import fr.roboteek.robot.configuration.speech.synthesis.google.GoogleSpeechSynthesisConfig;
import fr.roboteek.robot.configuration.speech.synthesis.piper.PiperSpeechSynthesisConfig;
import org.aeonbits.owner.ConfigCache;

public class Configurations {

    private Configurations() {
    }

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

    public static SpeechProviderConfig speechProviderConfig() {
        return ConfigCache.getOrCreate(SpeechProviderConfig.class);
    }

    public static VoskSpeechRecognizerConfig voskSpeechRecognizerConfig() {
        return ConfigCache.getOrCreate(VoskSpeechRecognizerConfig.class);
    }

    public static PiperSpeechSynthesisConfig piperSpeechSynthesisConfig() {
        return ConfigCache.getOrCreate(PiperSpeechSynthesisConfig.class);
    }
}
