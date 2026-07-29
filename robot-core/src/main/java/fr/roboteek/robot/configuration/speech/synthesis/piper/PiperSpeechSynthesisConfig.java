package fr.roboteek.robot.configuration.speech.synthesis.piper;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface PiperSpeechSynthesisConfig extends Config {

    /**
     * Nom du fichier modèle de voix ONNX, déployé sous
     * {@code ${ROBOT_HOME}/synthese-vocale/piper/models/<nom>}.
     *
     * @return le nom du fichier modèle
     */
    @Key("speech.synthesis.piper.voice.model.name")
    @DefaultValue("fr_FR-siwis-medium.onnx")
    String voiceModelFileName();

    /**
     * Script SOX appliqué à la sortie de Piper (filtre/effet, cf. les scripts existants
     * pour la voix Google). Piper n'a pas besoin de retouche par défaut : script passthrough.
     *
     * @return le nom du script de filtre
     */
    @Key("speech.synthesis.piper.voice.filter")
    @DefaultValue("synthesis_piper_passthrough.sh")
    String voiceFilter();
}
