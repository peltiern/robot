package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.organes.actionneurs.animation.Animation;

/**
 * Commande WebSocket pour déplacer le playhead de l'éditeur d'animation.
 * Le robot se déplace en temps réel à la position interpolée.
 */
public class AnimationScrubCommand {

    /** Animation en cours d'édition. */
    private Animation animation;

    /** Position du playhead en millisecondes. */
    private long time;

    public AnimationScrubCommand() {}

    public Animation getAnimation() { return animation; }
    public void setAnimation(Animation animation) { this.animation = animation; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
}
