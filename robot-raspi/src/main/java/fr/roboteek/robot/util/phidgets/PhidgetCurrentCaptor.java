package fr.roboteek.robot.util.phidgets;

import com.phidget22.*;
import fr.roboteek.robot.systemenerveux.event.CourantEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

public class PhidgetCurrentCaptor implements CurrentInputCurrentChangeListener {

    private final String nomSource;

    private CurrentInput currentInput;

    public PhidgetCurrentCaptor(int deviceSerialNumber, int hubPort, String nomSource) {
        this.nomSource = nomSource;
        try {
            currentInput = new CurrentInput();
            currentInput.setDeviceSerialNumber(deviceSerialNumber);
            currentInput.setHubPort(hubPort);

            // Ouverture du capteur
            currentInput.open(5000);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onCurrentChange(CurrentInputCurrentChangeEvent event) {
        if (event.getSource().equals(currentInput)) {
            CourantEvent courantEvent = new CourantEvent();
            courantEvent.setNomSource(nomSource);
            courantEvent.setCourant(event.getCurrent());
            RobotEventBus.getInstance().publishAsync(courantEvent);
        }
    }
}
