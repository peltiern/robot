package fr.roboteek.robot.sandbox.controller.ps3.jamepad;

import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadComponent;
import net.java.games.input.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum PS3Component implements GamepadComponent {

    BUTTON_TRIANGLE,
    BUTTON_SQUARE,
    BUTTON_CROSS,
    BUTTON_ROUND,
    BUTTON_SELECT,
    BUTTON_START,
    BUTTON_MODE,
    BUTTON_LEFT_1,
    BUTTON_LEFT_2,
    BUTTON_LEFT_JOYSTICK_3,
    BUTTON_RIGHT_1,
    BUTTON_RIGHT_2,
    BUTTON_RIGHT_JOYSTICK_3,
    BUTTON_CROSS_LEFT,
    BUTTON_CROSS_RIGHT,
    BUTTON_CROSS_TOP,
    BUTTON_CROSS_BOTTOM,

    BUTTON_ANALOG_LEFT_2,
    BUTTON_ANALOG_RIGHT_2,

    JOYSTICK_LEFT_AXIS_X,
    JOYSTICK_LEFT_AXIS_Y,
    JOYSTICK_LEFT_AMPLITUDE,
    JOYSTICK_LEFT_ANGLE,
    JOYSTICK_RIGHT_AXIS_X,
    JOYSTICK_RIGHT_AXIS_Y,
    JOYSTICK_RIGHT_AMPLITUDE,
    JOYSTICK_RIGHT_ANGLE
}
