package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Construit la map des contraintes moteur depuis la configuration Phidgets.
 *
 * Les positions dans PhidgetsConfig sont des positions moteur absolues.
 * Les animations utilisent des positions relatives (degrés depuis la position zéro).
 * Conversion : relativeMin = initialPosition - motorMaxPosition
 *              relativeMax = initialPosition - motorMinPosition
 */
public class MotorConstraintsFactory {

    private static final Logger logger = LoggerFactory.getLogger(MotorConstraintsFactory.class);

    private MotorConstraintsFactory() {}

    public static Map<TrackId, MotorConstraints> fromConfig(PhidgetsConfig config) {
        Map<TrackId, MotorConstraints> map = new EnumMap<>(TrackId.class);

        map.put(TrackId.OEIL_GAUCHE, new MotorConstraints(
                config.eyeLeftMotorSpeed(),
                config.eyeLeftMotorAcceleration(),
                config.eyeMotorRelativePositionMin(),
                config.eyeMotorRelativePositionMax()
        ));

        map.put(TrackId.OEIL_DROIT, new MotorConstraints(
                config.eyeRightMotorSpeed(),
                config.eyeRightMotorAcceleration(),
                config.eyeMotorRelativePositionMin(),
                config.eyeMotorRelativePositionMax()
        ));

        // Cou panoramique : positionMoteur = initialPosition - positionRelative
        double panRelMin = config.neckLeftRightMotorInitialPosition() - config.neckLeftRightMotorMaxPosition();
        double panRelMax = config.neckLeftRightMotorInitialPosition() - config.neckLeftRightMotorMinPosition();
        map.put(TrackId.COU_GAUCHE_DROITE, new MotorConstraints(
                config.neckLeftRightMotorSpeed(),
                config.neckLeftRightMotorAcceleration(),
                panRelMin, panRelMax
        ));

        // Cou inclinaison
        double tiltRelMin = config.neckTiltMotorInitialPosition() - config.neckTiltMotorMaxPosition();
        double tiltRelMax = config.neckTiltMotorInitialPosition() - config.neckTiltMotorMinPosition();
        map.put(TrackId.COU_HAUT_BAS, new MotorConstraints(
                config.neckTiltMotorSpeed(),
                config.neckTiltMotorAcceleration(),
                tiltRelMin, tiltRelMax
        ));

        // Cou monter/descendre : pas de @DefaultValue dans la config, peut ne pas être configuré
        try {
            double upDownRelMin = config.neckUpDownMotorInitialPosition() - config.neckUpDownMotorMaxPosition();
            double upDownRelMax = config.neckUpDownMotorInitialPosition() - config.neckUpDownMotorMinPosition();
            map.put(TrackId.COU_MONTER_DESCENDRE, new MotorConstraints(
                    config.neckUpDownMotorSpeed(),
                    config.neckUpDownMotorAcceleration(),
                    upDownRelMin, upDownRelMax
            ));
        } catch (Exception e) {
            logger.warn("Contraintes COU_MONTER_DESCENDRE non disponibles (moteur non configuré) : {}", e.getMessage());
        }

        return Collections.unmodifiableMap(map);
    }
}
