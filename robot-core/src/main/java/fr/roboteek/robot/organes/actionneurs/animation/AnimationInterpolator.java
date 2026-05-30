package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;

import java.util.List;

/**
 * Calcule les positions interpolées d'une animation à un instant donné.
 * Utilise l'algorithme Catmull-Rom, identique à l'éditeur JavaScript.
 */
public class AnimationInterpolator {

    /**
     * Construit un {@link MouvementYeuxEvent} avec les positions interpolées à l'instant {@code time}.
     * Retourne null si aucun track yeux n'est présent.
     */
    public MouvementYeuxEvent buildYeuxEvent(Animation animation, long time) {
        MouvementYeuxEvent event = new MouvementYeuxEvent();
        boolean hasCommand = false;

        var oeilG = animation.getTrack(TrackId.OEIL_GAUCHE);
        if (oeilG.isPresent()) {
            Track t = oeilG.get();
            double val = catmullRomAt(t.getSortedKeyframes(), time);
            event.setPositionOeilGauche(val);
            event.setVitesseOeilGauche(t.getDefaultVelocity());
            event.setAccelerationOeilGauche(t.getDefaultAcceleration());
            hasCommand = true;
        }

        var oeilD = animation.getTrack(TrackId.OEIL_DROIT);
        if (oeilD.isPresent()) {
            Track t = oeilD.get();
            double val = catmullRomAt(t.getSortedKeyframes(), time);
            event.setPositionOeilDroit(val);
            event.setVitesseOeilDroit(t.getDefaultVelocity());
            event.setAccelerationOeilDroit(t.getDefaultAcceleration());
            hasCommand = true;
        }

        return hasCommand ? event : null;
    }

    /**
     * Construit un {@link MouvementCouEvent} avec les positions interpolées à l'instant {@code time}.
     * Retourne null si aucun track cou n'est présent.
     */
    public MouvementCouEvent buildCouEvent(Animation animation, long time) {
        MouvementCouEvent event = new MouvementCouEvent();
        boolean hasCommand = false;

        var couGD = animation.getTrack(TrackId.COU_GAUCHE_DROITE);
        if (couGD.isPresent()) {
            Track t = couGD.get();
            event.setPositionPanoramique(catmullRomAt(t.getSortedKeyframes(), time));
            event.setVitessePanoramique(t.getDefaultVelocity());
            event.setAccelerationPanoramique(t.getDefaultAcceleration());
            hasCommand = true;
        }

        var couHB = animation.getTrack(TrackId.COU_HAUT_BAS);
        if (couHB.isPresent()) {
            Track t = couHB.get();
            event.setPositionInclinaison(catmullRomAt(t.getSortedKeyframes(), time));
            event.setVitesseInclinaison(t.getDefaultVelocity());
            event.setAccelerationInclinaison(t.getDefaultAcceleration());
            hasCommand = true;
        }

        var couMD = animation.getTrack(TrackId.COU_MONTER_DESCENDRE);
        if (couMD.isPresent()) {
            Track t = couMD.get();
            event.setPositionMonterDescendre(catmullRomAt(t.getSortedKeyframes(), time));
            event.setVitesseMonterDescendre(t.getDefaultVelocity());
            event.setAccelerationMonterDescendre(t.getDefaultAcceleration());
            hasCommand = true;
        }

        return hasCommand ? event : null;
    }

    // ── Catmull-Rom ───────────────────────────────────────────────────────────

    /**
     * Interpolation Catmull-Rom à l'instant {@code t} sur une liste de keyframes triées.
     * Identique à l'implémentation JavaScript de l'éditeur d'animation.
     */
    double catmullRomAt(List<Keyframe> kfs, long t) {
        if (kfs.isEmpty()) return 0;
        if (kfs.size() == 1) return kfs.get(0).getValue();
        if (t <= kfs.get(0).getTime()) return kfs.get(0).getValue();
        if (t >= kfs.get(kfs.size() - 1).getTime()) return kfs.get(kfs.size() - 1).getValue();

        // Trouver l'intervalle [p1, p2] contenant t
        int i = 1;
        while (i < kfs.size() && kfs.get(i).getTime() <= t) i++;

        Keyframe p1 = kfs.get(i - 1);
        Keyframe p2 = kfs.get(i);

        // Points fantômes aux extrémités pour les tangentes
        long dt12 = p2.getTime() - p1.getTime();
        Keyframe p0 = i > 1
                ? kfs.get(i - 2)
                : new Keyframe(p1.getTime() - dt12, p1.getValue());
        Keyframe p3 = i < kfs.size() - 1
                ? kfs.get(i + 1)
                : new Keyframe(p2.getTime() + dt12, p2.getValue());

        double u = (double) (t - p1.getTime()) / dt12;
        double u2 = u * u;
        double u3 = u2 * u;

        double v0 = p0.getValue(), v1 = p1.getValue(),
               v2 = p2.getValue(), v3 = p3.getValue();

        return 0.5 * (
                2 * v1
                + (-v0 + v2) * u
                + (2 * v0 - 5 * v1 + 4 * v2 - v3) * u2
                + (-v0 + 3 * v1 - 3 * v2 + v3) * u3
        );
    }
}
