package fr.roboteek.robot.util.gamepad.jinput;

public class GamepadTest implements LogitechListener {

    public static void main(String[] args) {
        GamepadManager gamepadManager = new GamepadManager(LogitechController.class);
        gamepadManager.addListener(new GamepadTest());
        gamepadManager.start();
    }

    @Override
    public void onEvent(LogitechControllerEvent event) {
        event.getModifiedComponents().forEach(ps3Component -> {
            System.out.println(event.getMapValues().get(ps3Component));
        });
    }
}
