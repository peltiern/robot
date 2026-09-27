package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.SOURCE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Qui a mis l'écoute en pause, et depuis quand : elle ne reprend que lorsque plus personne ne la
 * retient.
 * <p>
 * Il y avait un seul interrupteur, et deux mains dessus. La parole met l'écoute en pause et la relance
 * en fin de phrase ; le lecteur de sons en fait autant autour d'un son. Relevé sur le robot le
 * 2026-09-27 : quand une réaction émotionnelle joue sa bande-son avant la phrase, la fin de la
 * bande-son rallumait l'écoute <b>en pleine phrase</b>, et le robot entendait — et répondait à — ce
 * qui se disait pendant qu'il parlait.
 * <p>
 * Une source ne compte qu'une fois, sans compteur : la conversation met en pause pour la réflexion,
 * la phrase remet la même pause, et une seule reprise en fin de phrase libère les deux — c'est le
 * protocole d'avant, qu'un compteur aurait cassé en laissant l'écoute bloquée.
 * <p>
 * Et une pause ne peut pas durer toujours : une reprise perdue (processus mort, exception) ferait
 * un robot sourd pour de bon. Passé le délai maximum, la pause tombe d'elle-même, avec un
 * avertissement.
 */
public class PausesDeLEcoute {

    private static final Logger logger = LoggerFactory.getLogger(PausesDeLEcoute.class);

    private final Map<SOURCE, Instant> pauses = new ConcurrentHashMap<>();
    private final Clock horloge;
    private final Supplier<Duration> dureeMaximale;

    public PausesDeLEcoute(Clock horloge, Supplier<Duration> dureeMaximale) {
        this.horloge = horloge;
        this.dureeMaximale = dureeMaximale;
    }

    public void mettreEnPause(SOURCE source) {
        pauses.put(source, horloge.instant());
    }

    public void reprendre(SOURCE source) {
        pauses.remove(source);
    }

    /** Vrai tant qu'une source retient l'écoute ; les pauses trop vieilles tombent au passage. */
    public boolean enPause() {
        if (pauses.isEmpty()) {
            return false;
        }
        Instant limite = horloge.instant().minus(dureeMaximale.get());
        pauses.entrySet().removeIf(pause -> {
            boolean expiree = pause.getValue().isBefore(limite);
            if (expiree) {
                logger.warn("Pause de l'écoute par {} jamais levée : levée d'office au bout de {} s",
                        pause.getKey(), dureeMaximale.get().toSeconds());
            }
            return expiree;
        });
        return !pauses.isEmpty();
    }
}
