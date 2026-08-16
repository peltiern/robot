package fr.roboteek.robot.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Horloge que le test fait avancer à la main.
 * <p>
 * {@code Clock.fixed} ne suffit pas : ce qu'on veut vérifier, ce sont des durées — présence
 * confirmée, absence, temporisation de relance. Dormir réellement rendrait la suite de tests
 * interminable et instable.
 * <p>
 * Partagée par tout ce dont le comportement est affaire de temps : le registre de présence
 * comme l'arbitrage des activités.
 */
public class HorlogeReglable extends Clock {

    private Instant instant;

    public HorlogeReglable(Instant debut) {
        this.instant = debut;
    }

    /** Fait avancer le temps. */
    public void avancerDe(Duration duree) {
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
