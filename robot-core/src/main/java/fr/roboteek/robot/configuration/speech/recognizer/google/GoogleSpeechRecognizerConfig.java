package fr.roboteek.robot.configuration.speech.recognizer.google;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface GoogleSpeechRecognizerConfig extends Config {

    @Key("speech.recognizer.google.language.code")
    @DefaultValue("fr-FR")
    String languageCode();
}
