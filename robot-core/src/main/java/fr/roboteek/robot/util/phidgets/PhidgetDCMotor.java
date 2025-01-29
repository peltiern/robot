package fr.roboteek.robot.util.phidgets;

import com.phidget22.*;
import lombok.Getter;

public class PhidgetDCMotor implements AttachListener, DetachListener {

    /**
     * Moteur Phidget associé.
     */
    private DCMotor motor;

    /**
     * Encodeur Phidget associé.
     */
    @Getter
    private Encoder encodeur;

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

            encodeur = new Encoder();
            encodeur.setDeviceSerialNumber(deviceSerialNumber);
            encodeur.setHubPort(hubPort);
            encodeur.addAttachListener(this);
            encodeur.addDetachListener(this);

            // Ouverture de l'encodeur
            encodeur.open(5000);
            encodeur.setIOMode(EncoderIOMode.OPEN_COLLECTOR_2K2);
            encodeur.setPosition(0);

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

    public void forwardToPosition(Double vitesse, Double acceleration, long position) {
        try {
            forward(vitesse, acceleration);
            while (encodeur.getPosition() < position) {
                Thread.sleep(20);
            }
        } catch (PhidgetException | InterruptedException e) {
            e.printStackTrace();
        }
        stop();
    }

    public void backward(Double vitesse, Double acceleration) {
        forward(-vitesse, acceleration);
    }

    public void backwardToPosition(Double vitesse, Double acceleration, long position) {
        try {
            backward(vitesse, acceleration);
            while (encodeur.getPosition() > position) {
                Thread.sleep(20);
            }
        } catch (PhidgetException | InterruptedException e) {
            e.printStackTrace();
        }
        stop();
    }

    public void stop() {
        try {
            motor.setAcceleration(100);
            motor.setTargetVelocity(0);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            encodeur.close();
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
    
    public long getPositionEncodeur() {
        try {
            return encodeur.getPosition();
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void main(String args[]) {
        PhidgetDCMotor m = new PhidgetDCMotor(561050, 2, 1);
        try {
            Thread.sleep(3000);
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
