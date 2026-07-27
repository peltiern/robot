package fr.roboteek.robot.spring.server.controller;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import fr.roboteek.robot.spring.server.controller.dto.Articulation;
import fr.roboteek.robot.spring.server.controller.dto.Organe;
import fr.roboteek.robot.spring.server.controller.dto.Orientation;
import fr.roboteek.robot.spring.server.controller.dto.TypeOrgane;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ressource REST « organes » : décrit les organes du robot et, pour les actionneurs, leurs
 * articulations pilotables (butées + position courante).
 * <p>
 * But : découverte de capacités. Un client (webapp) se construit à partir de cette description,
 * sans coder en dur ni les organes, ni les libellés, ni les butées : il boucle sur les organes
 * renvoyés, et pour chaque articulation affiche un contrôle borné par les <b>vraies</b> butées
 * moteur (source de vérité = {@link PhidgetsConfig}) positionné sur l'<b>état réel</b> du robot
 * (le robot a pu bouger avant l'ouverture de la page).
 * <p>
 * Repère « logique » commun aux butées et à la position, identique aux évènements de mouvement :
 * <ul>
 *   <li><b>Cou</b> : position = position d'init − position moteur ; la plage moteur
 *   {@code [min, max]} devient {@code [init − max, init − min]}.</li>
 *   <li><b>Yeux</b> : position relative (0 = horizontal), bornée par les butées relatives
 *   communes aux deux yeux.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/organes")
@CrossOrigin(origins = "*")
public class OrganeController {

    private static final String UNITE_DEGRE = "deg";

    private final Cou cou;
    private final Yeux yeux;
    private final PhidgetsConfig phidgetsConfig = Configurations.phidgetsConfig();

    public OrganeController(Cou cou, Yeux yeux) {
        this.cou = cou;
        this.yeux = yeux;
    }

    /**
     * Liste tous les organes du robot avec, pour les actionneurs, leurs articulations.
     */
    @GetMapping
    public List<Organe> lister() {
        return List.of(organeYeux(), organeCou());
    }

    /**
     * Renvoie un organe précis par son identifiant, ou 404 s'il est inconnu.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Organe> parId(@PathVariable String id) {
        return lister().stream()
                .filter(organe -> organe.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Organe organeYeux() {
        double min = phidgetsConfig.eyeMotorRelativePositionMin();
        double max = phidgetsConfig.eyeMotorRelativePositionMax();
        return new Organe("yeux", "Yeux", TypeOrgane.ACTIONNEUR, List.of(
                new Articulation("oeilGauche", "Œil gauche", UNITE_DEGRE, min, max,
                        Orientation.VERTICAL, yeux.getPositionOeilGaucheCourante()),
                new Articulation("oeilDroit", "Œil droit", UNITE_DEGRE, min, max,
                        Orientation.VERTICAL, yeux.getPositionOeilDroitCourante())
        ));
    }

    private Organe organeCou() {
        return new Organe("cou", "Cou", TypeOrgane.ACTIONNEUR, List.of(
                new Articulation("pan", "Panoramique (gauche / droite)", UNITE_DEGRE,
                        phidgetsConfig.neckLeftRightMotorInitialPosition() - phidgetsConfig.neckLeftRightMotorMaxPosition(),
                        phidgetsConfig.neckLeftRightMotorInitialPosition() - phidgetsConfig.neckLeftRightMotorMinPosition(),
                        Orientation.HORIZONTAL, cou.getPositionPanoramiqueCourante()),
                new Articulation("tilt", "Inclinaison (haut / bas)", UNITE_DEGRE,
                        phidgetsConfig.neckTiltMotorInitialPosition() - phidgetsConfig.neckTiltMotorMaxPosition(),
                        phidgetsConfig.neckTiltMotorInitialPosition() - phidgetsConfig.neckTiltMotorMinPosition(),
                        Orientation.VERTICAL, cou.getPositionInclinaisonCourante()),
                new Articulation("upDown", "Monter / descendre", UNITE_DEGRE,
                        phidgetsConfig.neckUpDownMotorInitialPosition() - phidgetsConfig.neckUpDownMotorMaxPosition(),
                        phidgetsConfig.neckUpDownMotorInitialPosition() - phidgetsConfig.neckUpDownMotorMinPosition(),
                        Orientation.VERTICAL, cou.getPositionMonterDescendreCourante())
        ));
    }
}
