package fr.roboteek.robot.configuration.speech.synthesis.google;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type= HotReloadType.ASYNC)
@Sources({ "file:${ROBOT_HOME}/configuration/robot.properties"})
public interface GoogleSpeechSynthesisConfig extends Config {

    @Key("speech.synthesis.google.language.code")
    @DefaultValue("fr-FR")
    String languageCode();

    @Key("speech.synthesis.google.voice.name")
    @DefaultValue("fr-FR-Wavenet-D")
    String voiceName();

    @Key("speech.synthesis.google.voice.filter")
    @DefaultValue("synthesis_from_wavenet_d.sh")
    String voiceFilter();
}
