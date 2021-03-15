package fr.roboteek.robot.sandbox.respeaker.usb4java;

import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Respeacker mic-array V2 test.
 * Reading DOA and voice activity parameters.
 * Using the usb4java library (http://usb4java.org/) and its sample.
 * Inspired by the python demo available on http://wiki.seeedstudio.com/ReSpeaker_Mic_Array_v2.0/#doa-direction-of-arrival
 * <p>
 * Nicolas Peltier (peltiern)
 */
public class RespeakerMicArrayV2Test {

    public static short VENDOR_ID = (short) 0x2886;
    public static short PRODUCT_ID = (short) 0x0018;
    public static short DOA_PARAMETER_ID = 21;
    public static short DOA_PARAMETER_OFFSET = 0;
    public static short VOICE_ACTIVITY_PARAMETER_ID = 19;
    public static short VOICE_ACTIVITY__PARAMETER_OFFSET = 32;

    public static void main(String args[]) throws InterruptedException {
        Context context = new Context();
        int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);

        Device device = findDevice(context, VENDOR_ID, PRODUCT_ID);
        DeviceHandle deviceHandle = getDeviceHandle(device);
        int i = 0;
        while (i < 1000) {
            int angle = read(deviceHandle, DOA_PARAMETER_ID, DOA_PARAMETER_OFFSET);
            int voiceActivity = read(deviceHandle, VOICE_ACTIVITY_PARAMETER_ID, VOICE_ACTIVITY__PARAMETER_OFFSET);
            System.out.println("ANGLE = " + angle + "\tVOICE = " + (voiceActivity == 1));
            i++;
            Thread.sleep(50);
        }

        LibUsb.close(deviceHandle);
        LibUsb.exit(context);
    }

    public static Device findDevice(Context context, short vendorId, short productId) {
        // Read the USB device list
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(context, list);
        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        try {
            // Iterate over all devices and scan for the right one
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }
                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
                    return device;
                }
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }

        // Device not found
        return null;
    }

    public static DeviceHandle getDeviceHandle(Device device) {
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", result);
        return handle;
    }

    public static int read(DeviceHandle deviceHandle, short id, short offset) {
        short cmd = (short) ((short) 0x80 | offset | (short) 0x40);
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        int transfered = LibUsb.controlTransfer(deviceHandle,
                (byte) (LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE),
                (byte) 0, cmd, id, buffer, 5000l);
        if (transfered < 0) {
            throw new LibUsbException("Control transfer failed", transfered);
        }
        int value = buffer.order(ByteOrder.LITTLE_ENDIAN).getInt(0);
        return value;
    }
}
