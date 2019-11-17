package fr.roboteek.robot.sandbox.controller.ps3.jinput;

import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadComponentValue;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;

import java.util.*;

public class Ps3Controller extends GamepadController<Ps3ControllerEvent, PS3Listener> {

    private static final String TYPE = "Sony PLAYSTATION(R)3 Controller";

    private boolean buttonTrianglePressed = false;
    private boolean buttonSquarePressed = false;
    private boolean buttonCrossPressed = false;
    private boolean buttonRoundPressed = false;
    private boolean buttonSelectPressed = false;
    private boolean buttonStartPressed = false;
    private boolean buttonModePressed = false;
    private boolean buttonLeft1Pressed = false;
    private boolean buttonLeft2Pressed = false;
    private boolean buttonLeftJoystick3Pressed = false;
    private boolean buttonRight1Pressed = false;
    private boolean buttonRight2Pressed = false;
    private boolean buttonRightJoystick3Pressed = false;

    private float buttonAnalogLeft2Value = 0;
    private float buttonAnalogRight2Value = 0;
    private float joystickLeftAxisXValue = 0;
    private float joystickLeftAxisYValue = 0;
    private float joystickRightAxisXValue = 0;
    private float joystickRightAxisYValue = 0;


    public Ps3Controller(Controller controller) {
        super(controller, TYPE);
    }

    @Override
    public Optional<Ps3ControllerEvent> mapEvent(Event event) {
        Component component = event.getComponent();

        Float oldValue = null;
        Float newValue = null;
        Boolean oldPressed = null;
        Boolean newPressed = null;

        float value = event.getValue();
        boolean pressed = false;
        if (!component.isAnalog()) {
            pressed = value == 1.0f;
        }

        PS3Component ps3Component = PS3Component.fromValue(component.getName());

        if (ps3Component != null) {
            switch (ps3Component) {
                case BUTTON_TRIANGLE:
                    oldPressed = buttonTrianglePressed;
                    newPressed = pressed;
                    buttonTrianglePressed = pressed;
                    break;
                case BUTTON_SQUARE:
                    oldPressed = buttonSquarePressed;
                    newPressed = pressed;
                    buttonSquarePressed = pressed;
                    break;
                case BUTTON_CROSS:
                    oldPressed = buttonCrossPressed;
                    newPressed = pressed;
                    buttonCrossPressed = pressed;
                    break;
                case BUTTON_ROUND:
                    oldPressed = buttonRoundPressed;
                    newPressed = pressed;
                    buttonRoundPressed = pressed;
                    break;
                case BUTTON_SELECT:
                    oldPressed = buttonSelectPressed;
                    newPressed = pressed;
                    buttonSelectPressed = pressed;
                    break;
                case BUTTON_START:
                    oldPressed = buttonStartPressed;
                    newPressed = pressed;
                    buttonStartPressed = pressed;
                    break;
                case BUTTON_MODE:
                    oldPressed = buttonModePressed;
                    newPressed = pressed;
                    buttonModePressed = pressed;
                    break;
                case BUTTON_LEFT_1:
                    oldPressed = buttonLeft1Pressed;
                    newPressed = pressed;
                    buttonLeft1Pressed = pressed;
                    break;
                case BUTTON_LEFT_2:
                    oldPressed = buttonLeft2Pressed;
                    newPressed = pressed;
                    buttonLeft2Pressed = pressed;
                    break;
                case BUTTON_LEFT_JOYSTICK_3:
                    oldPressed = buttonLeftJoystick3Pressed;
                    newPressed = pressed;
                    buttonLeftJoystick3Pressed = pressed;
                    break;
                case BUTTON_RIGHT_1:
                    oldPressed = buttonRight1Pressed;
                    newPressed = pressed;
                    buttonRight1Pressed = pressed;
                    break;
                case BUTTON_RIGHT_2:
                    oldPressed = buttonRight2Pressed;
                    newPressed = pressed;
                    buttonRight2Pressed = pressed;
                    break;
                case BUTTON_RIGHT_JOYSTICK_3:
                    oldPressed = buttonRightJoystick3Pressed;
                    newPressed = pressed;
                    buttonRightJoystick3Pressed = pressed;
                    break;
                case BUTTON_ANALOG_LEFT_2:
                    oldValue = buttonAnalogLeft2Value;
                    newValue = value;
                    buttonAnalogLeft2Value = value;
                    break;
                case BUTTON_ANALOG_RIGHT_2:
                    oldValue = buttonAnalogRight2Value;
                    newValue = value;
                    buttonAnalogRight2Value = value;
                    break;
                case JOYSTICK_LEFT_AXIS_X:
                    oldValue = joystickLeftAxisXValue;
                    newValue = value;
                    joystickLeftAxisXValue = value;
                    break;
                case JOYSTICK_LEFT_AXIS_Y:
                    oldValue = joystickLeftAxisYValue;
                    newValue = value;
                    joystickLeftAxisYValue = value;
                    break;
                case JOYSTICK_RIGHT_AXIS_X:
                    oldValue = joystickRightAxisXValue;
                    newValue = value;
                    joystickRightAxisXValue = value;
                    break;
                case JOYSTICK_RIGHT_AXIS_Y:
                    oldValue = joystickRightAxisYValue;
                    newValue = value;
                    joystickRightAxisYValue = value;
                    break;
            }
            return Optional.of(createPs3ControllerEvent(ps3Component, oldValue, newValue, oldPressed, newPressed));
        }
        return Optional.empty();
    }

