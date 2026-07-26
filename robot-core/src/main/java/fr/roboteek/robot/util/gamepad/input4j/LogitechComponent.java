package fr.roboteek.robot.util.gamepad.input4j;

import fr.roboteek.robot.util.gamepad.shared.GamepadComponent;

/**
 * Contrôles logiques d'une manette Logitech F710, indépendants de la librairie
 * d'entrée. Le mapping vers les composants physiques (XInput via input4j) est fait
 * dans {@link LogitechControllerInput4j}.
 */
public enum LogitechComponent implements GamepadComponent {

    BUTTON_X,
    BUTTON_Y,
    BUTTON_A,
    BUTTON_B,
    BUTTON_BACK,
    BUTTON_START,
    BUTTON_LOGITECH,
    BUTTON_LEFT_1,
    BUTTON_LEFT_2,
    BUTTON_LEFT_JOYSTICK_3,
    BUTTON_RIGHT_1,
    BUTTON_RIGHT_2,
    BUTTON_RIGHT_JOYSTICK_3,

    BUTTON_ANALOG_LEFT_2,
    BUTTON_ANALOG_RIGHT_2,

    JOYSTICK_LEFT_AXIS_X,
    JOYSTICK_LEFT_AXIS_Y,
    JOYSTICK_RIGHT_AXIS_X,
    JOYSTICK_RIGHT_AXIS_Y,

    BUTTON_CROSS_TOP,
    BUTTON_CROSS_TOP_RIGHT,
    BUTTON_CROSS_RIGHT,
    BUTTON_CROSS_BOTTOM_RIGHT,
    BUTTON_CROSS_BOTTOM,
    BUTTON_CROSS_BOTTOM_LEFT,
    BUTTON_CROSS_LEFT,
    BUTTON_CROSS_TOP_LEFT,
    BUTTON_CROSS_CENTER
}
