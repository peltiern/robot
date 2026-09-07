package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.web.controller.dto.ReglagesRegard;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ressource REST « réglages du regard » : de quoi le HUD dessine la zone morte sur l'image.
 * <p>
 * Les valeurs sont relues à chaque appel, et non figées au démarrage : ces clés sont rechargées à
 * chaud, et un HUD qui montrerait le réglage d'il y a une heure serait pire que pas de repère du
 * tout — on croirait mesurer alors qu'on regarde un souvenir.
 * <p>
 * <b>Le rayon se calcule côté client</b>, à partir du champ et de la largeur de l'image reçue. Le
 * robot ne le connaît pas : c'est {@code Regard} qui décide de la focale, image par image, depuis la
 * largeur que lui donne l'évènement de vision.
 */
@RestController
@RequestMapping("/api/regard")
@CrossOrigin(origins = "*")
public class RegardController {

    @GetMapping
    public ReglagesRegard reglages() {
        var configuration = Configurations.robotConfig();
        return new ReglagesRegard(configuration.regardEnabled(),
                configuration.zoneMorteRegardDegres(),
                configuration.champHorizontalCameraDegres(),
                configuration.centreOptiqueXRelatif(),
                configuration.centreOptiqueYRelatif());
    }
}
