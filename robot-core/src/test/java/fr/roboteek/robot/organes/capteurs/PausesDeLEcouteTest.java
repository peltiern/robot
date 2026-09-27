package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.util.HorlogeReglable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.SOURCE.PAROLE;
import static fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent.SOURCE.SON;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Qui retient l'écoute, et quand elle reprend. */
class PausesDeLEcouteTest {

    private final HorlogeReglable horloge = new HorlogeReglable(Instant.parse("2026-09-27T16:00:00Z"));
    private final PausesDeLEcoute pauses = new PausesDeLEcoute(horloge, () -> Duration.ofSeconds(30));

    @Test
    void sansPauseLEcouteEstOuverte() {
        assertFalse(pauses.enPause());
    }

    /**
     * Le cas relevé sur le robot le 2026-09-27 : la bande-son d'une réaction finit pendant la
     * phrase. Sa reprise ne doit pas rallumer l'écoute tant que la phrase se dit.
     */
    @Test
    void laFinDUnSonNeRallumePasLEcoutePendantUnePhrase() {
        pauses.mettreEnPause(SON);
        pauses.mettreEnPause(PAROLE);

        pauses.reprendre(SON);
        assertTrue(pauses.enPause());

        pauses.reprendre(PAROLE);
        assertFalse(pauses.enPause());
    }

    /**
     * Le protocole de la conversation : pause à la réflexion, pause à la phrase, une seule reprise
     * en fin de phrase. Un compteur laisserait l'écoute bloquée ; une source ne compte qu'une fois.
     */
    @Test
    void uneMemeSourceNeCompteQuUneFois() {
        pauses.mettreEnPause(PAROLE);
        pauses.mettreEnPause(PAROLE);

        pauses.reprendre(PAROLE);
        assertFalse(pauses.enPause());
    }

    /** Une reprise perdue ne laisse pas le robot sourd pour de bon. */
    @Test
    void unePauseJamaisLeveeTombeDElleMeme() {
        pauses.mettreEnPause(SON);

        horloge.avancerDe(Duration.ofSeconds(30));
        assertTrue(pauses.enPause());

        horloge.avancerDe(Duration.ofSeconds(1));
        assertFalse(pauses.enPause());
    }

    /** Remettre en pause rafraîchit l'échéance : une phrase suivie d'une autre ne tombe pas en route. */
    @Test
    void remettreEnPauseRepartDeZero() {
        pauses.mettreEnPause(PAROLE);
        horloge.avancerDe(Duration.ofSeconds(25));
        pauses.mettreEnPause(PAROLE);
        horloge.avancerDe(Duration.ofSeconds(25));

        assertTrue(pauses.enPause());
    }
}
