package fr.roboteek.robot.sandbox.controller.ps3.jinput;

public class GamepadTest implements PS3Listener {

    public static void main(String[] args) {
        GamepadManager gamepadManager = new GamepadManager(Ps3Controller.class);
        GamepadTest listener = new GamepadTest();
        gamepadManager.addListener(listener);
        gamepadManager.start();
    }

    @Override
    public void onEvent(Ps3ControllerEvent event) {
        System.out.println("ESSAI = " + event.toString());
    }
}
