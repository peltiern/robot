package fr.roboteek.robot.activites;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;


/**
 * Abstract class for an activity.
 */
public abstract class AbstractActivity {

    /** Flag to indicate that the activity is stopped. */
    protected boolean stopActivity;

    protected boolean initialized;

    /**
     * Initializes the activity before event listeners activation.
     * For example, to load data, initialize values, ...
     */
    public abstract void init();

    public boolean isInitialized() {
        return initialized;
    }

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
    public void stop() {
        stopActivity = true;
    }

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

    public void playAnimation(Animation animation) {
        PlayAnimationEvent playAnimationEvent = new PlayAnimationEvent();
        playAnimationEvent.setAnimation(animation);
        RobotEventBus.getInstance().publishAsync(playAnimationEvent);
    }

}
