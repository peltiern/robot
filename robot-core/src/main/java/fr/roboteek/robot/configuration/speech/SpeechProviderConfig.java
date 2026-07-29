package fr.roboteek.robot.configuration.speech;

import fr.roboteek.robot.services.recognizer.SpeechRecognizerProvider;
import fr.roboteek.robot.services.synthesizer.SpeechSynthesizerProvider;
import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

/**
 * Sélection du fournisseur de reconnaissance/synthèse vocale (cloud ou local).
 * Éditable sans rebuild : changer la valeur dans {@code robot.properties} puis
 * redémarrer le conteneur.
 */
@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface SpeechProviderConfig extends Config {

    @Key("speech.recognizer.provider")
    @DefaultValue("GOOGLE")
    SpeechRecognizerProvider recognizerProvider();

    @Key("speech.synthesizer.provider")
    @DefaultValue("GOOGLE")
    SpeechSynthesizerProvider synthesizerProvider();
}
