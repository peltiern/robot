package fr.roboteek.robot.util.phidgets;

import com.phidget22.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhidgetDCMotor implements AttachListener, DetachListener {

    private static final Logger logger = LoggerFactory.getLogger(PhidgetDCMotor.class);

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
            motor.setAcceleration(100);
            motor.setTargetVelocity(0);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Vitesse effectivement appliquée par le contrôleur (-1 à 1), ou {@code null} si le canal ne
     * répond pas (moteur détaché, hub débranché).
     * <p>
     * C'est la vitesse <b>courante</b> et non la consigne : pendant une rampe d'accélération elle
     * n'a pas encore atteint la cible. Sert au watchdog, à deux titres : la lecture elle-même
     * prouve que le canal Phidget répond, et sa valeur dit si la roue tourne réellement — plus
     * fiable que de déduire le mouvement du dernier ordre reçu.
     *
     * @return la vitesse courante, ou {@code null} si elle est illisible
     */
    public Double getVitesse() {
        try {
            return motor.getVelocity();
        } catch (PhidgetException e) {
            return null;
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
                logger.debug("Moteur DC {} attaché", motor.getHubPort());
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
