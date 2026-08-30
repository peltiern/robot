package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.animation.modele.Animation;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import fr.roboteek.robot.organes.actionneurs.animation.modele.ImageCle;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Piste;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dit ce qu'une animation demande de plus que ce que les moteurs savent faire.
 * <p>
 * Il <b>avertit sans rien refuser</b> : une animation trop rapide reste enregistrable et jouable,
 * elle sera simplement traînarde sur le robot. Le seul juge utile est l'œil, et un éditeur qui
 * refuse d'enregistrer empêche d'essayer. L'avertissement dit où regarder quand le rendu déçoit.
 */
public class VerificateurAnimation {

    /**
     * Un point de l'animation que les moteurs ne tiendront pas.
     *
     * @param axe           l'axe concerné
     * @param instantDebut  début de l'intervalle en cause, en millisecondes
     * @param instantFin    fin de l'intervalle ; égal au début pour un défaut ponctuel
     * @param message       ce qui ne passe pas, rédigé pour être affiché tel quel dans l'éditeur
     */
    public record Avertissement(Axe axe, long instantDebut, long instantFin, String message) {
    }

    /**
     * Pas d'échantillonnage du contrôle de trajectoire. Plus fin que la cadence à laquelle le
     * lecteur enverra ses consignes : un dépassement que le contrôle ne verrait pas mais que le
     * lecteur atteindrait serait le pire des deux mondes.
     */
    private static final long PAS_ECHANTILLONNAGE_MS = 10;

    private final Map<Axe, LimitesMoteur> limitesParAxe;

    public VerificateurAnimation(Map<Axe, LimitesMoteur> limitesParAxe) {
        this.limitesParAxe = limitesParAxe;
    }

    /** Tous les avertissements de l'animation, dans l'ordre des pistes puis du temps. */
    public List<Avertissement> controler(Animation animation) {
        List<Avertissement> avertissements = new ArrayList<>();
        for (Piste piste : animation.pistes()) {
            LimitesMoteur limites = limitesParAxe.get(piste.axe());
            if (limites == null) {
                // Axe non configuré sur ce robot : rien à quoi comparer. Se taire plutôt que
                // d'inventer un avertissement par image-clé.
                continue;
            }
            boolean imagesClesDansLesButees = controlerLesPositions(piste, limites, avertissements);
            if (imagesClesDansLesButees) {
                controlerLaTrajectoire(piste, limites, avertissements);
            }
            controlerLesTransitions(piste, limites, avertissements);
        }
        return avertissements;
    }

    /**
     * Une position hors butée ne sera pas atteinte : le mouvement s'arrête sur la butée.
     *
     * @return vrai si toutes les images-clés tiennent dans les butées
     */
    private static boolean controlerLesPositions(Piste piste, LimitesMoteur limites, List<Avertissement> avertissements) {
        boolean toutesDedans = true;
        for (ImageCle imageCle : piste.imagesCles()) {
            if (!limites.contient(imageCle.valeur())) {
                toutesDedans = false;
                avertissements.add(new Avertissement(piste.axe(), imageCle.instant(), imageCle.instant(),
                        "Position %.1f° hors des butées [%.1f° ; %.1f°]"
                                .formatted(imageCle.valeur(), limites.positionMin(), limites.positionMax())));
            }
        }
        return toutesDedans;
    }

    /**
     * Le dépassement de la spline, invisible dans l'éditeur tant qu'on ne regarde que les points
     * posés : entre deux images-clés de même valeur précédées d'une montée, Catmull-Rom continue
     * sur sa lancée avant de revenir. Deux points à 20° peuvent ainsi faire passer la courbe à
     * 23,6°, et l'axe taper sa butée alors qu'aucune image-clé n'est fautive.
     * <p>
     * Un seul avertissement par piste, à l'endroit du pire écart : signaler chaque échantillon
     * fautif noierait l'éditeur sous des dizaines de lignes qui disent la même chose.
     */
    private static void controlerLaTrajectoire(Piste piste, LimitesMoteur limites, List<Avertissement> avertissements) {
        List<ImageCle> imagesCles = piste.imagesCles();
        if (imagesCles.size() < 2) {
            return;
        }
        long debut = imagesCles.getFirst().instant();
        long fin = imagesCles.getLast().instant();
        double pireValeur = 0;
        long pireInstant = -1;
        double pireEcart = 0;
        for (long instant = debut; instant <= fin; instant += PAS_ECHANTILLONNAGE_MS) {
            double valeur = Interpolateur.valeurA(imagesCles, instant);
            double ecart = Math.max(limites.positionMin() - valeur, valeur - limites.positionMax());
            if (ecart > pireEcart) {
                pireEcart = ecart;
                pireValeur = valeur;
                pireInstant = instant;
            }
        }
        if (pireInstant >= 0) {
            avertissements.add(new Avertissement(piste.axe(), pireInstant, pireInstant,
                    "La courbe dépasse jusqu'à %.1f° vers %d ms, hors des butées [%.1f° ; %.1f°], alors que les images-clés y tiennent"
                            .formatted(pireValeur, pireInstant, limites.positionMin(), limites.positionMax())));
        }
    }

    /**
     * Une transition trop ambitieuse : le moteur arrivera en retard, et l'image-clé suivante partira
     * d'une position qui n'est pas celle qu'on croit — le décalage se propage jusqu'à la fin.
     */
    private static void controlerLesTransitions(Piste piste, LimitesMoteur limites, List<Avertissement> avertissements) {
        List<ImageCle> imagesCles = piste.imagesCles();
        for (int i = 0; i < imagesCles.size() - 1; i++) {
            ImageCle depart = imagesCles.get(i);
            ImageCle arrivee = imagesCles.get(i + 1);
            double distance = Math.abs(arrivee.valeur() - depart.valeur());
            if (distance == 0) {
                continue;
            }
            double secondesDisponibles = (arrivee.instant() - depart.instant()) / 1000.0;
            double vitesse = Math.min(piste.vitesseDe(arrivee), limites.vitesseMax());
            double acceleration = Math.min(piste.accelerationDe(arrivee), limites.accelerationMax());

            double secondesNecessaires = dureeMinimale(distance, vitesse, acceleration);
            if (secondesNecessaires > secondesDisponibles) {
                avertissements.add(new Avertissement(piste.axe(), depart.instant(), arrivee.instant(),
                        "%.1f° en %d ms : il en faut %.0f au mieux (%.0f °/s, %.0f °/s²)"
                                .formatted(distance, arrivee.instant() - depart.instant(),
                                        secondesNecessaires * 1000, vitesse, acceleration)));
            }
        }
    }

    /**
     * Temps minimal pour parcourir une distance, rampes comprises.
     * <p>
     * Profil trapézoïdal quand la distance laisse le temps d'atteindre la vitesse de croisière,
     * triangulaire sinon — sur les yeux, dont l'accélération est faible devant la vitesse, c'est
     * presque toujours le cas triangulaire qui s'applique.
     *
     * @return durée en secondes
     */
    public static double dureeMinimale(double distance, double vitesse, double acceleration) {
        if (vitesse <= 0 || acceleration <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double distanceDesRampes = vitesse * vitesse / acceleration;
        if (distance >= distanceDesRampes) {
            return vitesse / acceleration + distance / vitesse;
        }
        return 2 * Math.sqrt(distance / acceleration);
    }
}
