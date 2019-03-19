package fr.roboteek.robot.sandbox.controller.ps3.jinput;

public interface GamepadListener<E extends GamepadEvent> {
    void onEvent(E event);
}
