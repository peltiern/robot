package fr.roboteek.robot.sandbox.controller.ps3.jamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import fr.roboteek.robot.sandbox.controller.ps3.shared.AbstractGamepadManager;
import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadComponentValue;

import java.util.*;

public class JamepadManager extends AbstractGamepadManager<PS3Listener> {

    private ControllerManager controller;
    private volatile boolean connected = true;

    @Override
    public void start() {
        controller = new ControllerManager(1);
        controller.initSDLGamepad();
        Thread threadManager = new Thread(this::runManager);
        threadManager.start();
    }

    private void runManager() {
        int i = 0;

        ControllerState previousState = controller.getState(0);

        while (true) {
            i++;
            ControllerState currentState = controller.getState(0);

            if (!currentState.isConnected) {
                System.out.println("Deconnexion");
                break;
            }

            List<PS3Component> modifiedComponents = getModifiedComponents(previousState, currentState);
            if (modifiedComponents != null && modifiedComponents.size() > 0) {
                Ps3ControllerEvent e = new Ps3ControllerEvent(modifiedComponents, mapControllerState(previousState, currentState));
                previousState = currentState;
                listeners.forEach(l -> l.onEvent(e));
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        controller.quitSDLGamepad();
    }

    private List<PS3Component> getModifiedComponents(ControllerState previousState, ControllerState currentState) {
        if (previousState == null || currentState == null) {
            return null;
        }

        List<PS3Component> modifiedComponents = new ArrayList<>();

        //BUTTON_TRIANGLE
        if (previousState.y != currentState.y) {
            modifiedComponents.add(PS3Component.BUTTON_TRIANGLE);
        }
        //BUTTON_SQUARE
        if (previousState.x != currentState.x) {
            modifiedComponents.add(PS3Component.BUTTON_SQUARE);
        }
        //BUTTON_CROSS
        if (previousState.a != currentState.a) {
            modifiedComponents.add(PS3Component.BUTTON_CROSS);
        }
        //BUTTON_ROUND
        if (previousState.b != currentState.b) {
            modifiedComponents.add(PS3Component.BUTTON_ROUND);
        }
        //BUTTON_SELECT
        if (previousState.back != currentState.back) {
            modifiedComponents.add(PS3Component.BUTTON_SELECT);
        }
        //BUTTON_START
        if (previousState.start != currentState.start) {
            modifiedComponents.add(PS3Component.BUTTON_START);
        }
        //BUTTON_MODE
        if (previousState.guide != currentState.guide) {
            modifiedComponents.add(PS3Component.BUTTON_MODE);
        }
        //BUTTON_LEFT_1
        if (previousState.lb != currentState.lb) {
            modifiedComponents.add(PS3Component.BUTTON_LEFT_1);
        }
        //BUTTON_LEFT_2
        if (previousState.leftTrigger != currentState.leftTrigger) {
            modifiedComponents.add(PS3Component.BUTTON_LEFT_2);
        }
        //BUTTON_LEFT_JOYSTICK_3
        if (previousState.leftStickClick != currentState.leftStickClick) {
            modifiedComponents.add(PS3Component.BUTTON_LEFT_JOYSTICK_3);
        }
        //BUTTON_RIGHT_1
        if (previousState.rb != currentState.rb) {
            modifiedComponents.add(PS3Component.BUTTON_RIGHT_1);
        }
        //BUTTON_RIGHT_2
        if (previousState.rightTrigger != currentState.rightTrigger) {
            modifiedComponents.add(PS3Component.BUTTON_RIGHT_2);
        }
        //BUTTON_RIGHT_JOYSTICK_3
        if (previousState.rightStickClick != currentState.rightStickClick) {
            modifiedComponents.add(PS3Component.BUTTON_RIGHT_JOYSTICK_3);
        }
        //BUTTON_CROSS_LEFT
        if (previousState.dpadLeft != currentState.dpadLeft) {
            modifiedComponents.add(PS3Component.BUTTON_CROSS_LEFT);
        }
        //BUTTON_CROSS_RIGHT
        if (previousState.dpadRight != currentState.dpadRight) {
            modifiedComponents.add(PS3Component.BUTTON_CROSS_RIGHT);
        }
        //BUTTON_CROSS_TOP
        if (previousState.dpadUp != currentState.dpadUp) {
            modifiedComponents.add(PS3Component.BUTTON_CROSS_TOP);
        }
        //BUTTON_CROSS_BOTTOM
        if (previousState.dpadDown != currentState.dpadDown) {
            modifiedComponents.add(PS3Component.BUTTON_CROSS_BOTTOM);
        }
        //BUTTON_ANALOG_LEFT_2
        if (previousState.leftTrigger != currentState.leftTrigger) {
            modifiedComponents.add(PS3Component.BUTTON_ANALOG_LEFT_2);
        }
        //BUTTON_ANALOG_RIGHT_2
        if (previousState.rightTrigger != currentState.rightTrigger) {
            modifiedComponents.add(PS3Component.BUTTON_ANALOG_RIGHT_2);
        }
        //JOYSTICK_LEFT_AXIS_X
        if (previousState.leftStickX != currentState.leftStickX) {
            modifiedComponents.add(PS3Component.JOYSTICK_LEFT_AXIS_X);
        }
        //JOYSTICK_LEFT_AXIS_Y
        if (previousState.leftStickY != currentState.leftStickY) {
            modifiedComponents.add(PS3Component.JOYSTICK_LEFT_AXIS_Y);
        }
        // JOYSTICK_LEFT_AMPLITUDE
        if (previousState.leftStickMagnitude != currentState.leftStickMagnitude) {
            modifiedComponents.add(PS3Component.JOYSTICK_LEFT_AMPLITUDE);
        }
        // JOYSTICK_LEFT_ANGLE
        if (previousState.leftStickAngle != currentState.leftStickAngle) {
            modifiedComponents.add(PS3Component.JOYSTICK_LEFT_ANGLE);
        }
        //JOYSTICK_RIGHT_AXIS_X
        if (previousState.rightStickX != currentState.rightStickX) {
            modifiedComponents.add(PS3Component.BUTTON_ANALOG_RIGHT_2);
        }
        //JOYSTICK_RIGHT_AXIS_Y
        if (previousState.rightStickY != currentState.rightStickY) {
            modifiedComponents.add(PS3Component.BUTTON_ANALOG_RIGHT_2);
        }
        // JOYSTICK_RIGHT_AMPLITUDE
        if (previousState.rightStickMagnitude != currentState.rightStickMagnitude) {
            modifiedComponents.add(PS3Component.JOYSTICK_RIGHT_AMPLITUDE);
        }
        // JOYSTICK_RIGHT_ANGLE
        if (previousState.rightStickAngle != currentState.rightStickAngle) {
            modifiedComponents.add(PS3Component.JOYSTICK_RIGHT_ANGLE);
        }
        return modifiedComponents;
    }

    private Map<PS3Component, GamepadComponentValue<PS3Component>> mapControllerState(ControllerState previousState, ControllerState currentState) {
        Map<PS3Component, GamepadComponentValue<PS3Component>> mapValues = new HashMap<>();
        //BUTTON_TRIANGLE
        mapValues.put(PS3Component.BUTTON_TRIANGLE, new GamepadComponentValue<>(PS3Component.BUTTON_TRIANGLE, previousState.y, currentState.y));
        //BUTTON_SQUARE
        mapValues.put(PS3Component.BUTTON_SQUARE, new GamepadComponentValue<>(PS3Component.BUTTON_SQUARE, previousState.x, currentState.x));
        //BUTTON_CROSS
        mapValues.put(PS3Component.BUTTON_CROSS, new GamepadComponentValue<>(PS3Component.BUTTON_CROSS, previousState.a, currentState.a));
        //BUTTON_ROUND
        mapValues.put(PS3Component.BUTTON_ROUND, new GamepadComponentValue<>(PS3Component.BUTTON_ROUND, previousState.b, currentState.b));
        //BUTTON_SELECT
        mapValues.put(PS3Component.BUTTON_SELECT, new GamepadComponentValue<>(PS3Component.BUTTON_SELECT, previousState.back, currentState.back));
        //BUTTON_START
        mapValues.put(PS3Component.BUTTON_START, new GamepadComponentValue<>(PS3Component.BUTTON_START, previousState.start, currentState.start));
        //BUTTON_MODE
        mapValues.put(PS3Component.BUTTON_MODE, new GamepadComponentValue<>(PS3Component.BUTTON_MODE, previousState.guide, currentState.guide));
        //BUTTON_LEFT_1
        mapValues.put(PS3Component.BUTTON_LEFT_1, new GamepadComponentValue<>(PS3Component.BUTTON_LEFT_1, previousState.lb, currentState.lb));
        //BUTTON_LEFT_2
        mapValues.put(PS3Component.BUTTON_LEFT_2, new GamepadComponentValue<>(PS3Component.BUTTON_LEFT_2, previousState.leftTrigger != 0, currentState.leftTrigger != 0));
        //BUTTON_LEFT_JOYSTICK_3
        mapValues.put(PS3Component.BUTTON_LEFT_JOYSTICK_3, new GamepadComponentValue<>(PS3Component.BUTTON_LEFT_JOYSTICK_3, previousState.leftStickClick, currentState.leftStickClick));
        //BUTTON_RIGHT_1
        mapValues.put(PS3Component.BUTTON_RIGHT_1, new GamepadComponentValue<>(PS3Component.BUTTON_RIGHT_1, previousState.rb, currentState.rb));
        //BUTTON_RIGHT_2
        mapValues.put(PS3Component.BUTTON_RIGHT_2, new GamepadComponentValue<>(PS3Component.BUTTON_RIGHT_2, previousState.rightTrigger != 0, currentState.rightTrigger != 0));
        //BUTTON_RIGHT_JOYSTICK_3
        mapValues.put(PS3Component.BUTTON_RIGHT_JOYSTICK_3, new GamepadComponentValue<>(PS3Component.BUTTON_RIGHT_JOYSTICK_3, previousState.rightStickClick, currentState.rightStickClick));
        //BUTTON_CROSS_LEFT
        mapValues.put(PS3Component.BUTTON_CROSS_LEFT, new GamepadComponentValue<>(PS3Component.BUTTON_CROSS_LEFT, previousState.dpadLeft, currentState.dpadLeft));
        //BUTTON_CROSS_RIGHT
        mapValues.put(PS3Component.BUTTON_CROSS_RIGHT, new GamepadComponentValue<>(PS3Component.BUTTON_CROSS_RIGHT, previousState.dpadRight, currentState.dpadRight));
        //BUTTON_CROSS_TOP
        mapValues.put(PS3Component.BUTTON_CROSS_TOP, new GamepadComponentValue<>(PS3Component.BUTTON_CROSS_TOP, previousState.dpadUp, currentState.dpadUp));
        //BUTTON_CROSS_BOTTOM
        mapValues.put(PS3Component.BUTTON_CROSS_BOTTOM, new GamepadComponentValue<>(PS3Component.BUTTON_CROSS_BOTTOM, previousState.dpadDown, currentState.dpadDown));
        //BUTTON_ANALOG_LEFT_2
        mapValues.put(PS3Component.BUTTON_ANALOG_LEFT_2, new GamepadComponentValue<>(PS3Component.BUTTON_ANALOG_LEFT_2, previousState.leftTrigger, currentState.leftTrigger));
        //BUTTON_ANALOG_RIGHT_2
        mapValues.put(PS3Component.BUTTON_ANALOG_RIGHT_2, new GamepadComponentValue<>(PS3Component.BUTTON_ANALOG_RIGHT_2, previousState.rightTrigger, currentState.rightTrigger));
        //JOYSTICK_LEFT_AXIS_X
        mapValues.put(PS3Component.JOYSTICK_LEFT_AXIS_X, new GamepadComponentValue<>(PS3Component.JOYSTICK_LEFT_AXIS_X, previousState.leftStickX, currentState.leftStickX));
        //JOYSTICK_LEFT_AXIS_Y
        mapValues.put(PS3Component.JOYSTICK_LEFT_AXIS_Y, new GamepadComponentValue<>(PS3Component.JOYSTICK_LEFT_AXIS_Y, previousState.leftStickY, currentState.leftStickY));
        //JOYSTICK_LEFT_AMPLITUDE
        mapValues.put(PS3Component.JOYSTICK_LEFT_AMPLITUDE, new GamepadComponentValue<>(PS3Component.JOYSTICK_LEFT_AMPLITUDE, previousState.leftStickMagnitude, currentState.leftStickMagnitude));
        //JOYSTICK_LEFT_ANGLE
        mapValues.put(PS3Component.JOYSTICK_LEFT_ANGLE, new GamepadComponentValue<>(PS3Component.JOYSTICK_LEFT_ANGLE, previousState.leftStickAngle, currentState.leftStickAngle));
        //JOYSTICK_RIGHT_AXIS_X
        mapValues.put(PS3Component.JOYSTICK_RIGHT_AXIS_X, new GamepadComponentValue<>(PS3Component.JOYSTICK_RIGHT_AXIS_X, previousState.rightStickX, currentState.rightStickX));
        //JOYSTICK_RIGHT_AXIS_Y
        mapValues.put(PS3Component.JOYSTICK_RIGHT_AXIS_Y, new GamepadComponentValue<>(PS3Component.JOYSTICK_RIGHT_AXIS_Y, previousState.rightStickY, currentState.rightStickY));
        //JOYSTICK_RIGHT_AMPLITUDE
        mapValues.put(PS3Component.JOYSTICK_RIGHT_AMPLITUDE, new GamepadComponentValue<>(PS3Component.JOYSTICK_RIGHT_AMPLITUDE, previousState.rightStickMagnitude, currentState.rightStickMagnitude));
        //JOYSTICK_RIGHT_ANGLE
        mapValues.put(PS3Component.JOYSTICK_RIGHT_ANGLE, new GamepadComponentValue<>(PS3Component.JOYSTICK_RIGHT_ANGLE, previousState.rightStickAngle, currentState.rightStickAngle));

        return mapValues;
    }
}
