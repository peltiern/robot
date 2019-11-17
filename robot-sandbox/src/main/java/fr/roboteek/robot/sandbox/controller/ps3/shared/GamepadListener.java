package fr.roboteek.robot.sandbox.controller.ps3.shared;

public interface GamepadListener<E extends GamepadEvent> {
    void onEvent(E event);
}
