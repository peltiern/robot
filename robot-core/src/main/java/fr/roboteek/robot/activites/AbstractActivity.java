package fr.roboteek.robot.activites;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;


/**
 * Abstract class for an activity.
 * <p>
 * Les activités sont des beans Spring singletons : leurs {@code @EventListener} sont
 * câblés en permanence, mais ne traitent les évènements que lorsque l'activité est
 * <b>active</b> (activée/désactivée par le Cerveau via {@link #activer()} /
 * {@link #desactiver()} — remplace l'ancien abonnement dynamique au bus Guava).
 */
public abstract class AbstractActivity {

    /**
     * Flag to indicate that the activity is stopped.
     */
    protected volatile boolean stopActivity;

    protected volatile boolean initialized;

    /**
     * Flag indiquant que l'activité est l'activité courante du Cerveau.
     */
    private volatile boolean active;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Publication des évènements du système nerveux (injecté par Spring).
     */
    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    /**
     * Initializes the activity before event listeners activation.
     * For example, to load data, initialize values, ...
     */
    public abstract void init();

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Active l'activité : réinitialise les flags, appelle {@link #init()} puis
     * autorise le traitement des évènements.
     */
    public final void activer() {
        stopActivity = false;
        init();
        active = true;
    }

    /**
     * Désactive l'activité : les évènements ne sont plus traités, puis {@link #stop()}.
     */
    public final void desactiver() {
        active = false;
        stop();
    }

    public boolean isActive() {
        return active;
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
     *
     * @param text the text to say
     */
    public void say(String text) {
        logger.debug("say : {}", text);
        final ParoleEvent paroleEvent = new ParoleEvent();
        paroleEvent.setTexte(text);
        applicationEventPublisher.publishEvent(paroleEvent);
    }

    public void playAnimation(Animation animation) {
        PlayAnimationEvent playAnimationEvent = new PlayAnimationEvent();
        playAnimationEvent.setAnimation(animation);
        applicationEventPublisher.publishEvent(playAnimationEvent);
    }

}
