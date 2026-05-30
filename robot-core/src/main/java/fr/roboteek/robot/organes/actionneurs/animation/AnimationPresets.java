package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.RobotSound;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Catalogue des animations prédéfinies du robot.
 * Chaque constante est une {@link Animation} décrite par tracks/keyframes.
 */
public class AnimationPresets {

    public static final Animation RANDOM;
    public static final Animation NEUTRAL;
    public static final Animation THINKING;
    public static final Animation SAD;
    public static final Animation SURPRISED;
    public static final Animation AMAZED;
    public static final Animation TEST;
    public static final Animation TEST_2;

    static {
        RANDOM = new Animation(Animation.RANDOM_NAME, 0);

        // NEUTRAL : tous les axes à 0° en 200ms
        NEUTRAL = buildSimple("NEUTRAL", 200,
                0, 0,
                0, 0,
                null);

        // THINKING : yeux baissés à gauche, cou levé
        THINKING = buildSimple("THINKING", 200,
                -10, 0,
                20, 20,
                null);

        // SAD : yeux très baissés, son triste
        SAD = buildSimple("SAD", 100,
                -24, -24,
                0, 0,
                RobotSound.SAD);

        // SURPRISED : oeil droit levé, son wow
        SURPRISED = new Animation("SURPRISED", 100);
        addEyeKeyframes(SURPRISED, 100, 0, -20);
        SURPRISED.getTrack(TrackId.OEIL_GAUCHE).ifPresent(t ->
                SURPRISED.addTrack(buildSoundTrack(100, RobotSound.WOW)));

        // AMAZED : 2 étapes
        AMAZED = new Animation("AMAZED", 600);
        Track ogAmazed = buildTrack(TrackId.OEIL_GAUCHE);
        ogAmazed.addKeyframe(new Keyframe(100, -3));
        ogAmazed.addKeyframe(new Keyframe(600, 0));
        AMAZED.addTrack(ogAmazed);
        Track odAmazed = buildTrack(TrackId.OEIL_DROIT);
        odAmazed.addKeyframe(new Keyframe(100, -3));
        odAmazed.addKeyframe(new Keyframe(600, 0));
        AMAZED.addTrack(odAmazed);
        Track couHbAmazed = buildTrack(TrackId.COU_HAUT_BAS);
        couHbAmazed.addKeyframe(new Keyframe(100, 20));
        couHbAmazed.addKeyframe(new Keyframe(600, 0));
        AMAZED.addTrack(couHbAmazed);

        // TEST : 3 étapes sur oeil gauche + cou
        TEST = new Animation("TEST", 6000);
        Track ogTest = buildTrack(TrackId.OEIL_GAUCHE);
        ogTest.addKeyframe(new Keyframe(2000, -15));
        ogTest.addKeyframe(new Keyframe(4000, 15));
        ogTest.addKeyframe(new Keyframe(6000, -15));
        TEST.addTrack(ogTest);
        Track odTest = buildTrack(TrackId.OEIL_DROIT);
        odTest.addKeyframe(new Keyframe(2000, -15));
        TEST.addTrack(odTest);
        Track couGdTest = buildTrack(TrackId.COU_GAUCHE_DROITE);
        couGdTest.addKeyframe(new Keyframe(2000, 0));
        TEST.addTrack(couGdTest);
        Track couHbTest = buildTrack(TrackId.COU_HAUT_BAS);
        couHbTest.addKeyframe(new Keyframe(2000, 0));
        TEST.addTrack(couHbTest);

        // TEST_2 : 4 étapes
        TEST_2 = new Animation("TEST_2", 6100);
        Track ogTest2 = buildTrack(TrackId.OEIL_GAUCHE);
        ogTest2.addKeyframe(new Keyframe(100, -15));
        ogTest2.addKeyframe(new Keyframe(2100, -24));
        ogTest2.addKeyframe(new Keyframe(4100, -6));
        ogTest2.addKeyframe(new Keyframe(6100, -15));
        TEST_2.addTrack(ogTest2);
        Track odTest2 = buildTrack(TrackId.OEIL_DROIT);
        odTest2.addKeyframe(new Keyframe(100, -15));
        odTest2.addKeyframe(new Keyframe(2100, -6));
        odTest2.addKeyframe(new Keyframe(4100, -24));
        odTest2.addKeyframe(new Keyframe(6100, -15));
        TEST_2.addTrack(odTest2);
        Track couGdTest2 = buildTrack(TrackId.COU_GAUCHE_DROITE);
        couGdTest2.addKeyframe(new Keyframe(100, 0));
        TEST_2.addTrack(couGdTest2);
        Track couHbTest2 = buildTrack(TrackId.COU_HAUT_BAS);
        couHbTest2.addKeyframe(new Keyframe(100, 0));
        TEST_2.addTrack(couHbTest2);
    }

    private AnimationPresets() {}

    public static Animation getByName(String name) {
        if (name == null) return null;
        return switch (name.toUpperCase()) {
            case "RANDOM"    -> RANDOM;
            case "NEUTRAL"   -> NEUTRAL;
            case "THINKING"  -> THINKING;
            case "SAD"       -> SAD;
            case "SURPRISED" -> SURPRISED;
            case "AMAZED"    -> AMAZED;
            case "TEST"      -> TEST;
            case "TEST_2"    -> TEST_2;
            default          -> null;
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Track buildTrack(TrackId id) {
        return switch (id) {
            case OEIL_GAUCHE, OEIL_DROIT ->
                    new Track(id, phidgetsConfig().eyeLeftMotorSpeed(), phidgetsConfig().eyeLeftMotorAcceleration());
            default ->
                    new Track(id, phidgetsConfig().neckLeftRightMotorSpeed(), phidgetsConfig().neckLeftRightMotorAcceleration());
        };
    }

    private static Animation buildSimple(String name, long time,
                                         double oeilG, double oeilD,
                                         double couGD, double couHB,
                                         RobotSound sound) {
        Animation anim = new Animation(name, time);
        addEyeKeyframes(anim, time, oeilG, oeilD);
        Track couGdTrack = buildTrack(TrackId.COU_GAUCHE_DROITE);
        couGdTrack.addKeyframe(new Keyframe(time, couGD));
        anim.addTrack(couGdTrack);
        Track couHbTrack = buildTrack(TrackId.COU_HAUT_BAS);
        couHbTrack.addKeyframe(new Keyframe(time, couHB));
        anim.addTrack(couHbTrack);
        return anim;
    }

    private static void addEyeKeyframes(Animation anim, long time, double oeilG, double oeilD) {
        Track ogTrack = buildTrack(TrackId.OEIL_GAUCHE);
        ogTrack.addKeyframe(new Keyframe(time, oeilG));
        anim.addTrack(ogTrack);
        Track odTrack = buildTrack(TrackId.OEIL_DROIT);
        odTrack.addKeyframe(new Keyframe(time, oeilD));
        anim.addTrack(odTrack);
    }

    private static Track buildSoundTrack(long time, RobotSound sound) {
        // Le son est géré séparément via PlaySoundEvent dans AnimationPlayer
        // Ce track est un placeholder non utilisé pour l'instant
        return buildTrack(TrackId.OEIL_GAUCHE);
    }
}
