package fr.roboteek.robot.util.gamepad.jinput;

import fr.roboteek.robot.util.gamepad.shared.GamepadComponentValue;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;

import java.util.*;

public class LogitechController extends GamepadController<LogitechControllerEvent, LogitechListener> {

    private static final String TYPE = "Logitech Gamepad F710";

    private boolean buttonXPressed = false;
    private boolean buttonYPressed = false;
    private boolean buttonAPressed = false;
    private boolean buttonBPressed = false;
    private boolean buttonBackPressed = false;
    private boolean buttonStartPressed = false;
    private boolean buttonLogitechPressed = false;
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

    private boolean buttonCrossTopPressed = false;
    private boolean buttonCrossTopRightPressed = false;
    private boolean buttonCrossRightPressed = false;
    private boolean buttonCrossBottomRightPressed = false;
    private boolean buttonCrossBottomPressed = false;
    private boolean buttonCrossBottomLeftPressed = false;
    private boolean buttonCrossLeftPressed = false;
    private boolean buttonCrossTopLeftPressed = false;
    private boolean buttonCrossCenterPressed = false;


    public LogitechController(Controller controller) {
        super(controller, TYPE);
    }

    @Override
    public Optional<LogitechControllerEvent> mapEvent(Event event) {
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

//        System.out.println("COMPOSANT = " + component.getName() + "\tVALUE = " + value + "\tPRESSED = " + pressed);

        LogitechComponent logitechComponent = LogitechComponent.fromComponentAndValue(component, value);

        if (logitechComponent != null) {
            switch (logitechComponent) {
                case BUTTON_X:
                    oldPressed = buttonXPressed;
                    newPressed = pressed;
                    buttonXPressed = pressed;
                    break;
                case BUTTON_Y:
                    oldPressed = buttonYPressed;
                    newPressed = pressed;
                    buttonYPressed = pressed;
                    break;
                case BUTTON_A:
                    oldPressed = buttonAPressed;
                    newPressed = pressed;
                    buttonAPressed = pressed;
                    break;
                case BUTTON_B:
                    oldPressed = buttonBPressed;
                    newPressed = pressed;
                    buttonBPressed = pressed;
                    break;
                case BUTTON_BACK:
                    oldPressed = buttonBackPressed;
                    newPressed = pressed;
                    buttonBackPressed = pressed;
                    break;
                case BUTTON_START:
                    oldPressed = buttonStartPressed;
                    newPressed = pressed;
                    buttonStartPressed = pressed;
                    break;
                case BUTTON_LOGITECH:
                    oldPressed = buttonLogitechPressed;
                    newPressed = pressed;
                    buttonLogitechPressed = pressed;
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
                case BUTTON_CROSS_TOP:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = true;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_TOP_RIGHT:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = true;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_RIGHT:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = true;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_BOTTOM_RIGHT:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = true;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_BOTTOM:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = true;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_BOTTOM_LEFT:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = true;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_LEFT:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = true;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_TOP_LEFT:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = true;
                    buttonCrossCenterPressed = false;
                    break;
                case BUTTON_CROSS_CENTER:
                    oldPressed = false;
                    newPressed = true;
                    buttonCrossTopPressed = false;
                    buttonCrossTopRightPressed = false;
                    buttonCrossRightPressed = false;
                    buttonCrossBottomRightPressed = false;
                    buttonCrossBottomPressed = false;
                    buttonCrossBottomLeftPressed = false;
                    buttonCrossLeftPressed = false;
                    buttonCrossTopLeftPressed = false;
                    buttonCrossCenterPressed = true;
                    break;
            }
            return Optional.of(createLogitechControllerEvent(logitechComponent, oldValue, newValue, oldPressed, newPressed));
        }
        return Optional.empty();
    }

    @Override
    public Class forListener() {
        return LogitechListener.class;
    }

