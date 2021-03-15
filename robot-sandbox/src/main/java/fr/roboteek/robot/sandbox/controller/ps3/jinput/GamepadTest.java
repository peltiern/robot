package fr.roboteek.robot.sandbox.controller.ps3.jinput;

public class GamepadTest implements PS3Listener {

    public static void main(String[] args) {
        GamepadManager gamepadManager = new GamepadManager(Ps3Controller.class);
        gamepadManager.addListener(new GamepadTest());
        gamepadManager.start();
    }

    @Override
    public void onEvent(Ps3ControllerEvent event) {
        event.getModifiedComponents().forEach(ps3Component -> {
            System.out.println(event.getMapValues().get(ps3Component));
        });
    }
}
