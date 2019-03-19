package fr.roboteek.robot.sandbox.controller.ps3.bluecove;

public class RemoteBluetoothServer {

    public static void main(String[] args) {
        Thread waitThread = new Thread(new WaitThread());
        waitThread.start();
    }
}
