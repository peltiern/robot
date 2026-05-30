package fr.roboteek.robot.organes.actionneurs.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Animation robotique décrite par tracks indépendants de keyframes.
 * Chaque track correspond à un axe moteur (oeil gauche, cou haut/bas, etc.).
 */
public class Animation {

    /** Nom réservé pour le mode aléatoire. */
    public static final String RANDOM_NAME = "RANDOM";

    private String name;
    private long totalDuration;
    private List<Track> tracks = new ArrayList<>();

    public Animation() {}

    public Animation(String name, long totalDuration) {
        this.name = name;
        this.totalDuration = totalDuration;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }

    /** Retourne le track correspondant à l'identifiant donné, s'il existe. */
    public Optional<Track> getTrack(TrackId id) {
        return tracks.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    /** Retourne tous les instants (ms) où au moins une keyframe est définie, triés. */
    public Set<Long> getAllKeyframeTimes() {
        return tracks.stream()
                .flatMap(t -> t.getKeyframes().stream())
                .map(Keyframe::getTime)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Indique si cette animation est le mode aléatoire. */
    public boolean isRandom() {
        return RANDOM_NAME.equals(name);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getTotalDuration() { return totalDuration; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }

    public List<Track> getTracks() { return tracks; }
    public void setTracks(List<Track> tracks) { this.tracks = tracks; }

    @Override
    public String toString() {
        return "Animation{name='" + name + "', duration=" + totalDuration + "ms, tracks=" + tracks.size() + "}";
    }
}
