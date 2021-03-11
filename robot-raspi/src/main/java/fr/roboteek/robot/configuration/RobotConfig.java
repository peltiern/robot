package fr.roboteek.robot.configuration;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface RobotConfig extends Config {

    @Key("language.code")
    @DefaultValue("fr-FR")
    String languageCode();

    /**
     * Name of the the webcam to select.
     *
     * @return the the webcam to select
     */
    @Key("device.webcam.name")
    String webcamName();

    /**
     * Name of the the microphone to select.
     *
     * @return the the microphone to select
     */
    @Key("device.microphone.name")
    String microphoneName();
}
