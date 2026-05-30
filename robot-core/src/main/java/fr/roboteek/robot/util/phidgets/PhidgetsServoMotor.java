package fr.roboteek.robot.util.phidgets;

import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.PhidgetException;
import com.phidget22.RCServo;
import com.phidget22.RCServoPositionChangeEvent;
import com.phidget22.RCServoPositionChangeListener;
import com.phidget22.RCServoTargetPositionReachedEvent;
import com.phidget22.RCServoTargetPositionReachedListener;
import com.phidget22.RCServoVelocityChangeEvent;
import com.phidget22.RCServoVelocityChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implémentation d'un moteur servo-moteur via le servo-contrôleur Phidgets.
 *
 * @author Java Developer
 */
public class PhidgetsServoMotor implements AttachListener, DetachListener, RCServoPositionChangeListener, RCServoTargetPositionReachedListener, ErrorListener, RCServoVelocityChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(PhidgetsServoMotor.class);

    /**
     * Moteur Phidget associé.
     */
    private RCServo rcServo;

    /**
     * Position initiale du moteur.
     */
    private double positionInitiale;

    /**
     * Position minimale du moteur.
     */
    private double positionMin;

    /**
     * Position maximale du moteur.
     */
    private double positionMax;

    /**
     * Vitesse par défaut.
     */
    private double vitesseParDefaut;

    /**
     * Accélération par défaut.
     */
    private double accelerationParDefaut;

    /**
     * Flag indiquant que la position est atteinte.
     */
    private AtomicBoolean positionAtteinte = new AtomicBoolean(true);

    /**
     * Verrou utilisé pour bloquer l'appelant jusqu'à ce que la position cible soit atteinte.
     */
    private volatile CountDownLatch positionLatch = new CountDownLatch(0);

    /**
     * Constructeur d'un moteur Phidget.
     *
     * @param index index du moteur sur le contrôleur
     */
    public PhidgetsServoMotor(int index, double positionInitiale, double positionMin, double positionMax, double vitesseParDefaut, double accelerationParDefaut) {
        try {
            this.positionInitiale = positionInitiale;
            this.positionMin = positionMin;
            this.positionMax = positionMax;
            this.vitesseParDefaut = vitesseParDefaut;
            this.accelerationParDefaut = accelerationParDefaut;
            rcServo = new RCServo();
            rcServo.addAttachListener(this);
            rcServo.addDetachListener(this);
            rcServo.addErrorListener(this);
            rcServo.addPositionChangeListener(this);
            rcServo.addTargetPositionReachedListener(this);
            rcServo.addVelocityChangeListener(this);

            // Configuration
            rcServo.setDeviceSerialNumber(561050);
            rcServo.setHubPort(0);
            rcServo.setChannel(index);

            // Ouverture du moteur
            rcServo.open(5000);

        } catch (PhidgetException e) {
            logger.error("Erreur lors de l'initialisation du moteur canal {}", index, e);
        }
    }

    /**
     * Récupère la position cible du moteur.
     *
     * @return la position cible du moteur
     */
    public double getPositionCible() {
        try {
            return rcServo.getTargetPosition();
        } catch (PhidgetException e) {
            logger.error("Erreur lors de la récupération de la position cible", e);
            return 0;
        }
    }

    public synchronized void setPositionCible(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        try {
            positionAtteinte.set(false);
            positionLatch = new CountDownLatch(1);
            setAcceleration(acceleration);
            rcServo.setTargetPosition(position);
            setVitesse(vitesse);
            if (waitForPosition) {
                try {
                    if (!positionLatch.await(10, TimeUnit.SECONDS)) {
                        logger.warn("Timeout en attente de position pour le moteur canal {}", rcServo.getChannel());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (PhidgetException e) {
            logger.error("Erreur lors du positionnement du moteur", e);
        }
    }

    /**
     * Récupère la position réelle du moteur.
     *
     * @return la position réelle du moteur
     */
    public double getPositionReelle() {
        try {
            return rcServo.getPosition();
        } catch (PhidgetException e) {
            logger.error("Erreur lors de la récupération de la position réelle du moteur", e);
            return 0;
        }
    }

    public void rotate(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        setPositionCible(getPositionReelle() + angle, vitesse, acceleration, waitForPosition);
    }

    public void forward(Double vitesse, Double acceleration, boolean waitForPosition) {
        setPositionCible(positionMax, vitesse, acceleration, waitForPosition);
    }

    public void backward(Double vitesse, Double acceleration, boolean waitForPosition) {
        setPositionCible(positionMin, vitesse, acceleration, waitForPosition);
    }

    public void stop() {
        try {
            rcServo.setVelocityLimit(0);
            positionAtteinte.set(true);
            positionLatch.countDown();
        } catch (PhidgetException e) {
            logger.error("Erreur lors de l'arrêt du moteur", e);
        }
    }

    public void close() {
        try {
            rcServo.close();
        } catch (PhidgetException e) {
            logger.error("Erreur lors de la fermeture du moteur", e);
        }
    }

    public double getPositionMax() {
        return positionMax;
    }

    public void setPositionMax(double position) {
        this.positionMax = position;
    }

    public double getPositionMin() {
        return positionMin;
    }

    public void setPositionMin(double position) {
        this.positionMin = position;
    }

    public boolean isEngaged() {
        try {
            return rcServo.getEngaged();
        } catch (PhidgetException e) {
            logger.error("Erreur lors de la récupération de l'état engaged", e);
            return false;
        }
    }

    public void setEngaged(boolean state) {
        try {
            rcServo.setEngaged(state);
        } catch (PhidgetException e) {
            logger.error("Erreur lors du changement de l'état engaged à {}", state, e);
        }
    }

    public boolean isSpeedRampingState() {
        try {
            return rcServo.getSpeedRampingState();
        } catch (PhidgetException e) {
            logger.error("Erreur lors de la récupération du speed ramping state", e);
            return false;
        }
    }

    public void setSpeedRampingState(boolean state) {
        try {
            rcServo.setSpeedRampingState(state);
        } catch (PhidgetException e) {
            logger.error("Erreur lors du changement du speed ramping state à {}", state, e);
        }
    }

    public boolean isStopped() {
        try {
            return positionAtteinte.get() && rcServo.getVelocity() == 0;
        } catch (PhidgetException e) {
            logger.error("Erreur lors de la vérification si le moteur est arrêté", e);
            return false;
        }
    }

    public void setVitesse(Double vitesse) {
        try {
            if (vitesse != null) {
                rcServo.setVelocityLimit(vitesse.doubleValue());
            } else {
                rcServo.setVelocityLimit(vitesseParDefaut);
            }
        } catch (PhidgetException e) {
            logger.error("Erreur lors du réglage de la vitesse", e);
        }
    }

    private void setAcceleration(Double acceleration) {
        try {
            if (acceleration != null) {
                rcServo.setAcceleration(acceleration.doubleValue());
            } else {
                rcServo.setAcceleration(accelerationParDefaut);
            }
        } catch (PhidgetException e) {
            logger.error("Erreur lors du réglage de l'accélération", e);
        }
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        try {
            if (attachEvent.getSource().equals(rcServo)) {
                rcServo.setAcceleration(accelerationParDefaut);
                rcServo.setTargetPosition(positionInitiale);
                rcServo.setVelocityLimit(vitesseParDefaut);
                rcServo.setEngaged(true);
                logger.info("Servo canal {} attaché", rcServo.getChannel());
            }
        } catch (PhidgetException e) {
            logger.error("Erreur lors de l'attachement du servo", e);
        }
    }

    @Override
    public void onTargetPositionReached(RCServoTargetPositionReachedEvent event) {
        if (event.getSource().equals(rcServo)) {
            positionAtteinte.set(true);
            positionLatch.countDown();
        }
    }

    @Override
    public void onPositionChange(RCServoPositionChangeEvent event) {
    }

    @Override
    public void onError(ErrorEvent errorEvent) {
        logger.error("Erreur Phidgets : {}", errorEvent.getDescription());
    }

    @Override
    public void onDetach(DetachEvent event) {
        if (event.getSource().equals(rcServo)) {
            try {
                logger.info("Servo canal {} détaché", rcServo.getChannel());
            } catch (PhidgetException e) {
                logger.error("Erreur lors du détachement du servo", e);
            }
        }
    }

    public static void main(String[] args) {
        final PhidgetsServoMotor moteurG = new PhidgetsServoMotor(0, 90, 50, 150, 100, 2000);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        moteurG.setSpeedRampingState(true);
        moteurG.setPositionCible(130, null, null, true);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        moteurG.setSpeedRampingState(true);
        moteurG.setPositionCible(60, null, null, true);
        System.exit(0);
    }

    @Override
    public void onVelocityChange(RCServoVelocityChangeEvent event) {
        if (event.getSource() == rcServo) {
            logger.debug("Changement vitesse : {}", event.getVelocity());
        }
    }
}
