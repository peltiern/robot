package fr.roboteek.robot.organes.actionneurs.voix;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Le découpage d'une réponse en morceaux à dire l'un après l'autre. */
class DecoupageEnPhrasesTest {

    /** Une réponse relevée sur le robot le 2026-09-27. */
    @Test
    void uneReponseSeCoupeAuxFinsDePhrase() {
        assertEquals(List.of("Oh non Nicolas !", "Qui est mort ? Je suis vraiment désolé !"),
                DecoupageEnPhrases.decouper("Oh non Nicolas ! Qui est mort ? Je suis vraiment désolé !"));
    }

    /** « Ça ? » seul paierait une relance de play pour presque rien : il rejoint la suite. */
    @Test
    void lesBoutsTropCourtsSontJoints() {
        assertEquals(List.of("Ça ? Juste ça ?", "Complète ta pensée, Nicolas !"),
                DecoupageEnPhrases.decouper("Ça ? Juste ça ? Complète ta pensée, Nicolas !"));
    }

    @Test
    void unResteTropCourtRejointLeMorceauPrecedent() {
        assertEquals(List.of("Je suis vraiment désolé pour toi. Oh."),
                DecoupageEnPhrases.decouper("Je suis vraiment désolé pour toi. Oh."));
    }

    @Test
    void unePhraseUniqueResteEntiere() {
        assertEquals(List.of("Au revoir."), DecoupageEnPhrases.decouper("Au revoir."));
    }

    /** Un nombre décimal ou une ponctuation collée ne sont pas des fins de phrase. */
    @Test
    void unPointSansBlancNeCoupePas() {
        assertEquals(List.of("La version 3.5 est sortie, Wall-E!Super !"),
                DecoupageEnPhrases.decouper("La version 3.5 est sortie, Wall-E!Super !"));
    }

    @Test
    void unTexteVideNeDonneRien() {
        assertEquals(List.of(), DecoupageEnPhrases.decouper("  "));
        assertEquals(List.of(), DecoupageEnPhrases.decouper(null));
    }
}
