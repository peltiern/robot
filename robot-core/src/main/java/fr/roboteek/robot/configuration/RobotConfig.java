package fr.roboteek.robot.configuration;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface RobotConfig extends Config {

    @Key("language.code")
    @DefaultValue("fr-FR")
    String languageCode();

    @Key("device.webcam.name")
    String webcamName();

    @Key("device.microphone.name")
    String microphoneName();

    @Key("pilote.differentiel.roue.diametre")
    @DefaultValue("98")
    int diametreRoue();

    @Key("pilote.differentiel.roue.distance")
    @DefaultValue("300")
    int distanceRoues();

    @Key("pilote.differentiel.roue.rapport_transmission")
    @DefaultValue("0.5")
    float rapportTransmission();

    @Key("pilote.differentiel.roue.encodeur.ticks_par_rotation")
    @DefaultValue("1993")
    int ticksParRotation();
}
