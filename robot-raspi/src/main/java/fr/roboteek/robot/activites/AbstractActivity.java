package fr.roboteek.robot.activites;

import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;


/**
 * Abstract class for an activity.
 */
public abstract class AbstractActivity {

    /**
     * Initializes the activity before event listeners activation.
     * For example, to load data, initialize values, ...
     */
    public abstract void init();

    /**
     * Runs the activity.
     *
     * @return true if the activity had been stopped, false it is ended "properly"
     */
    public abstract boolean run();

    /**
     * Stops properly the activity.
     * For example, to save data, ...
     */
    public abstract void stop();

    /**
     * Sends an event to say a text.
     * @param text the text to say
     */
    public void say(String text) {
        System.out.println(Thread.currentThread().getName() + " want to say : " + text);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(text);
        RobotEventBus.getInstance().publish(paroleEvent);
    }

}
