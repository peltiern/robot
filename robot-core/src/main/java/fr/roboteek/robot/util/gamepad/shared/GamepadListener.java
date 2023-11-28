package fr.roboteek.robot.util.gamepad.shared;

public interface GamepadListener<E extends GamepadEvent> {
    void onEvent(E event);
}
