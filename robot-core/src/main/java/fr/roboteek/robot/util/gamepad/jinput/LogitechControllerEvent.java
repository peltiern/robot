package fr.roboteek.robot.util.gamepad.jinput;

import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import fr.roboteek.robot.util.gamepad.shared.GamepadEvent;

import java.util.List;
import java.util.Map;

public class LogitechControllerEvent extends GamepadEvent<LogitechComponent> {

    public LogitechControllerEvent(List<LogitechComponent> modifiedComponents, Map<LogitechComponent, GamepadComponentValue<LogitechComponent>> mapValues) {
        super(modifiedComponents, mapValues);
    }
}
