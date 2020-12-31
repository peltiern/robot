/**
 * 
 */
package fr.roboteek.robot.systemenerveux.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.Executors;

/**
 * @author Nicolas
 *
 */
public class RobotEventBus {

	private EventBus eventBus;
	private AsyncEventBus asyncEventBus;

	/** Constructeur privé */
	private RobotEventBus() {
		eventBus = new EventBus("RobotEventBus");
		asyncEventBus = new AsyncEventBus("RobotEventBus ASYNC", Executors.newFixedThreadPool(100));
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
		eventBus.register(listener);
		asyncEventBus.register(listener);
	}

	public synchronized void unsubscribe(Object listener) {
		eventBus.unregister(listener);
		asyncEventBus.unregister(listener);
	}

	public void publish(RobotEvent event) {
		eventBus.post(event);
	}

	public void publishAsync(RobotEvent event) {
		asyncEventBus.post(event);
	}

}
