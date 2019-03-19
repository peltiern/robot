package fr.roboteek.robot.util.gamepad;

public interface GamepadListener<E extends GamepadEvent> {
    void onEvent(E event);
}
