package fr.roboteek.robot.organes.actionneurs.animation;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.RobotSound;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlayAnimationEvent;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.systemenerveux.event.StopAnimationEvent;
import fr.roboteek.robot.util.commons.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AnimationPlayer extends AbstractOrgane {

    private static final Logger logger = LoggerFactory.getLogger(AnimationPlayer.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "AnimationPlayer"));

    private final List<ScheduledFuture<?>> currentTasks = new CopyOnWriteArrayList<>();

    private volatile boolean automaticMode = false;
    private volatile ScheduledFuture<?> randomTask = null;

    private final AnimationValidator validator = new AnimationValidator(
            MotorConstraintsFactory.fromConfig(Configurations.phidgetsConfig()));

    private final AnimationRepository repository = new AnimationRepository(Constantes.DOSSIER_ANIMATIONS);

    @Override
    public void initialiser() {
        logger.info("AnimationPlayer initialisé");
    }

    @Override
    public void arreter() {
        cancelCurrentAnimation();
        scheduler.shutdown();
        logger.info("AnimationPlayer arrêté");
    }

    @Subscribe
    public void handleStopAnimationEvent(StopAnimationEvent event) {
        logger.debug("Arrêt de l'animation en cours");
        cancelCurrentAnimation();
    }

    @Subscribe
    public void handlePlayAnimationEvent(PlayAnimationEvent event) {
        if (event == null) return;

        Animation animation = resolveAnimation(event);
        if (animation == null) {
            logger.warn("Animation introuvable : {}", event.getAnimationName());
            return;
        }

        cancelCurrentAnimation();

        if (animation.isRandom()) {
            logger.debug("Mode aléatoire activé");
            automaticMode = true;
            scheduleNextRandom();
        } else {
            logger.debug("Lecture de l'animation '{}'", animation.getName());
            validateAndWarn(animation);
            scheduleAnimation(animation);
        }
    }

    // ── Scheduling ────────────────────────────────────────────────────────────

    private void scheduleAnimation(Animation animation) {
        Set<Long> times = animation.getAllKeyframeTimes();
        for (Long time : times) {
            ScheduledFuture<?> task = scheduler.schedule(
                    () -> playAtTime(animation, time),
                    time, TimeUnit.MILLISECONDS);
            currentTasks.add(task);
        }
    }

    private void cancelCurrentAnimation() {
        automaticMode = false;
        if (randomTask != null) {
            randomTask.cancel(false);
            randomTask = null;
        }
        currentTasks.forEach(t -> t.cancel(false));
        currentTasks.clear();
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    private void playAtTime(Animation animation, long time) {
        MouvementYeuxEvent yeuxEvent = buildYeuxEvent(animation, time);
        MouvementCouEvent couEvent = buildCouEvent(animation, time);

        if (hasYeuxCommands(yeuxEvent)) {
            RobotEventBus.getInstance().publishAsync(yeuxEvent);
        }
        if (hasCouCommands(couEvent)) {
            RobotEventBus.getInstance().publishAsync(couEvent);
        }
    }

    private MouvementYeuxEvent buildYeuxEvent(Animation animation, long time) {
        MouvementYeuxEvent event = new MouvementYeuxEvent();
        applyEyeTrack(animation, time, TrackId.OEIL_GAUCHE, event, true);
        applyEyeTrack(animation, time, TrackId.OEIL_DROIT, event, false);
        return event;
    }

    private void applyEyeTrack(Animation animation, long time, TrackId trackId,
                                MouvementYeuxEvent event, boolean gauche) {
        animation.getTrack(trackId)
                .flatMap(t -> t.getKeyframeAt(time).map(kf -> new Object[]{t, kf}))
                .ifPresent(pair -> {
                    Track track = (Track) pair[0];
                    Keyframe kf = (Keyframe) pair[1];
                    if (gauche) {
                        event.setPositionOeilGauche(kf.getValue());
                        event.setVitesseOeilGauche(track.resolveVelocity(kf));
                        event.setAccelerationOeilGauche(track.resolveAcceleration(kf));
                    } else {
                        event.setPositionOeilDroit(kf.getValue());
                        event.setVitesseOeilDroit(track.resolveVelocity(kf));
                        event.setAccelerationOeilDroit(track.resolveAcceleration(kf));
                    }
                });
    }

    private MouvementCouEvent buildCouEvent(Animation animation, long time) {
        MouvementCouEvent event = new MouvementCouEvent();
        animation.getTrack(TrackId.COU_GAUCHE_DROITE)
                .flatMap(t -> t.getKeyframeAt(time).map(kf -> new Object[]{t, kf}))
                .ifPresent(pair -> {
                    Track t = (Track) pair[0]; Keyframe kf = (Keyframe) pair[1];
                    event.setPositionPanoramique(kf.getValue());
                    event.setVitessePanoramique(t.resolveVelocity(kf));
                    event.setAccelerationPanoramique(t.resolveAcceleration(kf));
                });
        animation.getTrack(TrackId.COU_HAUT_BAS)
                .flatMap(t -> t.getKeyframeAt(time).map(kf -> new Object[]{t, kf}))
                .ifPresent(pair -> {
                    Track t = (Track) pair[0]; Keyframe kf = (Keyframe) pair[1];
                    event.setPositionInclinaison(kf.getValue());
                    event.setVitesseInclinaison(t.resolveVelocity(kf));
                    event.setAccelerationInclinaison(t.resolveAcceleration(kf));
                });
        animation.getTrack(TrackId.COU_MONTER_DESCENDRE)
                .flatMap(t -> t.getKeyframeAt(time).map(kf -> new Object[]{t, kf}))
                .ifPresent(pair -> {
                    Track t = (Track) pair[0]; Keyframe kf = (Keyframe) pair[1];
                    event.setPositionMonterDescendre(kf.getValue());
                    event.setVitesseMonterDescendre(t.resolveVelocity(kf));
                    event.setAccelerationMonterDescendre(t.resolveAcceleration(kf));
                });
        return event;
    }

    private boolean hasYeuxCommands(MouvementYeuxEvent event) {
        return event.getPositionOeilGauche() != null
                || event.getPositionOeilDroit() != null;
    }

    private boolean hasCouCommands(MouvementCouEvent event) {
        return event.getPositionPanoramique() != null
                || event.getPositionInclinaison() != null
                || event.getPositionMonterDescendre() != null;
    }

    // ── Mode aléatoire ────────────────────────────────────────────────────────

    private void scheduleNextRandom() {
        if (!automaticMode) return;
        long delay = RandomUtils.nextLong(500, 3000);
        randomTask = scheduler.schedule(() -> {
            playRandomStep();
            scheduleNextRandom();
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void playRandomStep() {
        MouvementYeuxEvent yeuxEvent = new MouvementYeuxEvent();
        MouvementCouEvent couEvent = new MouvementCouEvent();

        if (RandomUtils.nextInt(0, 3) % 3 == 0) {
            double pos = RandomUtils.nextDouble(Configurations.phidgetsConfig().eyeMotorRelativePositionMin(), 0);
            if (RandomUtils.nextBoolean()) {
                yeuxEvent.setPositionOeilGauche(pos);
                yeuxEvent.setPositionOeilDroit(pos);
            } else if (RandomUtils.nextBoolean()) {
                yeuxEvent.setPositionOeilGauche(0.0);
                yeuxEvent.setPositionOeilDroit(pos);
            } else {
                yeuxEvent.setPositionOeilGauche(pos);
                yeuxEvent.setPositionOeilDroit(0.0);
            }
        }

        if (RandomUtils.nextInt(0, 2) % 2 == 0) {
            couEvent.setPositionPanoramique(RandomUtils.nextDouble(-40, 40));
        }
        if (RandomUtils.nextInt(0, 2) % 2 == 0) {
            couEvent.setPositionInclinaison(RandomUtils.nextDouble(-30, 30));
        }

        if (hasYeuxCommands(yeuxEvent)) RobotEventBus.getInstance().publishAsync(yeuxEvent);
        if (hasCouCommands(couEvent))   RobotEventBus.getInstance().publishAsync(couEvent);
    }

    // ── Résolution et validation ───────────────────────────────────────────────

    private Animation resolveAnimation(PlayAnimationEvent event) {
        if (event.getAnimation() != null) return event.getAnimation();
        if (event.getAnimationName() != null) {
            // Priorité : fichier JSON > presets hardcodés
            Optional<Animation> fromFile = repository.load(event.getAnimationName());
            if (fromFile.isPresent()) return fromFile.get();
            return AnimationPresets.getByName(event.getAnimationName());
        }
        return null;
    }

    private void validateAndWarn(Animation animation) {
        List<AnimationValidator.ValidationWarning> warnings = validator.validate(animation);
        for (AnimationValidator.ValidationWarning w : warnings) {
            logger.warn("Animation '{}' — track {} [{} → {}ms] : {}",
                    animation.getName(), w.trackId(), w.timeFrom(), w.timeTo(), w.message());
        }
    }
}
