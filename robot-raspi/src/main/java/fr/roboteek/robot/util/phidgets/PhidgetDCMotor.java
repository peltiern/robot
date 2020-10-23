package fr.roboteek.robot.util.phidgets;

import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DCMotor;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.PhidgetException;

public class PhidgetDCMotor implements AttachListener, DetachListener {

    /**
     * Moteur Phidget associé.
     */
    private DCMotor motor;

    /**
     * Accélération par défaut.
     */
    private double accelerationParDefaut;

    /**
     * Constructeur d'un moteur Phidget.
     */
    public PhidgetDCMotor(int deviceSerialNumber, int hubPort, double accelerationParDefaut) {
        try {
            this.accelerationParDefaut = accelerationParDefaut;

            motor = new DCMotor();
            motor.setDeviceSerialNumber(deviceSerialNumber);
            motor.setHubPort(hubPort);
            motor.addAttachListener(this);
            motor.addDetachListener(this);

            // Ouverture du moteur
            motor.open(5000);

        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void forward(Double vitesse, Double acceleration) {
        try {
            motor.setAcceleration(acceleration);
            motor.setTargetVelocity(vitesse);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public void backward(Double vitesse, Double acceleration) {
        forward(-vitesse, acceleration);
    }

    public void stop() {
        try {
            motor.setTargetVelocity(0);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            motor.close();
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        // Une fois que le moteur est attaché, on l'active
        try {
            if (attachEvent.getSource() == motor) {
                //moteur.setDataInterval(32);
                motor.setAcceleration(accelerationParDefaut);
                motor.setTargetVelocity(0);
                System.out.println("DC motor " + motor.getHubPort() + " attached");
            }

        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach(DetachEvent detachEvent) {

    }

    public static void main(String args[]) {
        PhidgetDCMotor m = new PhidgetDCMotor(561050, 0, 1);
        try {
            Thread.sleep(10000);
            m.forward(0.5, 1.0);
            Thread.sleep(3000);
            m.backward(0.5, 1.0);
            Thread.sleep(3000);
            m.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // TODO Voir pour gérer la température et le courant pour pouvoir envoyer des évènements
    // TODO peut-être créer une classe PhidgetTemperature et PhidgetCurrent
}
