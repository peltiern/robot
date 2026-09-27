package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.decisionnel.emotion.Emotion;
import fr.roboteek.robot.web.controller.dto.EmotionConnue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Les émotions que le robot connaît, telles que {@link Emotion} les définit : la liste ne vit qu'à
 * cet endroit. L'appli en garde une copie dans le navigateur pour s'en servir robot éteint.
 */
@RestController
@RequestMapping("/api/emotions")
@CrossOrigin(origins = "*")
public class EmotionController {

    @GetMapping
    public List<EmotionConnue> emotions() {
        return Arrays.stream(Emotion.values())
                .map(e -> new EmotionConnue(e.cle(), e.libelle(), e.emoji()))
                .toList();
    }
}
