package fr.roboteek.robot.activites;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;
import fr.roboteek.robot.systemenerveux.event.ParoleEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ClassUtils;


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
     * Identifiant de l'activité : le nom simple de sa classe. Sert à la désigner dans un
     * {@code DemandeActiviteEvent} et dans les logs.
     * <p>
     * {@link ClassUtils#getUserClass} et non {@code getClass()} : les activités sont proxiées
     * par CGLIB (@Async) et la classe du proxy s'appelle « ConversationActivity$$SpringCGLIB$$0 ».
     */
    public String identifiant() {
        return ClassUtils.getUserClass(this).getSimpleName();
    }

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
     * <p>
     * Ne pas rendre {@code final} : les activités sont proxiées par CGLIB (@Async) et une
     * méthode finale s'exécuterait sur le proxy, dont les champs sont distincts de la cible —
     * le flag {@code active} ne serait alors jamais vu par les listeners.
     */
    public void activer() {
        stopActivity = false;
        init();
        active = true;
    }

    /**
     * Désactive l'activité : les évènements ne sont plus traités, puis {@link #stop()}.
     * Ne pas rendre {@code final} (voir {@link #activer()}).
     */
    public void desactiver() {
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
