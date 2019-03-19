package fr.roboteek.robot.util.phidget;

import java.util.concurrent.atomic.AtomicBoolean;

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
import com.phidget22.RCServoVelocityChangeEvent;
import com.phidget22.RCServoVelocityChangeListener;

/**
 * Implémentation d'un moteur servo-moteur via le servo-contrôleur Phidget.
 * @author Java Developer
 */
public class PhidgetMotor2 implements AttachListener, DetachListener, RCServoPositionChangeListener, RCServoTargetPositionReachedListener, ErrorListener, RCServoVelocityChangeListener {

	/** Moteur Phidget associé. */
	private RCServo rcServo;

	/** Position initiale du moteur. */
	private double positionInitiale;

	/** Position minimale du moteur. */
	private double positionMin;

	/** Position maximale du moteur. */
	private double positionMax;

	/** Vitesse par défaut. */
	private double vitesseParDefaut;
	
	/** Accélération par défaut. */
	private double accelerationParDefaut;
	
	/** Flag indiquant que la position est atteinte. */
	private AtomicBoolean positionAtteinte = new AtomicBoolean(true);

	/**
	 * Constructeur d'un moteur Phidget.
	 * @param index index du moteur sur le contrôleur
	 */
	public PhidgetMotor2(int index, double positionInitiale, double positionMin, double positionMax, double vitesseParDefaut, double accelerationParDefaut) {
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
			rcServo.setChannel(index);

			// Ouverture du moteur
			rcServo.open(5000);

		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public synchronized void setPositionCible(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
		try {
			System.out.println("POSITION DEMANDEE à " + System.currentTimeMillis() + " = " + position);
			positionAtteinte.set(false);
			setAcceleration(acceleration);
			rcServo.setTargetPosition(position);
			setVitesse(vitesse);
			while (waitForPosition && !positionAtteinte.get());
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO A voir si nécessaire
			//rcServo.setTargetPosition(rcServo.getPosition());
			positionAtteinte.set(true);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			return positionAtteinte.get() && rcServo.getVelocity() == 0;
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	private void setVitesse(Double vitesse) {
		try {
			if (vitesse != null) {
				rcServo.setVelocityLimit(vitesse.doubleValue());
			} else {
				rcServo.setVelocityLimit(vitesseParDefaut);
			}
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

			moteur.setDataInterval(32);
			moteur.setAcceleration(accelerationParDefaut);
			moteur.setTargetPosition(positionInitiale);
			moteur.setVelocityLimit(vitesseParDefaut);
			moteur.setEngaged(true);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Override
	public void onTargetPositionReached(RCServoTargetPositionReachedEvent event) {
		if (event.getSource().equals(rcServo)) {
			try {
				positionAtteinte.set(true);
				System.out.println("POSITION REACHED = time = " + System.currentTimeMillis() + ", event = " + event.getPosition() + ", servo = " + rcServo.getChannel() + ":" + rcServo.getPosition());
			} catch (PhidgetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}
	
	@Override
	public void onPositionChange(RCServoPositionChangeEvent event) {
		if (event.getSource() == rcServo) {
//			// Envoi d'un évènement à l'ensemble des écouteurs
//			if (listeEcouteursChangementPosition != null && !listeEcouteursChangementPosition.isEmpty()) {
//				final MotorPositionChangeEvent evenement = new MotorPositionChangeEvent(this, event.getPosition());
//				for (MotorPositionChangeListener ecouteur : listeEcouteursChangementPosition) {
//					ecouteur.onPositionchanged(evenement);
//				}
//			}
		}

	}

	@Override
	public void onError(ErrorEvent errorEvent) {
		System.out.println("Error: " + errorEvent.getDescription());
	}

	@Override
	public void onDetach(DetachEvent arg0) {
		// TODO Auto-generated method stub

	}

	public static void main (String[] args) {
		final PhidgetMotor2 moteurG = new PhidgetMotor2(2, 92, 50, 150, 60, 600);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moteurG.setSpeedRampingState(true);
		moteurG.setPositionCible(110, null, null, true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurG.setPositionCible(80, null, null, true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurG.setPositionCible(110, null, null, true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurG.setPositionCible(92, null, null, true);
		moteurG.setPositionCible(110, null, null, false);

		final PhidgetMotor2 moteurD = new PhidgetMotor2(3, 86, 50, 150, 60, 600);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurD.setSpeedRampingState(true);
		moteurD.setPositionCible(106, null, null, true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurD.setPositionCible(76, null, null, true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurD.setPositionCible(106, null, null, true);
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		moteurD.setPositionCible(86, null, null, true);
		System.exit(0);
	}

	@Override
	public void onVelocityChange(RCServoVelocityChangeEvent event) {
		if (event.getSource() == rcServo) {
				System.out.println("Changement Vitesse = time = " + System.currentTimeMillis() + ", " + event.getVelocity());
		};
	}
}
