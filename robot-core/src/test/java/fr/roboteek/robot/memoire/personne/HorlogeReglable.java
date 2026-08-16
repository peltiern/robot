package fr.roboteek.robot.memoire.personne;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Horloge que le test fait avancer à la main.
 * <p>
 * {@code Clock.fixed} ne suffit pas : tout ce qu'on veut vérifier ici, ce sont des durées —
 * présence confirmée, absence, temporisation. Dormir réellement rendrait la suite de tests
 * interminable et instable.
 */
class HorlogeReglable extends Clock {

    private Instant instant;

    HorlogeReglable(Instant debut) {
        this.instant = debut;
    }

    /** Fait avancer le temps. */
    void avancerDe(Duration duree) {
        instant = instant.plus(duree);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
