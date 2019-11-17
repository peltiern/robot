package fr.roboteek.robot.sandbox.controller.ps3.jamepad;

import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadComponentValue;
import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadEvent;

import java.util.List;
import java.util.Map;

public class Ps3ControllerEvent extends GamepadEvent<PS3Component> {

    public Ps3ControllerEvent(List<PS3Component> modifiedComponents, Map<PS3Component, GamepadComponentValue<PS3Component>> mapValues) {
        super(modifiedComponents, mapValues);
    }
}
