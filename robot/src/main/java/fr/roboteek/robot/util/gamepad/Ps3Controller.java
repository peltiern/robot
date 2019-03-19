package fr.roboteek.robot.util.gamepad;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;

import java.util.Optional;

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
        Ps3ControllerEvent event = new Ps3ControllerEvent();
        event.setComponent(ps3Component);
        event.setOldNumericValue(oldValue);
        event.setNewNumericValue(newValue);
        event.setOldPressed(oldPressed);
        event.setNewPressed(newPressed);
        event.setButtonTrianglePressed(buttonTrianglePressed);
        event.setButtonSquarePressed(buttonSquarePressed);
        event.setButtonCrossPressed(buttonCrossPressed);
        event.setButtonRoundPressed(buttonRoundPressed);
        event.setButtonSelectPressed(buttonSelectPressed);
        event.setButtonStartPressed(buttonStartPressed);
        event.setButtonModePressed(buttonModePressed);
        event.setButtonLeft1Pressed(buttonLeft1Pressed);
        event.setButtonLeft2Pressed(buttonLeft2Pressed);
        event.setButtonLeftJoystick3Pressed(buttonLeftJoystick3Pressed);
        event.setButtonRight1Pressed(buttonRight1Pressed);
        event.setButtonRight2Pressed(buttonRight2Pressed);
        event.setButtonRightJoystick3Pressed(buttonRightJoystick3Pressed);
        event.setButtonAnalogLeft2Value(buttonAnalogLeft2Value);
        event.setButtonAnalogRight2Value(buttonAnalogRight2Value);
        event.setJoystickLeftAxisXValue(joystickLeftAxisXValue);
        event.setJoystickLeftAxisYValue(joystickLeftAxisYValue);
        event.setJoystickRightAxisXValue(joystickRightAxisXValue);
        event.setJoystickRightAxisYValue(joystickRightAxisYValue);
        return event;
    }

}
