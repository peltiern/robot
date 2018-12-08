package fr.roboteek.robot.util.phidget;

import java.util.ArrayList;
import java.util.List;

import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.DeviceClass;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.PhidgetException;
import com.phidget22.RCServo;
import com.phidget22.RCServoPositionChangeEvent;
import com.phidget22.RCServoPositionChangeListener;
import com.phidget22.RCServoTargetPositionReachedEvent;
import com.phidget22.RCServoTargetPositionReachedListener;

/**
 * Implémentation d'un moteur servo-moteur via le servo-contrôleur Phidget.
 * @author Java Developer
 */
public class PhidgetMotor2 implements Motor, AttachListener, DetachListener, RCServoPositionChangeListener, RCServoTargetPositionReachedListener, ErrorListener {

	/** Moteur Phidget associé. */
	private RCServo rcServo;
	
	/** Position initiale du moteur. */
	private double positionInitiale;
	
	/** Position minimale du moteur. */
	private double positionMin;
	
	/** Position maximale du moteur. */
	private double positionMax;

	/** Limite de la vélocité. */
	private double velociteLimitePourPosition;

	/** Liste des écouteurs de changement de position. */
	private List<MotorPositionChangeListener> listeEcouteursChangementProsition = new ArrayList<MotorPositionChangeListener>();

	/**
	 * Constructeur d'un moteur Phidget.
	 * @param index index du moteur sur le contrôleur
	 */
	public PhidgetMotor2(int index, double positionInitiale, double positionMin, double positionMax) {
		try {
			this.positionInitiale = positionInitiale;
			this.positionMin = positionMin;
			this.positionMax = positionMax;
			rcServo = new RCServo();
			rcServo.addAttachListener(this);
			rcServo.addDetachListener(this);
			rcServo.addErrorListener(this);
			rcServo.addPositionChangeListener(this);
			rcServo.addTargetPositionReachedListener(this);
			
			// Configuration
			rcServo.setChannel(index);
			
			// Ouverture du moteur
			rcServo.open(5000);
			
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void rotate(double angle) {
		setPositionCible(getPositionReelle() + angle);
	}

	public void forward() {
		try {
			rcServo.setVelocityLimit(velociteLimitePourPosition);
			rcServo.setTargetPosition(positionMax);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void backward() {
		try {
			rcServo.setVelocityLimit(velociteLimitePourPosition);
			rcServo.setTargetPosition(positionMin);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			rcServo.setVelocityLimit(0);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public double getAccelerationMax() {
		try {
			return rcServo.getMaxAcceleration();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}


	public double getAccelerationMin() {
		try {
			return rcServo.getMinAcceleration();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public double getVelocityMax() {
		try {
			return rcServo.getMaxVelocityLimit();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}

	}

	public double getVelocityMin() {
		try {
			return rcServo.getMinVelocityLimit();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
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

	public double getAcceleration() {
		try {
			return rcServo.getAcceleration();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public void setAcceleration(double acceleration) {
		try {
			rcServo.setAcceleration(acceleration);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public double getVelocityLimit() {
		try {
			return rcServo.getVelocityLimit();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	public void setVelocityLimit(double velocity) {
		velociteLimitePourPosition = velocity;
		try {
			rcServo.setVelocityLimit(velocity);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public double getVelocity() {
		try {
			return rcServo.getVelocity();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Récupère la position réelle du moteur.
	 * @return la position réelle du moteur
	 */
	public double getPositionReelle() {
		try {
			return rcServo.getPosition();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Récupère la position cible du moteur.
	 * @return la position cible du moteur
	 */
	public double getPositionCible() {
		try {
			return rcServo.getTargetPosition();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}

	public void setPositionCible(double position) {
		try {
			rcServo.setVelocityLimit(velociteLimitePourPosition);
			rcServo.setTargetPosition(position);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isEngaged() {
		try {
			return rcServo.getEngaged();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public void setEngaged(boolean state) {
		try {
			rcServo.setEngaged(state);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isSpeedRampingState() {
		try {
			return rcServo.getSpeedRampingState();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public void setSpeedRampingState(boolean state) {
		try {
			rcServo.setSpeedRampingState(state);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isStopped() {
		try {
			return !rcServo.getIsMoving();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public double getTorque() {
		try {
			return rcServo.getTorque();
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}
	
	public void setTorque(double torque) {
		try {
			rcServo.setTorque(torque);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addMotorPositionChangeListener(MotorPositionChangeListener listener) {
		listeEcouteursChangementProsition.add(listener);
	}

	public void removePhidgetMotorPositionChangeListener(MotorPositionChangeListener listener) {
		listeEcouteursChangementProsition.remove(listener);
	}

	@Override
	public void onAttach(AttachEvent attachEvent) {
		// Une fois que le moteur est attaché, on l'active
		try {
			final RCServo moteur = (RCServo) attachEvent.getSource();
			
			/**
			* Get device information and display it.
			**/
			int serialNumber = moteur.getDeviceSerialNumber();
			String channelClass = moteur.getChannelClassName();
			int channel = moteur.getChannel();
			
			DeviceClass deviceClass = moteur.getDeviceClass();
			if (deviceClass != DeviceClass.VINT) {
				System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: " + serialNumber +
					  "\n\t-> Channel:  " + channel + "\n");
			} 
			else {            
				int hubPort = moteur.getHubPort();
				System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: " + serialNumber +
					  "\n\t-> Hub Port: " + hubPort + "\n\t-> Channel:  " + channel + "\n");
			}
			
			moteur.setTargetPosition(positionInitiale);
			moteur.setEngaged(true);
			
			System.out.println("Ca marche");
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void onError(ErrorEvent errorEvent) {
		System.out.println("Error: " + errorEvent.getDescription());
	}

	@Override
	public void onTargetPositionReached(RCServoTargetPositionReachedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPositionChange(RCServoPositionChangeEvent event) {
		// Envoi d'un évènement à l'ensemble des écouteurs
		if (listeEcouteursChangementProsition != null && !listeEcouteursChangementProsition.isEmpty()) {
			final MotorPositionChangeEvent evenement = new MotorPositionChangeEvent(this, event.getPosition());
			for (MotorPositionChangeListener ecouteur : listeEcouteursChangementProsition) {
				ecouteur.onPositionchanged(evenement);
			}
		}
	}

	@Override
	public void onDetach(DetachEvent arg0) {
		// TODO Auto-generated method stub

	}
	
	public static void main (String[] args) {
		final PhidgetMotor2 moteurG = new PhidgetMotor2(2, 92, 50, 150);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurG.setSpeedRampingState(true);
		moteurG.setVelocityLimit(60);
		moteurG.setPositionCible(110);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurG.setPositionCible(80);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurG.setVelocityLimit(60);
		moteurG.setPositionCible(110);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurG.setPositionCible(92);
		
		final PhidgetMotor2 moteurD = new PhidgetMotor2(3, 86, 50, 150);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurD.setSpeedRampingState(true);
		moteurD.setVelocityLimit(60);
		moteurD.setPositionCible(106);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurD.setPositionCible(76);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurD.setVelocityLimit(60);
		moteurD.setPositionCible(106);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurD.setPositionCible(86);
		System.exit(0);
	}
}
