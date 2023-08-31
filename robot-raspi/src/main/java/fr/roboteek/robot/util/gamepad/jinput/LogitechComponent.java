package fr.roboteek.robot.util.gamepad.jinput;

import fr.roboteek.robot.util.gamepad.shared.GamepadComponent;
import net.java.games.input.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum LogitechComponent implements GamepadComponent {

    BUTTON_X(Component.Identifier.Button.X.getName()),
    BUTTON_Y(Component.Identifier.Button.Y.getName()),
    BUTTON_A(Component.Identifier.Button.A.getName()),
    BUTTON_B(Component.Identifier.Button.B.getName()),
    BUTTON_BACK(Component.Identifier.Button.SELECT.getName()),
    BUTTON_START(Component.Identifier.Button.START.getName()),
    BUTTON_LOGITECH(Component.Identifier.Button.MODE.getName()),
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
    JOYSTICK_RIGHT_AXIS_Y(Component.Identifier.Axis.RY.getName()),

    BUTTON_CROSS_TOP(null),
    BUTTON_CROSS_TOP_RIGHT(null),
    BUTTON_CROSS_RIGHT(null),
    BUTTON_CROSS_BOTTOM_RIGHT(null),
    BUTTON_CROSS_BOTTOM(null),
    BUTTON_CROSS_BOTTOM_LEFT(null),
    BUTTON_CROSS_LEFT(null),
    BUTTON_CROSS_TOP_LEFT(null),
    BUTTON_CROSS_CENTER(null);

    private static final Map<String, LogitechComponent> mapPs3Components = new HashMap<>();

    static {
        Arrays.stream(values()).forEach(logitechComponent -> mapPs3Components.put(logitechComponent.inputValue, logitechComponent));
    }

    private static final LogitechComponent[] crossDirections = { BUTTON_CROSS_TOP_LEFT, BUTTON_CROSS_TOP, BUTTON_CROSS_TOP_RIGHT, BUTTON_CROSS_RIGHT, BUTTON_CROSS_BOTTOM_RIGHT, BUTTON_CROSS_BOTTOM, BUTTON_CROSS_BOTTOM_LEFT, BUTTON_CROSS_LEFT };


    /**
     * JInput Value.
     */
    private final String inputValue;

    LogitechComponent(String inputValue) {
        this.inputValue = inputValue;
    }

    public static LogitechComponent fromComponentAndValue(Component component, float value) throws IllegalArgumentException {
        if (StringUtils.equals(component.getName(), Component.Identifier.Axis.POV.getName())) {
            return processPov(value);
        } else {
            return mapPs3Components.get(component.getName());
        }
    }

    private static LogitechComponent processPov(float value) {
        float newValue = value * 8;
        if (newValue % 1 != 0 && newValue >= 0.0f && newValue <= 8.0f) {
            throw new IllegalArgumentException("Valeur invalide");
        }
        else if (newValue != 0.0f) {
            System.out.println(crossDirections[(int) newValue - 1]);
            return crossDirections[(int) newValue - 1];
        } else {
            return BUTTON_CROSS_CENTER;
        }
    }
}
