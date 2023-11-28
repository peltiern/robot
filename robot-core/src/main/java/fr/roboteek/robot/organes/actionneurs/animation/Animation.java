package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.RobotSound;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Animation {

    // Specific animation to generate random animations
    public static Animation RANDOM;

    public static Animation NEUTRAL;
    public static Animation THINKING;
    public static Animation SAD;
    public static Animation SURPRISED;
    public static Animation AMAZED;
    public static Animation TEST;
    public static Animation TEST_2;
    private static Map<String, Animation> mapAnimationsByName;

    static {
        RANDOM = new Animation();

        NEUTRAL = new Animation();
        NEUTRAL.addAnimationStep(new AnimationStep(200, 0, 0, 0, 0, null));

        THINKING = new Animation();
        THINKING.addAnimationStep(new AnimationStep(200, -10, 0, 20, 20, null));

        SAD = new Animation();
        SAD.addAnimationStep(new AnimationStep(100, -24, -24, 0, 0, RobotSound.SAD));

        SURPRISED = new Animation();
        SURPRISED.addAnimationStep(new AnimationStep(100, 0, -20, MouvementCouEvent.POSITION_NEUTRE, MouvementCouEvent.POSITION_NEUTRE, RobotSound.WOW));

        AMAZED = new Animation();
        AMAZED.addAnimationStep(new AnimationStep(100, -3, -3, MouvementCouEvent.POSITION_NEUTRE, 20, RobotSound.OH));
        AMAZED.addAnimationStep(new AnimationStep(500, MouvementYeuxEvent.POSITION_NEUTRE, MouvementYeuxEvent.POSITION_NEUTRE, MouvementCouEvent.POSITION_NEUTRE, 0, null));

        TEST = new Animation();
        TEST.addAnimationStep(new AnimationStep(2000, -15, -15, 0, 0, null));
        TEST.addAnimationStep(new AnimationStep(2000, 15, MouvementCouEvent.POSITION_NEUTRE, MouvementCouEvent.POSITION_NEUTRE, null));
        TEST.addAnimationStep(new AnimationStep(2000, -15, MouvementCouEvent.POSITION_NEUTRE, MouvementCouEvent.POSITION_NEUTRE, null));

        TEST_2 = new Animation();
        TEST_2.addAnimationStep(new AnimationStep(100, -15, -15, 0, 0, null));
        TEST_2.addAnimationStep(new AnimationStep(2000, -24, -6, 0, 0, null));
        TEST_2.addAnimationStep(new AnimationStep(2000, -6, -24, 0, 0, null));
        TEST_2.addAnimationStep(new AnimationStep(2000, -15, -15, 0, 0, null));

        mapAnimationsByName = new HashMap<>();
        mapAnimationsByName.put("NEUTRAL", NEUTRAL);
        mapAnimationsByName.put("THINKING", THINKING);
        mapAnimationsByName.put("SAD", SAD);
        mapAnimationsByName.put("SURPRISED", SURPRISED);
        mapAnimationsByName.put("AMAZED", AMAZED);
        mapAnimationsByName.put("TEST", TEST);
        mapAnimationsByName.put("TEST_2", TEST_2);
    }

    /**
     * Liste des étapes de l'animation.
     */
    private List<AnimationStep> steps = new ArrayList<>();

    public Animation() {
    }

    public Animation(List<AnimationStep> steps) {
        this.steps = steps;
    }

    public void addAnimationStep(AnimationStep animationStep) {
        steps.add(animationStep);
    }

    public List<AnimationStep> getSteps() {
        return steps;
    }

    public void setSteps(List<AnimationStep> steps) {
        this.steps = steps;
    }

    public static Animation getAnimationByName(String animationName) {
        if (StringUtils.isBlank(animationName)) {
            return null;
        }
        return mapAnimationsByName.get(animationName);
    }
}
