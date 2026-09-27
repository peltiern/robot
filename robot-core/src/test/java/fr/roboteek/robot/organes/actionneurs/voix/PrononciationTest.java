package fr.roboteek.robot.organes.actionneurs.voix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ce que Piper lit à la place de ce qui est écrit. */
class PrononciationTest {

    @Test
    void lesEcrituresDuNomDeviennentWally() {
        assertEquals("Bonjour ! Je suis Wally.", Prononciation.pourPiper("Bonjour ! Je suis Wall-E."));
        assertEquals("Wally, Wally, Wally et Wally", Prononciation.pourPiper("WALL-E, Wall E, walle et Wall-e"));
    }

    /** Un mot qui commence pareil n'est pas le nom du robot. */
    @Test
    void lesAutresMotsNeBougentPas() {
        assertEquals("un Wallon près du wall et de Walles", Prononciation.pourPiper("un Wallon près du wall et de Walles"));
    }
}
