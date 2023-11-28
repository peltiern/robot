package fr.roboteek.robot.configuration.phidgets;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.*;

@HotReload(type = HotReloadType.ASYNC)
@Sources({"file:${ROBOT_HOME}/configuration/robot.properties"})
public interface PhidgetsConfig extends Config {

    @Key("phidgets.hub.serial.number")
    @DefaultValue("561050")
    int hubSerialNumber();

    @Key("phidgets.differential.driving.motor.left.port")
    @DefaultValue("1")
    int differentialDrivingLeftMotorPort();

    @Key("phidgets.differential.driving.motor.right.port")
    @DefaultValue("2")
    int differentialDrivingRightMotorPort();

    @Key("phidgets.differential.driving.motor.max.speed")
    @DefaultValue("0.7")
    double differentialDrivingMotorMaxSpeed();

    @Key("phidgets.differential.driving.motor.acceleration")
    @DefaultValue("1")
    double differentialDrivingMotorAcceleration();

    @Key("phidgets.neck.motor.left_right.index")
    @DefaultValue("0")
    int neckLeftRightMotorIndex();

    @Key("phidgets.neck.motor.left_right.position.init")
    @DefaultValue("85")
    double neckLeftRightMotorInitialPosition();

    @Key("phidgets.neck.motor.left_right.position.min")
    @DefaultValue("20")
    double neckLeftRightMotorMinPosition();

    @Key("phidgets.neck.motor.left_right.position.max")
    @DefaultValue("150")
    double neckLeftRightMotorMaxPosition();

    @Key("phidgets.neck.motor.left_right.speed")
    @DefaultValue("100")
    double neckLeftRightMotorSpeed();

    @Key("phidgets.neck.motor.left_right.acceleration")
    @DefaultValue("200")
    double neckLeftRightMotorAcceleration();

    @Key("phidgets.neck.motor.up_down.index")
    @DefaultValue("0")
    int neckUpDownMotorIndex();

    @Key("phidgets.neck.motor.up_down.position.init")
    @DefaultValue("85")
    double neckUpDownMotorInitialPosition();

    @Key("phidgets.neck.motor.up_down.position.min")
    @DefaultValue("35")
    double neckUpDownMotorMinPosition();

    @Key("phidgets.neck.motor.up_down.position.max")
    @DefaultValue("110")
    double neckUpDownMotorMaxPosition();

    @Key("phidgets.neck.motor.up_down.speed")
    @DefaultValue("100")
    double neckUpDownMotorSpeed();

    @Key("phidgets.neck.motor.up_down.acceleration")
    @DefaultValue("200")
    double neckUpDownMotorAcceleration();

    @Key("phidgets.eyes.motor.left.index")
    @DefaultValue("2")
    int eyeLeftMotorIndex();

    @Key("phidgets.eyes.motor.left.position.zero")
    @DefaultValue("92")
    double eyeLeftMotorPositionZero();

    @Key("phidgets.eyes.motor.left.speed")
    @DefaultValue("40")
    double eyeLeftMotorSpeed();

    @Key("phidgets.eyes.motor.left.acceleration")
    @DefaultValue("60")
    double eyeLeftMotorAcceleration();

    @Key("phidgets.eyes.motor.right.index")
    @DefaultValue("3")
    int eyeRightMotorIndex();

    @Key("phidgets.eyes.motor.right.position.zero")
    @DefaultValue("86")
    double eyeRightMotorPositionZero();

    @Key("phidgets.eyes.motor.right.speed")
    @DefaultValue("40")
    double eyeRightMotorSpeed();

    @Key("phidgets.eyes.motor.right.acceleration")
    @DefaultValue("60")
    double eyeRightMotorAcceleration();

    @Key("phidgets.eyes.motor.relative.position.min")
    @DefaultValue("-24")
    double eyeMotorRelativePositionMin();

    @Key("phidgets.eyes.motor.relative.position.max")
    @DefaultValue("20")
    double eyeMotorRelativePositionMax();
}
