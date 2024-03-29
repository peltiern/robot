package fr.roboteek.robot.systemenerveux.event;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;

/**
 * Evènement pour lire une animation.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class PlayAnimationEvent extends RobotEvent {

    public static final String EVENT_TYPE = "play-animation";

    /**
     * Animation à jouer.
     */
    private Animation animation;

    /**
     * Nom de l'animation à jouer.
     */
    private String animationName;

    public PlayAnimationEvent() {
        super(EVENT_TYPE);
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public String getAnimationName() {
        return animationName;
    }

    public void setAnimationName(String animationName) {
        this.animationName = animationName;
    }

    @Override
    public String toString() {
        return "PlayAnimationEvent{" +
                "animation=" + animation +
                ", animationName='" + animationName + '\'' +
                "} " + super.toString();
    }
}
