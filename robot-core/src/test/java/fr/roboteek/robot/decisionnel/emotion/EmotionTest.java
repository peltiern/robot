package fr.roboteek.robot.decisionnel.emotion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** La relecture d'une émotion depuis sa clé. */
class EmotionTest {

    @Test
    void uneCleSeRelitEnEmotion() {
        assertEquals(Emotion.EMERVEILLEMENT, Emotion.depuisCle("emerveillement"));
    }

    /** Une émotion retirée de la liste un jour : l'animation qui la portait reste lisible, sans émotion. */
    @Test
    void uneCleInconnueNEstPasUneEmotion() {
        assertNull(Emotion.depuisCle("nostalgie"));
        assertNull(Emotion.depuisCle(null));
    }
}
