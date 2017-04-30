/**
 * 
 */
package fr.roboteek.robot.systemenerveux.event;

import net.engio.mbassy.bus.MBassador;

/**
 * @author Nicolas
 *
 */
public class RobotEventBus {
	
	private MBassador<RobotEvent> mBassador;

	/** Constructeur privé */	
	private RobotEventBus() {
		mBassador = new MBassador<>();
	}

	/** Holder */
	private static class RobotEventBusHolder
	{		
		/** Instance unique non préinitialisée */
		private final static RobotEventBus instance = new RobotEventBus();
	}

	/** Point d'accès pour l'instance unique du singleton */
	public static RobotEventBus getInstance()
	{
		return RobotEventBusHolder.instance;
	}
	
	public synchronized void subscribe(Object listener) {
		mBassador.subscribe(listener);
	}
	
	public synchronized void unsubscribe(Object listener) {
		mBassador.unsubscribe(listener);
	}
	
	public void publish(RobotEvent event) {
		mBassador.publish(event);
	}
	
	public void publishAsync(RobotEvent event) {
		mBassador.publishAsync(event);
	}

}
