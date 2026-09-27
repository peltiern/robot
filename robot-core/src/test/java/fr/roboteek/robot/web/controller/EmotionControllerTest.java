package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.web.controller.dto.EmotionConnue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La liste des émotions, telle que l'appli la reçoit. */
class EmotionControllerTest {

    @Test
    void chaqueEmotionArriveAvecSonNomEtSonEmoji() {
        List<EmotionConnue> emotions = new EmotionController().emotions();

        assertEquals(14, emotions.size());
        assertEquals(new EmotionConnue("neutre", "Neutre", "😐"), emotions.getFirst());
        assertEquals(new EmotionConnue("emerveillement", "Émerveillement", "🤩"), emotions.get(12));
    }
}
