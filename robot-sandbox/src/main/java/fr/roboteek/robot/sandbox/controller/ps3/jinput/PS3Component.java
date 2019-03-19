package fr.roboteek.robot.sandbox.controller.ps3.jinput;

import net.java.games.input.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum PS3Component implements GamepadComponent {

    BUTTON_TRIANGLE(Component.Identifier.Button.X.getName()),
    BUTTON_SQUARE(Component.Identifier.Button.Y.getName()),
    BUTTON_CROSS(Component.Identifier.Button.A.getName()),
    BUTTON_ROUND(Component.Identifier.Button.B.getName()),
    BUTTON_SELECT(Component.Identifier.Button.SELECT.getName()),
    BUTTON_START(Component.Identifier.Button.START.getName()),
    BUTTON_MODE(Component.Identifier.Button.MODE.getName()),
    BUTTON_LEFT_1(Component.Identifier.Button.LEFT_THUMB.getName()),
    BUTTON_LEFT_2(Component.Identifier.Button.LEFT_THUMB2.getName()),
    BUTTON_LEFT_JOYSTICK_3(Component.Identifier.Button.LEFT_THUMB3.getName()),
    BUTTON_RIGHT_1(Component.Identifier.Button.RIGHT_THUMB.getName()),
    BUTTON_RIGHT_2(Component.Identifier.Button.RIGHT_THUMB2.getName()),
    BUTTON_RIGHT_JOYSTICK_3(Component.Identifier.Button.RIGHT_THUMB3.getName()),

    BUTTON_ANALOG_LEFT_2(Component.Identifier.Axis.Z.getName()),
    BUTTON_ANALOG_RIGHT_2(Component.Identifier.Axis.RZ.getName()),

    JOYSTICK_LEFT_AXIS_X(Component.Identifier.Axis.X.getName()),
    JOYSTICK_LEFT_AXIS_Y(Component.Identifier.Axis.Y.getName()),
    JOYSTICK_RIGHT_AXIS_X(Component.Identifier.Axis.RX.getName()),
    JOYSTICK_RIGHT_AXIS_Y(Component.Identifier.Axis.RY.getName());

    private static final Map<String, PS3Component> mapPs3Components = new HashMap<>();

    static {
        Arrays.stream(values()).forEach(ps3Component -> mapPs3Components.put(ps3Component.inputValue, ps3Component));
    }
    

    /** JInput Value. */
    private String inputValue;

    PS3Component(String inputValue) {
        this.inputValue = inputValue;
    }

    public static PS3Component fromValue(String value) throws IllegalArgumentException {
        return mapPs3Components.get(value);
    }
}
