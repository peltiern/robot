package fr.roboteek.robot.sandbox.controller.ps3.hid4java;

import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

public class Test {

    public static void main(String[] args) {
        HidServices hidServices = HidManager.getHidServices();

// Provide a list of attached devices
        for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            System.out.println(hidDevice);
        }
    }
}
