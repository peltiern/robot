package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.organes.actionneurs.animation.LimitesMoteur;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.web.controller.dto.AxeAnimable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Ressource REST « axes animables » : de quoi l'éditeur construit sa timeline.
 * <p>
 * Un axe absent de cette liste n'est pas réglé sur ce robot — le monter/descendre est le seul
 * dans ce cas, et il n'a pas de valeur par défaut dans la configuration. L'éditeur ne doit pas
 * proposer d'animer ce qu'aucun servo ne suivrait.
 */
@RestController
@RequestMapping("/api/axes-animables")
@CrossOrigin(origins = "*")
public class AxeAnimableController {

    @GetMapping
    public List<AxeAnimable> axes() {
        Map<Axe, LimitesMoteur> limites = LimitesMoteur.parAxe(Configurations.phidgetsConfig());
        return java.util.Arrays.stream(Axe.values())
                .filter(limites::containsKey)
                .map(axe -> {
                    LimitesMoteur limite = limites.get(axe);
                    return new AxeAnimable(axe.name(), axe.libelle(),
                            limite.positionMin(), limite.positionMax(),
                            limite.vitesseMax(), limite.accelerationMax());
                })
                .toList();
    }
}
