package fr.roboteek.robot.organes.actionneurs.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Track d'animation pour un axe moteur.
 * Contient une liste de keyframes et les valeurs par défaut de vitesse/accélération.
 */
public class Track {

    private TrackId id;
    private double defaultVelocity;
    private double defaultAcceleration;
    private List<Keyframe> keyframes = new ArrayList<>();

    public Track() {}

    public Track(TrackId id, double defaultVelocity, double defaultAcceleration) {
        this.id = id;
        this.defaultVelocity = defaultVelocity;
        this.defaultAcceleration = defaultAcceleration;
    }

    public void addKeyframe(Keyframe kf) {
        keyframes.add(kf);
    }

    /** Retourne les keyframes triées par temps croissant. */
    public List<Keyframe> getSortedKeyframes() {
        return keyframes.stream()
                .sorted(Comparator.comparingLong(Keyframe::getTime))
                .collect(Collectors.toList());
    }

    /** Retourne la keyframe à l'instant exact donné, si elle existe. */
    public Optional<Keyframe> getKeyframeAt(long time) {
        return keyframes.stream()
                .filter(k -> k.getTime() == time)
                .findFirst();
    }

    /** Résout la vitesse effective : celle de la keyframe si définie, sinon le défaut du track. */
    public double resolveVelocity(Keyframe kf) {
        return kf.getVelocity() != null ? kf.getVelocity() : defaultVelocity;
    }

    /** Résout l'accélération effective : celle de la keyframe si définie, sinon le défaut du track. */
    public double resolveAcceleration(Keyframe kf) {
        return kf.getAcceleration() != null ? kf.getAcceleration() : defaultAcceleration;
    }

    public TrackId getId() { return id; }
    public void setId(TrackId id) { this.id = id; }

    public double getDefaultVelocity() { return defaultVelocity; }
    public void setDefaultVelocity(double defaultVelocity) { this.defaultVelocity = defaultVelocity; }

    public double getDefaultAcceleration() { return defaultAcceleration; }
    public void setDefaultAcceleration(double defaultAcceleration) { this.defaultAcceleration = defaultAcceleration; }

    public List<Keyframe> getKeyframes() { return keyframes; }
    public void setKeyframes(List<Keyframe> keyframes) { this.keyframes = keyframes; }

    @Override
    public String toString() {
        return "Track{id=" + id + ", keyframes=" + keyframes.size() + "}";
    }
}
