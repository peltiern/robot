package fr.roboteek.robot.util.phidget;

import java.util.ArrayList;
import java.util.List;

import com.phidgets.AdvancedServoPhidget;
import com.phidgets.PhidgetException;
import com.phidgets.event.ServoPositionChangeEvent;
import com.phidgets.event.ServoPositionChangeListener;

/**
 * Implémentation d'un moteur servo-moteur via le servo-contrôleur Phidget.
 * @author Java Developer
 */
public class PhidgetMotor implements Motor, ServoPositionChangeListener {
    
    /** Instance du servo contrôleur. */
    private AdvancedServoPhidget servoController;;
    
    /** Index du moteur sur le servo-contrôleur. */
    private int index;
    
    public int getIndex() {
        return index;
    }

    /** Liste des écouteurs de changement de position. */
    private List<MotorPositionChangeListener> listeEcouteursChangementProsition = new ArrayList<MotorPositionChangeListener>();
    
    /**
     * Constructeur d'un moteur Phidget.
     * @param index index du moteur
     * @param servoController instance du servo-contrôleur
     */
    public PhidgetMotor(int index, AdvancedServoPhidget servoController) {
        this.index = index;
        this.servoController = servoController;
        try {
            servoController.setServoType(index, AdvancedServoPhidget.PHIDGET_SERVO_HITEC_HS422);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void rotate(double angle) {
        setPosition(getPosition() + angle);
    }

    public void forward() {
        try {
            servoController.setVelocityLimit(index, 20);
            servoController.setPosition(index, servoController.getPositionMax(index));
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void backward() {
        try {
            servoController.setVelocityLimit(index, 20);
            servoController.setPosition(index, servoController.getPositionMin(index));
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
//            servoController.setPosition(index, servoController.getPosition(index));
            servoController.setVelocityLimit(index, 0);
      } catch (PhidgetException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
    }
    
    /**
     * Returns the maximum acceleration that a motor will accept, or return.
     * <p>This value is in degrees per second squared.
     * @return Maximum acceleration
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getAccelerationMax() {
        try {
            return servoController.getAccelerationMax(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Returns the minimum acceleration that a motor will accept, or return.
     * <p>This value is in degrees per second squared.
     * @return Minimum acceleration
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getAccelerationMin() {
        try {
            return servoController.getAccelerationMin(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Returns the maximum velocity that a servo motor will accept, or return.
     * <p>This value is in degrees per second.
     * @return Maximum velocity
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getVelocityMax() {
        try {
            return servoController.getVelocityMax(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Returns the minimum velocity that a servo motor will accept, or return.
     * <p>This value is in degrees per second.
     * @return Minimum velocity
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getVelocityMin() {
        try {
            return servoController.getVelocityMin(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Returns the maximum position that a servo motor will accept, or return.
     * <p>This value is in degrees.
     * @return Maximum position
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getPositionMax() {
        try {
            return servoController.getPositionMax(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Sets the maximum position that a servo motor will accept, or return. This is for limiting the range of motion of the servo
     * controller. The Maximum cannot be extended beyond it's original value.
     * <p>This value is in degrees.
     * @param position Maximum position
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setPositionMax(double position) {
        try {
            servoController.setPositionMax(index, position);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns the minimum position that a servo motor will accept, or return.
     * <p>This value uses the same units as 
     * <p>This value is in degrees.
     * @return Minimum position
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getPositionMin() {
        try {
            return servoController.getPositionMin(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Sets the minimum position that a servo motor will accept, or return. This is for limiting the range of motion of the servo
     * controller. The Minimum cannot be extended beyond it's original value.
     * <p>This value is in degrees.
     * @param position Minimum position
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setPositionMin(double position) {
        try {
            servoController.setPositionMin(index, position);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns a motor's acceleration. The valid range is between {@link #getAccelerationMin getAccelerationMin} 
     * and {@link #getAccelerationMax getAccelerationMax}, and refers to how fast the Servo Controller will change the speed of a motor.
     * <p>This value is in degrees per second squared.
     * @return acceleration of motor
     * @throws PhidgetException If this Phidget is not opened and attached, if the index is invalid, or if the acceleration is unknown.
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getAcceleration() {
        try {
            return servoController.getAcceleration(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Sets a motor's acceleration. 
     * The valid range is between {@link #getAccelerationMin getAccelerationMin} and {@link #getAccelerationMax getAccelerationMax}. 
     * This controls how fast the motor changes speed.
     * <p>This value is in degrees per second squared.
     * @param acceleration requested acceleration for that motor
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index or acceleration are invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setAcceleration(double acceleration) {
        try {
            servoController.setAcceleration(index, acceleration);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns a motor's velocity limit. This is the maximum velocity that the motor will turn at. 
     * The valid range is between {@link #getVelocityMin getVelocityMin} and {@link #getVelocityMax getVelocityMax}, 
     * with 0 being stopped.
     * <p>This value is in degrees per second.
     * @return current speed of the motor
     * @throws PhidgetException If this Phidget is not opened and attached, if the index is invalid, or if the velocity in unknown. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getVelocityLimit() {
        try {
            return servoController.getVelocityLimit(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Sets a motor's velocity limit. This is the maximum velocity that the motor will turn at.
     * The valid range is between {@link #getVelocityMin getVelocityMin} and {@link #getVelocityMax getVelocityMax}, 
     * with 0 being stopped.
     * <p>This value is in degrees per second.
     * @param velocity requested velocity for the motor
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index or velocity are invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setVelocityLimit(double velocity) {
        try {
            servoController.setVelocityLimit(index, velocity);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns a motor's current velocity. The valid range is between {@link #getVelocityMin getVelocityMin} and {@link #getVelocityMax getVelocityMax}, 
     * with 0 being stopped.
     * <p>This value is in degrees per second.
     * @return current speed of the motor
     * @throws PhidgetException If this Phidget is not opened and attached, if the index is invalid, or if the velocity in unknown. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getVelocity() {
        try {
            return servoController.getVelocity(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Returns a motor's current position. This is the actual position that the motor is at right now.
     * The valid range is between {@link #getPositionMin getPositionMin} and {@link #getPositionMax getPositionMax}.
     * <p>This value is in degrees.
     * @return current position of the motor
     * @throws PhidgetException If this Phidget is not opened and attached, if the index is invalid, or if the position in unknown. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getPosition() {
        try {
            return servoController.getPosition(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Sets a motor's target position. Use this is set the target position for the servo. 
     * If the servo is {@link #setEngaged engaged} it will start moving towards this target position.
     * The valid range is between {@link #getPositionMin getPositionMin} and {@link #getPositionMax getPositionMax}.
     * <p>This value is in degrees.
     * @param position target position of the motor
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index or position are invalid. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setPosition(double position) {
        try {
            servoController.setPosition(index, position);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns the engaged state of a motor.
     *
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is out of range. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public boolean isEngaged() {
        try {
            return servoController.getEngaged(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Engage or disengage a motor.
     * <p>
     * This engages or disengages the servo motor. The motors are by default disengaged when the servo controller is plugged in.
     * When the servo is disengaged, position, velocity, etc. can all be set, but the motor will not start moving until it is engaged.
     * If position is read when a motor is disengaged, it will throw an exception.
     * <p>
     * This corresponds to a PCM of 0 being sent to the servo.
     *
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is out of range. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setEngaged(boolean state) {
        try {
            servoController.setEngaged(index, state);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns the speed ramping state of a motor.
     *
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is out of range. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public boolean isSpeedRampingOn() {
        try {
            return servoController.getSpeedRampingOn(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Sets the speed ramping state.
     * <p>
     * Disable speed ramping to disable velocity and acceleration control. With speed ramping disabled, the servo will be sent to
     * the desired position immediately upon receiving the command. This is how the regular Phidget Servo Controller works.
     * <p>
     * This is turned on by default.
     *
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is out of range. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public void setSpeedRampingOn(boolean state) {
        try {
            servoController.setSpeedRampingOn(index, state);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Returns a motor's current usage. The valid range depends on the servo controller.
     * This value is in Amps.
     * @return current usage of the motor
     * @throws PhidgetException If this Phidget is not opened and attached, if the index is invalid, or if the value is unknown. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public double getCurrent() {
        try {
            return servoController.getCurrent(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    /**
     * Returns the stopped state of a motor. Use this to determine if the motor is moving and/or up to date with the latest commands you have sent.
     * If this is true, the motor is guaranteed to be stopped and to have processed every command issued. Generally, this would
     * be polled after a target position is set to wait until that position is reached.
     *
     * @throws PhidgetException If this Phidget is not opened and attached, or if the index is out of range. 
     * See {@link com.phidgets.Phidget#open(int) open} for information on determining if a device is attached.
     */
    public boolean isStopped() {
        try {
            return servoController.getStopped(index);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    
    public void addMotorPositionChangeListener(MotorPositionChangeListener listener) {
        listeEcouteursChangementProsition.add(listener);
    }
    
    public void removePhidgetMotorPositionChangeListener(MotorPositionChangeListener listener) {
        listeEcouteursChangementProsition.remove(listener);
    }

    public void servoPositionChanged(ServoPositionChangeEvent event) {
        if (event.getIndex() == index) {
            // Envoi d'un évènement à l'ensemble des écouteurs
            if (listeEcouteursChangementProsition != null && !listeEcouteursChangementProsition.isEmpty()) {
                final MotorPositionChangeEvent evenement = new MotorPositionChangeEvent(this, event.getValue());
                for (MotorPositionChangeListener ecouteur : listeEcouteursChangementProsition) {
                    ecouteur.onPositionchanged(evenement);
                }
            }
        }
    }
}
