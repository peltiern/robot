package fr.roboteek.robot.configuration.speech.recognizer.vosk;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface VoskSpeechRecognizerConfig extends Config {

    /**
     * Nom du dossier modèle Vosk, déployé sous
     * {@code ${ROBOT_HOME}/reconnaissanceVocale/vosk/<nom>}.
     *
     * @return le nom du dossier modèle
     */
    @Key("speech.recognizer.vosk.model.name")
    @DefaultValue("vosk-model-small-fr-0.22")
    String modelName();
}
