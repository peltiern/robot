package fr.roboteek.robot.organes.actionneurs.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Valide qu'une animation est physiquement jouable par les moteurs.
 *
 * Détecte :
 * - les positions hors des bornes physiques du moteur
 * - la vitesse demandée qui dépasse la capacité du moteur
 * - les segments où le moteur ne peut pas atteindre la position cible dans le temps imparti
 */
public class AnimationValidator {

    public record ValidationWarning(
            TrackId trackId,
            long timeFrom,
            long timeTo,
            String message) {}

    private final Map<TrackId, MotorConstraints> constraints;

    public AnimationValidator(Map<TrackId, MotorConstraints> constraints) {
        this.constraints = constraints;
    }

    /**
     * Valide toutes les transitions de chaque track de l'animation.
     *
     * @param animation l'animation à valider
     * @return liste des avertissements (vide si tout est OK)
     */
    public List<ValidationWarning> validate(Animation animation) {
        List<ValidationWarning> warnings = new ArrayList<>();

        for (Track track : animation.getTracks()) {
            MotorConstraints mc = constraints.get(track.getId());
            List<Keyframe> sorted = track.getSortedKeyframes();

            // Vérification des bornes de position pour chaque keyframe
            if (mc != null) {
                for (Keyframe kf : sorted) {
                    if (kf.getValue() < mc.getMinPosition() || kf.getValue() > mc.getMaxPosition()) {
                        warnings.add(new ValidationWarning(
                                track.getId(), kf.getTime(), kf.getTime(),
                                String.format("Position %.1f° hors des bornes physiques [%.1f°, %.1f°]",
                                        kf.getValue(), mc.getMinPosition(), mc.getMaxPosition())
                        ));
                    }
                }
            }

            // Vérification de chaque transition entre keyframes consécutives
            for (int i = 0; i < sorted.size() - 1; i++) {
                Keyframe k1 = sorted.get(i);
                Keyframe k2 = sorted.get(i + 1);
                double distance = Math.abs(k2.getValue() - k1.getValue());
                if (distance == 0) continue;

                double availableSeconds = (k2.getTime() - k1.getTime()) / 1000.0;
                double requestedVelocity = track.resolveVelocity(k2);
                double requestedAccel = track.resolveAcceleration(k2);

                // Avertissement si la vitesse demandée dépasse la capacité du moteur
                if (mc != null && requestedVelocity > mc.getMaxVelocity()) {
                    warnings.add(new ValidationWarning(
                            track.getId(), k1.getTime(), k2.getTime(),
                            String.format("Vitesse demandée %.0f°/s dépasse le maximum du moteur %.0f°/s",
                                    requestedVelocity, mc.getMaxVelocity())
                    ));
                }

                // Calcul du temps minimum avec la vitesse réellement applicable
                double effectiveVelocity = mc != null
                        ? Math.min(requestedVelocity, mc.getMaxVelocity())
                        : requestedVelocity;
                double effectiveAccel = mc != null
                        ? Math.min(requestedAccel, mc.getMaxAcceleration())
                        : requestedAccel;

                double minTime = computeMinTime(distance, effectiveVelocity, effectiveAccel);
                if (minTime > availableSeconds) {
                    warnings.add(new ValidationWarning(
                            track.getId(), k1.getTime(), k2.getTime(),
                            String.format(
                                    "Le moteur ne peut pas parcourir %.1f° en %dms " +
                                    "(minimum requis : %.0fms, vitesse effective : %.0f°/s, accel : %.0f°/s²)",
                                    distance,
                                    k2.getTime() - k1.getTime(),
                                    minTime * 1000,
                                    effectiveVelocity,
                                    effectiveAccel)
                    ));
                }
            }
        }
        return warnings;
    }

    /**
     * Calcule le temps minimum pour parcourir une distance avec profil trapézoïdal.
     *
     * @param distance    distance en degrés
     * @param velocity    vitesse max en °/s
     * @param acceleration accélération en °/s²
     * @return temps minimum en secondes
     */
    public double computeMinTime(double distance, double velocity, double acceleration) {
        double dRamp = (velocity * velocity) / acceleration;
        if (distance >= dRamp) {
            // Profil trapézoïdal : rampe + plateau + rampe
            return velocity / acceleration + distance / velocity;
        } else {
            // Profil triangulaire : pas assez de distance pour atteindre la vitesse max
            return 2.0 * Math.sqrt(distance / acceleration);
        }
    }
}
