package fr.roboteek.robot.util.phidgets;

import com.phidget22.PhidgetException;
import com.phidget22.TemperatureSensor;
import com.phidget22.TemperatureSensorTemperatureChangeEvent;
import com.phidget22.TemperatureSensorTemperatureChangeListener;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.TemperatureEvent;

public class PhidgetTemperatureCaptor implements TemperatureSensorTemperatureChangeListener {

    private final String nomSource;

    private TemperatureSensor temperatureSensor;

    public PhidgetTemperatureCaptor(int deviceSerialNumber, int hubPort, String nomSource) {
        this.nomSource = nomSource;
        try {
            temperatureSensor = new TemperatureSensor();
            temperatureSensor.setDeviceSerialNumber(deviceSerialNumber);
            temperatureSensor.setHubPort(hubPort);

            // Ouverture du capteur
            temperatureSensor.open(5000);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onTemperatureChange(TemperatureSensorTemperatureChangeEvent event) {
        if (event.getSource().equals(temperatureSensor)) {
            TemperatureEvent temperatureEvent = new TemperatureEvent();
            temperatureEvent.setNomSource(nomSource);
            temperatureEvent.setTemperature(event.getTemperature());
            RobotEventBus.getInstance().publishAsync(temperatureEvent);
        }
    }
}
