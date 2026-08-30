package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.actionneurs.animation.modele.Axe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ce qu'un axe sait faire, pour juger si une animation lui est demandable.
 * <p>
 * Les positions sont en degrés <b>relatifs</b>, comme les images-clés, et non en position moteur
 * absolue : la configuration Phidgets raisonne en position absolue, la conversion se fait ici une
 * fois pour toutes.
 *
 * @param vitesseMax      vitesse maximale en °/s
 * @param accelerationMax accélération maximale en °/s²
 * @param positionMin     position relative la plus basse atteignable, en degrés
 * @param positionMax     position relative la plus haute atteignable, en degrés
 */
public record LimitesMoteur(double vitesseMax, double accelerationMax, double positionMin, double positionMax) {

    private static final Logger logger = LoggerFactory.getLogger(LimitesMoteur.class);

    /**
     * Limites de chaque axe déduites de la configuration.
     * <p>
     * <b>Ce sont les vitesses de travail</b> — celles auxquelles le regard et la manette font
     * bouger la tête — et non les maximums du matériel. C'est délibéré, et c'est le banc de mesure
     * du 2026-08-29 qui a tranché : interrogés, les servos annoncent 74 013 °/s et 370 065 °/s².
     * La documentation du RCC1000 explique ces deux nombres : ce sont les valeurs par défaut de
     * {@code MaxVelocityLimit} et {@code MaxAcceleration}, calculées par la bibliothèque à partir
     * du seul rapport d'échelle entre {@code MinPosition}/{@code MaxPosition} et
     * {@code MinPulseWidth}/{@code MaxPulseWidth}. Rien n'y vient du servo, qui ne dit jamais ce
     * qu'il sait faire. Valider une animation contre ces chiffres reviendrait à ne rien valider.
     * <p>
     * Le plafond reste donc le réglage, quitte à ce qu'il soit prudent : une animation qui le
     * dépasse est avertie, jamais refusée, et l'œil tranche. {@link #avecCapacites} reste offert
     * pour un axe dont on connaîtrait la vraie capacité, mesurée et non demandée au contrôleur.
     */
    public static Map<Axe, LimitesMoteur> parAxe(PhidgetsConfig configuration) {
        Map<Axe, LimitesMoteur> limites = new EnumMap<>(Axe.class);

        limites.put(Axe.OEIL_GAUCHE, new LimitesMoteur(
                configuration.eyeLeftMotorSpeed(), configuration.eyeLeftMotorAcceleration(),
                configuration.eyeMotorRelativePositionMin(), configuration.eyeMotorRelativePositionMax()));

        limites.put(Axe.OEIL_DROIT, new LimitesMoteur(
                configuration.eyeRightMotorSpeed(), configuration.eyeRightMotorAcceleration(),
                configuration.eyeMotorRelativePositionMin(), configuration.eyeMotorRelativePositionMax()));

        limites.put(Axe.COU_GAUCHE_DROITE, depuisPositionsMoteur(
                configuration.neckLeftRightMotorSpeed(), configuration.neckLeftRightMotorAcceleration(),
                configuration.neckLeftRightMotorInitialPosition(),
                configuration.neckLeftRightMotorMinPosition(), configuration.neckLeftRightMotorMaxPosition()));

        limites.put(Axe.COU_HAUT_BAS, depuisPositionsMoteur(
                configuration.neckTiltMotorSpeed(), configuration.neckTiltMotorAcceleration(),
                configuration.neckTiltMotorInitialPosition(),
                configuration.neckTiltMotorMinPosition(), configuration.neckTiltMotorMaxPosition()));

        // Seul axe sans @DefaultValue dans la configuration : sur un robot où il n'est pas réglé,
        // le lire lève. Mieux vaut une animation non contrôlée sur cet axe qu'un démarrage refusé.
        try {
            limites.put(Axe.COU_MONTER_DESCENDRE, depuisPositionsMoteur(
                    configuration.neckUpDownMotorSpeed(), configuration.neckUpDownMotorAcceleration(),
                    configuration.neckUpDownMotorInitialPosition(),
                    configuration.neckUpDownMotorMinPosition(), configuration.neckUpDownMotorMaxPosition()));
        } catch (RuntimeException e) {
            logger.warn("Axe {} non configuré : ses animations ne seront pas contrôlées", Axe.COU_MONTER_DESCENDRE);
        }

        return Map.copyOf(limites);
    }

    /**
     * Conversion des butées moteur en butées relatives. Le repère logique est inversé par rapport
     * au moteur ({@code position = init - positionMoteur}, cf. {@code Cou}), donc le maximum
     * moteur donne le minimum relatif.
     */
    private static LimitesMoteur depuisPositionsMoteur(double vitesse, double acceleration,
                                                       double positionInitiale, double moteurMin, double moteurMax) {
        return new LimitesMoteur(vitesse, acceleration, positionInitiale - moteurMax, positionInitiale - moteurMin);
    }

    /** Les mêmes butées, avec les vitesse et accélération réellement annoncées par le servo. */
    public LimitesMoteur avecCapacites(double vitesseMaxMoteur, double accelerationMaxMoteur) {
        return new LimitesMoteur(vitesseMaxMoteur, accelerationMaxMoteur, positionMin, positionMax);
    }

    /** Vrai si la position demandée est atteignable. */
    public boolean contient(double position) {
        return position >= positionMin && position <= positionMax;
    }

    /**
     * Ramène une position dans les butées.
     * <p>
     * Indispensable au lecteur, et pas seulement par prudence : la spline de Catmull-Rom
     * <b>dépasse</b> la valeur des images-clés qu'elle traverse. Deux points posés à 20° suffisent
     * à faire passer la courbe par 23,6° entre les deux ({@code InterpolateurTest}). Une animation
     * dont toutes les images-clés tiennent dans les butées peut donc en sortir en chemin.
     */
    public double ecreter(double position) {
        return Math.clamp(position, positionMin, positionMax);
    }
}
