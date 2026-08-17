package fr.roboteek.robot.web.controller;

import fr.roboteek.robot.configuration.Configurations;
import fr.roboteek.robot.configuration.RobotConfig;
import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.capteurs.CapteurMateriel;
import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import fr.roboteek.robot.securite.RegistreSante;
import fr.roboteek.robot.securite.SanteOrgane;
import fr.roboteek.robot.web.controller.dto.Articulation;
import fr.roboteek.robot.web.controller.dto.Mesure;
import fr.roboteek.robot.web.controller.dto.Organe;
import fr.roboteek.robot.web.controller.dto.Orientation;
import fr.roboteek.robot.web.controller.dto.Sante;
import fr.roboteek.robot.web.controller.dto.TypeOrgane;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final CapteurMateriel capteurMateriel;
    private final RegistreSante registreSante;
    private final PhidgetsConfig phidgetsConfig = Configurations.phidgetsConfig();
    private final RobotConfig robotConfig = Configurations.robotConfig();

    public OrganeController(Cou cou, Yeux yeux, CapteurMateriel capteurMateriel, RegistreSante registreSante) {
        this.cou = cou;
        this.yeux = yeux;
        this.capteurMateriel = capteurMateriel;
        this.registreSante = registreSante;
    }

    /**
     * Liste tous les organes du robot : actionneurs (avec leurs articulations), capteurs (avec
     * leurs mesures), et — depuis le watchdog — les organes qui n'exposent aucune capacité
     * mais dont on veut connaître l'état vital (chenilles gauche et droite, manette, lecteur
     * d'animations).
     * <p>
     * Chaque organe porte sa {@link Sante}, calculée avec le <b>même</b> délai de silence que celui
     * dont se sert le watchdog pour couper les moteurs : ce que montre l'interface est
     * exactement ce sur quoi il raisonne.
     */
    @GetMapping
    public List<Organe> lister() {
        Map<String, SanteOrgane> sante = releveSante();
        List<Organe> organes = new ArrayList<>(List.of(
                organeYeux(sante), organeCou(sante), organeMateriel(sante)));
        // Les organes surveillés qui ne sont pas décrits ci-dessus n'ont ni articulation ni mesure
        // à offrir : ils n'apparaissent que pour leur santé, et les clients qui bouclent sur les
        // capacités les ignorent d'eux-mêmes.
        sante.values().stream()
                .filter(etat -> organes.stream().noneMatch(organe -> organe.id().equals(etat.id())))
                .map(OrganeController::organeSansCapacite)
                .forEach(organes::add);
        return organes;
    }

    /** Santé de chaque organe surveillé, indexée par identifiant. */
    private Map<String, SanteOrgane> releveSante() {
        long delaiMillis = (long) (robotConfig.watchDogSilenceSecondes() * 1000);
        Map<String, SanteOrgane> parId = new LinkedHashMap<>();
        for (SanteOrgane etat : registreSante.releve(delaiMillis)) {
            parId.put(etat.id(), etat);
        }
        return parId;
    }

    /**
     * Organe présent uniquement pour son état vital. Le type vient de l'organe lui-même : le
     * déduire de {@code surveille} rangeait la manette parmi les actionneurs, alors qu'elle
     * observe l'opérateur sans rien bouger (cf. {@code NatureOrgane}).
     */
    private static Organe organeSansCapacite(SanteOrgane etat) {
        TypeOrgane type = switch (etat.nature()) {
            case ACTIONNEUR -> TypeOrgane.ACTIONNEUR;
            case CAPTEUR -> TypeOrgane.CAPTEUR;
        };
        return new Organe(etat.id(), etat.libelle(), type, List.of(), List.of(), toSante(etat));
    }

    /** Traduction du relevé interne en DTO, {@code null} si l'organe n'est pas surveillé. */
    private static Sante toSante(SanteOrgane etat) {
        return etat == null ? null : new Sante(etat.etat(), etat.ageMillis(), etat.surveille());
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

    private Organe organeYeux(Map<String, SanteOrgane> sante) {
        double min = phidgetsConfig.eyeMotorRelativePositionMin();
        double max = phidgetsConfig.eyeMotorRelativePositionMax();
        return new Organe("yeux", "Yeux", TypeOrgane.ACTIONNEUR, List.of(
                new Articulation("oeilGauche", "Œil gauche", UNITE_DEGRE, min, max,
                        Orientation.VERTICAL, yeux.getPositionOeilGaucheCourante()),
                new Articulation("oeilDroit", "Œil droit", UNITE_DEGRE, min, max,
                        Orientation.VERTICAL, yeux.getPositionOeilDroitCourante())
        ), List.of(), toSante(sante.get("yeux")));
    }

    private Organe organeCou(Map<String, SanteOrgane> sante) {
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
        ), List.of(), toSante(sante.get("cou")));
    }

    /**
     * Organe capteur « matériel » (CPU, mémoire, température, disque de la machine hôte, via
     * OSHI). Mesures en pourcentage bornées 0-100 (y compris la température, échelle indicative
     * pour le rendu — un CPU peut légitimement dépasser 100 °C en cas de dérive, l'échelle du
     * graphique n'a pas à s'y adapter dynamiquement).
     */
    private Organe organeMateriel(Map<String, SanteOrgane> sante) {
        return new Organe("materiel", "Matériel", TypeOrgane.CAPTEUR, List.of(), List.of(
                new Mesure("cpuCharge", "Charge CPU", "%", 0, 100, capteurMateriel.getChargeCpuPourcent()),
                new Mesure("memoire", "Mémoire", "%", 0, 100, capteurMateriel.getMemoireUtiliseePourcent()),
                new Mesure("temperatureCpu", "Température CPU", "°C", 0, 100, capteurMateriel.getTemperatureCpu()),
                new Mesure("disque", "Disque", "%", 0, 100, capteurMateriel.getDisqueUtilisePourcent())
        ), toSante(sante.get("materiel")));
    }
}
