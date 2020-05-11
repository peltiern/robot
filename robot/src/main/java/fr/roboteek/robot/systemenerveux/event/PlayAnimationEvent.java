package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;

/**
 * Evènement pour lire une animation.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class PlayAnimationEvent extends RobotEvent {

    public static final String EVENT_TYPE = "PlayAnimation";

    /**
     * Animation à jouer.
     */
    private Animation animation;

    public PlayAnimationEvent() {
        super(EVENT_TYPE);
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }
}
