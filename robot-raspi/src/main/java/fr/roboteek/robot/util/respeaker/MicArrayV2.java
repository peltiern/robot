package fr.roboteek.robot.util.respeaker;

import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MicArrayV2 {

    private static short VENDOR_ID = (short) 0x2886;
    private static short PRODUCT_ID = (short) 0x0018;

    /**
     * Instance de la classe (singleton).
     */
    private static MicArrayV2 instance;

    private Context context;
    private Device device;
    private DeviceHandle deviceHandle;


    private MicArrayV2() {
        init();
    }

    /**
     * Récupère l'instance de la classe (le singleton).
     *
     * @return l'instance de la classe
     */
    public synchronized static MicArrayV2 getInstance() {
        if (instance == null) {
            instance = new MicArrayV2();
        }
        return instance;
    }

    public int getDoaAngle() {
        return readInt(MicArrayV2Param.DOAANGLE);
    }

    public boolean isVoiceActivity() {
        return readInt(MicArrayV2Param.VOICEACTIVITY) == 1;
    }

    public boolean isSpeechDetected() {
        return readInt(MicArrayV2Param.SPEECHDETECTED) == 1;
    }

    public void stop() {
        LibUsb.close(deviceHandle);
        LibUsb.exit(context);
    }

    private void init() {
        context = new Context();
        int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to initialize libusb.", result);
        }
        device = findDevice();
        // TODO Gérer le null
        deviceHandle = getDeviceHandle();
        // TODO Gérer le null
    }

    private Device findDevice() {
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
                if (descriptor.idVendor() == VENDOR_ID && descriptor.idProduct() == PRODUCT_ID) {
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

    private DeviceHandle getDeviceHandle() {
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", result);
        return handle;
    }

    private ByteBuffer read(MicArrayV2Param param) {
        short cmd = (short) ((short) 0x80 | param.getOffset());
        if (param.getType() == MicArrayV2Param.TYPE.INT) {
            cmd |= (short) 0x40;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        int transfered = LibUsb.controlTransfer(deviceHandle,
                (byte) (LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE),
                (byte) 0, cmd, param.getIndex(), buffer, 5000l);
        if (transfered < 0) {
            throw new LibUsbException("Control transfer failed", transfered);
        }
        return buffer;
    }

    private int readInt(MicArrayV2Param param) {
        ByteBuffer buffer = read(param);
        int value = buffer.order(ByteOrder.LITTLE_ENDIAN).getInt(0);
        return value;
    }

    private float readFloat(MicArrayV2Param param) {
        ByteBuffer buffer = read(param);
        float value = buffer.order(ByteOrder.LITTLE_ENDIAN).getFloat(0);
        return value;
    }
}
