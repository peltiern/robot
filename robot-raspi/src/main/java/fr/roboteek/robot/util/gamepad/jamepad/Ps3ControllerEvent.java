package fr.roboteek.robot.util.gamepad.jamepad;

import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import fr.roboteek.robot.util.gamepad.shared.GamepadEvent;

import java.util.List;
import java.util.Map;

public class Ps3ControllerEvent extends GamepadEvent<PS3Component> {

    public Ps3ControllerEvent(List<PS3Component> modifiedComponents, Map<PS3Component, GamepadComponentValue<PS3Component>> mapValues) {
        super(modifiedComponents, mapValues);
    }
}
