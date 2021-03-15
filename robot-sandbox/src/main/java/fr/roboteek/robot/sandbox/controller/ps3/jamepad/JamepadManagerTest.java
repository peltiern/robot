package fr.roboteek.robot.sandbox.controller.ps3.jamepad;

public class JamepadManagerTest implements PS3Listener {

    public static void main(String[] args) {
        JamepadManager jamepadManager = new JamepadManager();
        jamepadManager.addListener(new JamepadManagerTest());
        jamepadManager.start();
    }

    @Override
    public void onEvent(Ps3ControllerEvent event) {
        event.getModifiedComponents().forEach(ps3Component -> {
            System.out.println(event.getMapValues().get(ps3Component));
        });
    }
}