    @Override
    public Class forListener() {
        return PS3Listener.class;
    }

    private Ps3ControllerEvent createPs3ControllerEvent(PS3Component ps3Component, Float oldValue, Float newValue, Boolean oldPressed, Boolean newPressed) {
        List<PS3Component> modifiedComponents = new ArrayList<>();
        modifiedComponents.add(ps3Component);

        Map<PS3Component, GamepadComponentValue<PS3Component>> mapValues = new HashMap<>();
        //BUTTON_TRIANGLE
        mapValues.put(PS3Component.BUTTON_TRIANGLE, new GamepadComponentValue<>(PS3Component.BUTTON_TRIANGLE, buttonTrianglePressed, buttonTrianglePressed));
        //BUTTON_SQUARE
        mapValues.put(PS3Component.BUTTON_SQUARE, new GamepadComponentValue<>(PS3Component.BUTTON_SQUARE, buttonSquarePressed, buttonSquarePressed));
        //BUTTON_CROSS
        mapValues.put(PS3Component.BUTTON_CROSS, new GamepadComponentValue<>(PS3Component.BUTTON_CROSS, buttonCrossPressed, buttonCrossPressed));
        //BUTTON_ROUND
        mapValues.put(PS3Component.BUTTON_ROUND, new GamepadComponentValue<>(PS3Component.BUTTON_ROUND, buttonRoundPressed, buttonRoundPressed));
        //BUTTON_SELECT
        mapValues.put(PS3Component.BUTTON_SELECT, new GamepadComponentValue<>(PS3Component.BUTTON_SELECT, buttonSelectPressed, buttonSelectPressed));
        //BUTTON_START
        mapValues.put(PS3Component.BUTTON_START, new GamepadComponentValue<>(PS3Component.BUTTON_START, buttonStartPressed, buttonStartPressed));
        //BUTTON_MODE
        mapValues.put(PS3Component.BUTTON_MODE, new GamepadComponentValue<>(PS3Component.BUTTON_MODE, buttonModePressed, buttonModePressed));
        //BUTTON_LEFT_1
        mapValues.put(PS3Component.BUTTON_LEFT_1, new GamepadComponentValue<>(PS3Component.BUTTON_LEFT_1, buttonLeft1Pressed, buttonLeft1Pressed));
        //BUTTON_LEFT_2
        mapValues.put(PS3Component.BUTTON_LEFT_2, new GamepadComponentValue<>(PS3Component.BUTTON_LEFT_2, buttonLeft2Pressed, buttonLeft2Pressed));
        //BUTTON_LEFT_JOYSTICK_3
        mapValues.put(PS3Component.BUTTON_LEFT_JOYSTICK_3, new GamepadComponentValue<>(PS3Component.BUTTON_LEFT_JOYSTICK_3, buttonLeftJoystick3Pressed, buttonLeftJoystick3Pressed));
        //BUTTON_RIGHT_1
        mapValues.put(PS3Component.BUTTON_RIGHT_1, new GamepadComponentValue<>(PS3Component.BUTTON_RIGHT_1, buttonRight1Pressed, buttonRight1Pressed));
        //BUTTON_RIGHT_2
        mapValues.put(PS3Component.BUTTON_RIGHT_2, new GamepadComponentValue<>(PS3Component.BUTTON_RIGHT_2, buttonRight2Pressed, buttonRight2Pressed));
        //BUTTON_RIGHT_JOYSTICK_3
        mapValues.put(PS3Component.BUTTON_RIGHT_JOYSTICK_3, new GamepadComponentValue<>(PS3Component.BUTTON_RIGHT_JOYSTICK_3, buttonRightJoystick3Pressed, buttonRightJoystick3Pressed));
        //BUTTON_ANALOG_LEFT_2
        mapValues.put(PS3Component.BUTTON_ANALOG_LEFT_2, new GamepadComponentValue<>(PS3Component.BUTTON_ANALOG_LEFT_2, buttonAnalogLeft2Value, buttonAnalogLeft2Value));
        //BUTTON_ANALOG_RIGHT_2
        mapValues.put(PS3Component.BUTTON_ANALOG_RIGHT_2, new GamepadComponentValue<>(PS3Component.BUTTON_ANALOG_RIGHT_2, buttonAnalogRight2Value, buttonAnalogRight2Value));
        //JOYSTICK_LEFT_AXIS_X
        mapValues.put(PS3Component.JOYSTICK_LEFT_AXIS_X, new GamepadComponentValue<>(PS3Component.JOYSTICK_LEFT_AXIS_X, joystickLeftAxisXValue, joystickLeftAxisXValue));
        //JOYSTICK_LEFT_AXIS_Y
        mapValues.put(PS3Component.JOYSTICK_LEFT_AXIS_Y, new GamepadComponentValue<>(PS3Component.JOYSTICK_LEFT_AXIS_Y, joystickLeftAxisYValue, joystickLeftAxisYValue));
        //JOYSTICK_RIGHT_AXIS_X
        mapValues.put(PS3Component.JOYSTICK_RIGHT_AXIS_X, new GamepadComponentValue<>(PS3Component.JOYSTICK_RIGHT_AXIS_X, joystickRightAxisXValue, joystickRightAxisXValue));
        //JOYSTICK_RIGHT_AXIS_Y
        mapValues.put(PS3Component.JOYSTICK_RIGHT_AXIS_Y, new GamepadComponentValue<>(PS3Component.JOYSTICK_RIGHT_AXIS_Y, joystickRightAxisYValue, joystickRightAxisYValue));

        if (ps3Component == PS3Component.BUTTON_ANALOG_LEFT_2 || ps3Component == PS3Component.BUTTON_ANALOG_RIGHT_2
                || ps3Component == PS3Component.JOYSTICK_LEFT_AXIS_X || ps3Component == PS3Component.JOYSTICK_LEFT_AXIS_Y ||
                ps3Component == PS3Component.JOYSTICK_RIGHT_AXIS_X || ps3Component == PS3Component.JOYSTICK_RIGHT_AXIS_Y) {
            mapValues.put(ps3Component, new GamepadComponentValue<>(ps3Component, oldValue, newValue));
        } else {
            mapValues.put(ps3Component, new GamepadComponentValue<>(ps3Component, oldPressed, newPressed));
        }

        return new Ps3ControllerEvent(modifiedComponents, mapValues);
    }

}
