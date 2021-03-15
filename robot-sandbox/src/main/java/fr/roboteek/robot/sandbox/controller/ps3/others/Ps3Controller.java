package fr.roboteek.robot.sandbox.controller.ps3.others;

import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.*;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import java.util.List;

public class Ps3Controller {

    public static void main(String[] args) throws Exception {

        BluetoothManager bluetoothManager = new BluetoothManagerBuilder()
                .withTinyBTransport(true)
                .build();
        bluetoothManager.addDeviceDiscoveryListener(discoveredDevice -> System.out.println("DISCOVER = " + discoveredDevice.getName() + " (" + discoveredDevice.getURL() + ")"));
        bluetoothManager.addAdapterDiscoveryListener(adapter -> System.out.println("ADAPTER = " + adapter.getName() + " (" + adapter.getURL() + ")"));
        bluetoothManager.getDiscoveredDevices().forEach(discoveredDevice -> {
            System.out.println("COMBINED = " + discoveredDevice.getName() + " (" + discoveredDevice.getURL() + ")" + discoveredDevice.isCombined());
        });
        bluetoothManager.getDeviceGovernor(new URL("/XX:XX:XX:XX:XX:XX/00:19:C1:64:9E:26")).getCharacteristics().forEach(url -> System.out.println("URL = " + url.toString()));
//                    .getDeviceGovernor(new URL("/XX:XX:XX:XX:XX:XX/00:19:C1:64:9E:26")).getCharacteristics().forEach(url -> System.out.println("URL = "+ url.toString()));

        //getDiscoveredDevices().stream().forEach(discoveredDevice -> System.out.println(discoveredDevice.getURL() + "," + discoveredDevice.getName()));
//                    .getCharacteristicGovernor(new URL("/XX:XX:XX:XX:XX:XX/00:19:C1:64:9E:26/"
//                            + "0000180f-0000-1000-8000-00805f9b34fb/00002a19-0000-1000-8000-00805f9b34fb"), true)
//                    .whenReady(CharacteristicGovernor::read)
//                    .thenAccept(data -> {
//                        System.out.println("Battery level: " + data[0]);
//                    }).get();
    }

}