    private LogitechControllerEvent createLogitechControllerEvent(LogitechComponent logitechComponent, Float oldValue, Float newValue, Boolean oldPressed, Boolean newPressed) {
        List<LogitechComponent> modifiedComponents = new ArrayList<>();
        modifiedComponents.add(logitechComponent);

        Map<LogitechComponent, GamepadComponentValue<LogitechComponent>> mapValues = new HashMap<>();
        //BUTTON_X
        mapValues.put(LogitechComponent.BUTTON_X, new GamepadComponentValue<>(LogitechComponent.BUTTON_X, buttonXPressed, buttonXPressed));
        //BUTTON_Y
        mapValues.put(LogitechComponent.BUTTON_Y, new GamepadComponentValue<>(LogitechComponent.BUTTON_Y, buttonYPressed, buttonYPressed));
        //BUTTON_A
        mapValues.put(LogitechComponent.BUTTON_A, new GamepadComponentValue<>(LogitechComponent.BUTTON_A, buttonAPressed, buttonAPressed));
        //BUTTON_B
        mapValues.put(LogitechComponent.BUTTON_B, new GamepadComponentValue<>(LogitechComponent.BUTTON_B, buttonBPressed, buttonBPressed));
        //BUTTON_BACK
        mapValues.put(LogitechComponent.BUTTON_BACK, new GamepadComponentValue<>(LogitechComponent.BUTTON_BACK, buttonBackPressed, buttonBackPressed));
        //BUTTON_START
        mapValues.put(LogitechComponent.BUTTON_START, new GamepadComponentValue<>(LogitechComponent.BUTTON_START, buttonStartPressed, buttonStartPressed));
        //BUTTON_LOGITECH
        mapValues.put(LogitechComponent.BUTTON_LOGITECH, new GamepadComponentValue<>(LogitechComponent.BUTTON_LOGITECH, buttonLogitechPressed, buttonLogitechPressed));
        //BUTTON_LEFT_1
        mapValues.put(LogitechComponent.BUTTON_LEFT_1, new GamepadComponentValue<>(LogitechComponent.BUTTON_LEFT_1, buttonLeft1Pressed, buttonLeft1Pressed));
        //BUTTON_LEFT_2
        mapValues.put(LogitechComponent.BUTTON_LEFT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_LEFT_2, buttonLeft2Pressed, buttonLeft2Pressed));
        //BUTTON_LEFT_JOYSTICK_3
        mapValues.put(LogitechComponent.BUTTON_LEFT_JOYSTICK_3, new GamepadComponentValue<>(LogitechComponent.BUTTON_LEFT_JOYSTICK_3, buttonLeftJoystick3Pressed, buttonLeftJoystick3Pressed));
        //BUTTON_RIGHT_1
        mapValues.put(LogitechComponent.BUTTON_RIGHT_1, new GamepadComponentValue<>(LogitechComponent.BUTTON_RIGHT_1, buttonRight1Pressed, buttonRight1Pressed));
        //BUTTON_RIGHT_2
        mapValues.put(LogitechComponent.BUTTON_RIGHT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_RIGHT_2, buttonRight2Pressed, buttonRight2Pressed));
        //BUTTON_RIGHT_JOYSTICK_3
        mapValues.put(LogitechComponent.BUTTON_RIGHT_JOYSTICK_3, new GamepadComponentValue<>(LogitechComponent.BUTTON_RIGHT_JOYSTICK_3, buttonRightJoystick3Pressed, buttonRightJoystick3Pressed));
        //BUTTON_ANALOG_LEFT_2
        mapValues.put(LogitechComponent.BUTTON_ANALOG_LEFT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_ANALOG_LEFT_2, buttonAnalogLeft2Value, buttonAnalogLeft2Value));
        //BUTTON_ANALOG_RIGHT_2
        mapValues.put(LogitechComponent.BUTTON_ANALOG_RIGHT_2, new GamepadComponentValue<>(LogitechComponent.BUTTON_ANALOG_RIGHT_2, buttonAnalogRight2Value, buttonAnalogRight2Value));
        //JOYSTICK_LEFT_AXIS_X
        mapValues.put(LogitechComponent.JOYSTICK_LEFT_AXIS_X, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_LEFT_AXIS_X, joystickLeftAxisXValue, joystickLeftAxisXValue));
        //JOYSTICK_LEFT_AXIS_Y
        mapValues.put(LogitechComponent.JOYSTICK_LEFT_AXIS_Y, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_LEFT_AXIS_Y, joystickLeftAxisYValue, joystickLeftAxisYValue));
        //JOYSTICK_RIGHT_AXIS_X
        mapValues.put(LogitechComponent.JOYSTICK_RIGHT_AXIS_X, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_RIGHT_AXIS_X, joystickRightAxisXValue, joystickRightAxisXValue));
        //JOYSTICK_RIGHT_AXIS_Y
        mapValues.put(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y, new GamepadComponentValue<>(LogitechComponent.JOYSTICK_RIGHT_AXIS_Y, joystickRightAxisYValue, joystickRightAxisYValue));
        //BUTTON_CROSS_TOP
        mapValues.put(LogitechComponent.BUTTON_CROSS_TOP, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_TOP, buttonCrossTopPressed, buttonCrossTopPressed));
        //BUTTON_CROSS_TOP_RIGHT
        mapValues.put(LogitechComponent.BUTTON_CROSS_TOP_RIGHT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_TOP_RIGHT, buttonCrossTopRightPressed, buttonCrossTopRightPressed));
        //BUTTON_CROSS_RIGHT
        mapValues.put(LogitechComponent.BUTTON_CROSS_RIGHT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_RIGHT, buttonCrossRightPressed, buttonCrossRightPressed));
        //BUTTON_CROSS_BOTTOM_RIGHT
        mapValues.put(LogitechComponent.BUTTON_CROSS_BOTTOM_RIGHT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_BOTTOM_RIGHT, buttonCrossBottomRightPressed, buttonCrossBottomRightPressed));
        //BUTTON_CROSS_BOTTOM
        mapValues.put(LogitechComponent.BUTTON_CROSS_BOTTOM, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_BOTTOM, buttonCrossBottomPressed, buttonCrossBottomPressed));
        //BUTTON_CROSS_BOTTOM_LEFT
        mapValues.put(LogitechComponent.BUTTON_CROSS_BOTTOM_LEFT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_BOTTOM_LEFT, buttonCrossBottomLeftPressed, buttonCrossBottomLeftPressed));
        //BUTTON_CROSS_LEFT
        mapValues.put(LogitechComponent.BUTTON_CROSS_LEFT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_LEFT, buttonCrossLeftPressed, buttonCrossLeftPressed));
        //BUTTON_CROSS_TOP_LEFT
        mapValues.put(LogitechComponent.BUTTON_CROSS_TOP_LEFT, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_TOP_LEFT, buttonCrossTopLeftPressed, buttonCrossTopLeftPressed));
        //BUTTON_CROSS_CENTER
        mapValues.put(LogitechComponent.BUTTON_CROSS_CENTER, new GamepadComponentValue<>(LogitechComponent.BUTTON_CROSS_CENTER, buttonCrossCenterPressed, buttonCrossCenterPressed));

        if (logitechComponent == LogitechComponent.BUTTON_ANALOG_LEFT_2 || logitechComponent == LogitechComponent.BUTTON_ANALOG_RIGHT_2
                || logitechComponent == LogitechComponent.JOYSTICK_LEFT_AXIS_X || logitechComponent == LogitechComponent.JOYSTICK_LEFT_AXIS_Y ||
                logitechComponent == LogitechComponent.JOYSTICK_RIGHT_AXIS_X || logitechComponent == LogitechComponent.JOYSTICK_RIGHT_AXIS_Y) {
            mapValues.put(logitechComponent, new GamepadComponentValue<>(logitechComponent, oldValue, newValue));
        } else {
            mapValues.put(logitechComponent, new GamepadComponentValue<>(logitechComponent, oldPressed, newPressed));
        }

        return new LogitechControllerEvent(modifiedComponents, mapValues);
    }

}
