package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_ROULIS;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent.MOUVEMENTS_OEIL;
import fr.roboteek.robot.systemenerveux.event.TelemetrieOrganeEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.phidgets.PhidgetsServoMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Classe représentant les yeux du robot.
 * <p>
 * Migré en bean Spring : cycle de vie géré par {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#ACTIONNEURS_AVEC_MOTEUR}. Les moteurs ne sont créés
 * et engagés qu'au {@code start()} — pas à la construction du bean — pour respecter
 * l'ordre des phases (moteurs derniers démarrés, premiers arrêtés).
 *
 * @author Java Developer
 */
@Component
public class Yeux extends AbstractOrgane implements SmartLifecycle, OrganeSurveille {

    /**
     * Moteur Gauche / Droite.
     */
    private PhidgetsServoMotor moteurOeilGauche;

    /**
     * Moteur Haut / Bas.
     */
    private PhidgetsServoMotor moteurOeilDroit;

    /**
     * Phidgets Configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    // Volatiles : écrits par les threads d'évènements, lus par le watchdog (enMouvement()).
    private volatile MOUVEMENTS_OEIL mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.STOPPER;
    private volatile MOUVEMENTS_OEIL mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.STOPPER;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Yeux.class);

    /**
     * Tolérance (en degrés) pour considérer une position servo atteinte.
     */
    private static final double TOLERANCE_POSITION = 1.0;

    /**
     * Marge (en degrés) dont la position de repos peut dépasser les butées logicielles de
     * mouvement : l'appui mécanique stable se trouve juste au-delà des angles autorisés
     * en fonctionnement. Garde-fou contre une valeur aberrante dans robot.properties.
     */
    private static final double MARGE_REPOS = 5.0;

    /**
     * Variation minimale (degrés) pour rediffuser la télémétrie de position : filtre
     * l'immobilité sans hacher le mouvement (voir {@link #diffuserTelemetrie()}).
     */
    private static final double SEUIL_VARIATION_TELEMETRIE = 0.2;

    /** Dernières positions diffusées en télémétrie, pour n'émettre que sur variation réelle. */
    private Map<String, Double> dernieresPositionsDiffusees = Map.of();

    /**
     * Rémanence du constat de mouvement (voir {@link #enMouvement()}) : une consigne de position
     * atteinte lentement peut ne pas franchir {@link #SEUIL_VARIATION_TELEMETRIE} à chaque relevé
     * de 50 ms, et les yeux paraîtraient immobiles par intermittence en plein mouvement.
     */
    private static final long REMANENCE_MOUVEMENT_MS = 500;

    /** Instant de la dernière variation de position constatée. */
    private volatile long instantDerniereVariation = 0L;

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Arrêt d'urgence en cours : tout ordre de mouvement est refusé jusqu'au réarmement
     * (cf. {@link fr.roboteek.robot.securite.ArretUrgence}).
     */
    private volatile boolean arretUrgence = false;

    /**
     * Constructeur.
     */
    public Yeux() {
        super();
        phidgetsConfig = phidgetsConfig();
    }

    @Override
    public void initialiser() {
        // Création des moteurs au démarrage de la phase (et non à la construction du bean).
        // Chaque servo est engagé à sa position de repos — sa position physique probable,
        // laissée par le dernier arrêt — pour éviter le saut à pleine vitesse à l'engagement ;
        // le retour à la position zéro se fait ensuite en rampe douce (reset()).
        moteurOeilGauche = new PhidgetsServoMotor(
                phidgetsConfig.eyeLeftMotorIndex(),
                phidgetsConfig.eyeLeftMotorPositionZero(),
                toPositionAbsolueOeilGauche(phidgetsConfig.eyeMotorRelativePositionMax()),
                toPositionAbsolueOeilGauche(phidgetsConfig.eyeMotorRelativePositionMin()),
                phidgetsConfig.eyeLeftMotorSpeed(),
                phidgetsConfig.eyeLeftMotorAcceleration(),
                toPositionAbsolueOeilGauche(positionReposRelative())
        );
        moteurOeilDroit = new PhidgetsServoMotor(
                phidgetsConfig.eyeRightMotorIndex(),
                phidgetsConfig.eyeRightMotorPositionZero(),
                toPositionAbsolueOeilDroit(phidgetsConfig.eyeMotorRelativePositionMax()),
                toPositionAbsolueOeilDroit(phidgetsConfig.eyeMotorRelativePositionMin()),
                phidgetsConfig.eyeRightMotorSpeed(),
                phidgetsConfig.eyeRightMotorAcceleration(),
                toPositionAbsolueOeilDroit(positionReposRelative())
        );

        moteurOeilGauche.setSpeedRampingState(true);

        moteurOeilDroit.setSpeedRampingState(true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reset();
        logger.info("Yeux : fin initialisation");
    }

    /**
     * Tourne l'oeil gauche vers le bas sans s'arrêter.
     */
    public void tournerOeilGaucheVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.TOURNER_BAS) {
            mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.TOURNER_BAS;
            moteurOeilGauche.backward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil gauche vers le haut sans s'arrêter.
     */
    public void tournerOeilGaucheVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.TOURNER_HAUT) {
            mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.TOURNER_HAUT;
            moteurOeilGauche.forward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil gauche d'un certain angle.
     *
     * @param angle angle en degrés (négatif : vers le bas, positif : vers le haut)
     */
    public void tournerOeilGauche(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurOeilGauche.rotate(-angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne l'oeil gauche à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     *
     * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilGauche(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
        if (positionRelative >= phidgetsConfig.eyeMotorRelativePositionMin() && positionRelative <= phidgetsConfig.eyeMotorRelativePositionMax()) {
            double positionMoteur = toPositionAbsolueOeilGauche(positionRelative);
            moteurOeilGauche.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de l'oeil gauche.
     */
    public void stopperOeilGauche() {
        moteurOeilGauche.stop();
        mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.STOPPER;
    }

    /**
     * Tourne l'oeil droit vers le bas sans s'arrêter.
     */
    public void tournerOeilDroitVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.TOURNER_BAS) {
            mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.TOURNER_BAS;
            moteurOeilDroit.forward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil droit vers le haut sans s'arrêter.
     */
    public void tournerOeilDroitVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.TOURNER_HAUT) {
            mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.TOURNER_HAUT;
            moteurOeilDroit.backward(vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Tourne l'oeil droit d'un certain angle.
     *
     * @param angle angle en degrés (négatif : en bas, positif : en haut)
     */
    public void tournerOeilDroit(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        moteurOeilDroit.rotate(-angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne l'oeil droit à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     *
     * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilDroit(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
        if (positionRelative >= phidgetsConfig.eyeMotorRelativePositionMin() && positionRelative <= phidgetsConfig.eyeMotorRelativePositionMax()) {
            double positionMoteur = toPositionAbsolueOeilDroit(positionRelative);
            moteurOeilDroit.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de l'oeil droit.
     */
    public void stopperOeilDroit() {
        moteurOeilDroit.stop();
        mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.STOPPER;
    }

    /**
     * Fait tourner les yeux d'un cran de roulis (rotation « solidaire » des deux yeux : ils
     * n'ont pas de moteur de roulis dédié, on le simule en bougeant chaque oeil en sens opposé).
     * <p>
     * Modèle, volontairement simple et sans état mémorisé — chaque appel repart de la position
     * réelle courante des deux yeux :
     * <ol>
     *   <li><b>Calibration</b> (mesurée à la manette, sans Y) : pour les DEUX yeux, une position
     *   relative qui AUGMENTE fait MONTER l'oeil, qui DIMINUE le fait DESCENDRE.</li>
     *   <li><b>Sens</b> : {@code angleRoulisDemande < 0} (ANTI_HORAIRE) => oeil gauche MONTE et
     *   oeil droit DESCEND ; {@code > 0} (HORAIRE) => l'inverse. Les deux yeux vont TOUJOURS en
     *   sens opposés — un roulis ne peut donc jamais baisser les deux yeux à la fois.</li>
     *   <li><b>Solidaire + butées</b> : les deux yeux bougent de la MÊME amplitude. Celle-ci est
     *   plafonnée au débattement restant du plus contraint des deux yeux dans SON sens. Si l'un
     *   est déjà en butée dans le sens voulu, l'amplitude tombe à 0 : le roulis est bloqué de ce
     *   côté (l'autre oeil ne bouge pas tout seul), mais reste possible dans le sens inverse.</li>
     * </ol>
     *
     * @param angleRoulisDemande amplitude signée du cran (degrés) : &lt;0 ANTI_HORAIRE, &gt;0 HORAIRE
     */
    public void setPositionRoulis(double angleRoulisDemande, Double vitesse, Double acceleration, boolean waitForPosition) {
        final double min = phidgetsConfig.eyeMotorRelativePositionMin();
        final double max = phidgetsConfig.eyeMotorRelativePositionMax();

        // Position relative RÉELLE courante des deux yeux (bornée à la plage utile pour absorber
        // le bruit capteur). Aucun état figé : le roulis s'applique par-dessus la position du moment.
        double positionGauche = clamp(toPositionRelativeOeilGauche(moteurOeilGauche.getPositionReelle()), min, max);
        double positionDroit = clamp(toPositionRelativeOeilDroit(moteurOeilDroit.getPositionReelle()), min, max);

        // Sens de chaque oeil (opposés) : ANTI_HORAIRE (angle < 0) => gauche monte (+), droit descend (-).
        double sensGauche = angleRoulisDemande < 0 ? +1 : -1;
        double sensDroit = -sensGauche;

        // Débattement restant de chaque oeil dans SON sens, puis amplitude solidaire = le minimum.
        double margeGauche = sensGauche > 0 ? max - positionGauche : positionGauche - min;
        double margeDroit = sensDroit > 0 ? max - positionDroit : positionDroit - min;
        double amplitude = Math.max(0, Math.min(Math.abs(angleRoulisDemande), Math.min(margeGauche, margeDroit)));

        // Cibles RE-bornées à [min,max] : quand l'amplitude est limitée par l'oeil le plus contraint,
        // sa cible tombe pile sur sa butée et un arrondi flottant pourrait la faire dépasser d'un
        // epsilon. positionnerOeil rejetterait alors ce mouvement (garde min/max) et un SEUL oeil
        // bougerait. Le clamp garantit que les DEUX cibles restent acceptées => mouvement solidaire.
        double cibleGauche = clamp(positionGauche + sensGauche * amplitude, min, max);
        double cibleDroit = clamp(positionDroit + sensDroit * amplitude, min, max);
        positionnerOeilGauche(cibleGauche, vitesse, acceleration, false);
        positionnerOeilDroit(cibleDroit, vitesse, acceleration, waitForPosition);
    }

    /**
     * Borne une valeur dans l'intervalle [min, max].
     */
    private static double clamp(double valeur, double min, double max) {
        return Math.max(min, Math.min(max, valeur));
    }


    /**
     * Intercepte les évènements de mouvements.
     *
     * @param mouvementYeuxEvent évènement de mouvements
     */
    @EventListener
    @Async(RobotEventsConfig.YEUX_EVENT_EXECUTOR)
    public void handleMouvementYeuxEvent(MouvementYeuxEvent mouvementYeuxEvent) {
        if (!running || arretUrgence) {
            return;
        }
        if (mouvementYeuxEvent.getPositionOeilGauche() != MouvementYeuxEvent.POSITION_NEUTRE) {
            // TODO ne pas mettre en synchrone si oeil droit en synchrone ==> A corriger
            positionnerOeilGauche(mouvementYeuxEvent.getPositionOeilGauche(), mouvementYeuxEvent.getVitesseOeilGauche(), mouvementYeuxEvent.getAccelerationOeilGauche(), false/*mouvementYeuxEvent.isSynchrone()*/);
        } else if (mouvementYeuxEvent.getMouvementOeilGauche() != null) {
            if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.STOPPER) {
                stopperOeilGauche();
            } else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_BAS) {
                tournerOeilGaucheVersBas(mouvementYeuxEvent.getVitesseOeilGauche(), mouvementYeuxEvent.getAccelerationOeilGauche(), mouvementYeuxEvent.isSynchrone());
            } else if (mouvementYeuxEvent.getMouvementOeilGauche() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
                tournerOeilGaucheVersHaut(mouvementYeuxEvent.getVitesseOeilGauche(), mouvementYeuxEvent.getAccelerationOeilGauche(), mouvementYeuxEvent.isSynchrone());
            }
        }
        if (mouvementYeuxEvent.getPositionOeilDroit() != MouvementYeuxEvent.POSITION_NEUTRE) {
            positionnerOeilDroit(mouvementYeuxEvent.getPositionOeilDroit(), mouvementYeuxEvent.getVitesseOeilDroit(), mouvementYeuxEvent.getAccelerationOeilDroit(), mouvementYeuxEvent.isSynchrone());
        } else if (mouvementYeuxEvent.getMouvementOeilDroit() != null) {
            if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.STOPPER) {
                stopperOeilDroit();
            } else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_BAS) {
                tournerOeilDroitVersBas(mouvementYeuxEvent.getVitesseOeilDroit(), mouvementYeuxEvent.getAccelerationOeilDroit(), mouvementYeuxEvent.isSynchrone());
            } else if (mouvementYeuxEvent.getMouvementOeilDroit() == MOUVEMENTS_OEIL.TOURNER_HAUT) {
                tournerOeilDroitVersHaut(mouvementYeuxEvent.getVitesseOeilDroit(), mouvementYeuxEvent.getAccelerationOeilDroit(), mouvementYeuxEvent.isSynchrone());
            }
        }
    }

    /**
     * Intercepte les évènements de mouvements de cou.
     *
     * @param mouvementCouEvent évènement de mouvements de cou
     */
    @EventListener
    @Async(RobotEventsConfig.YEUX_EVENT_EXECUTOR)
    public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        if (!running || arretUrgence) {
            return;
        }
        if (mouvementCouEvent.getMouvementRoulis() == MOUVEMENTS_ROULIS.HORAIRE) {
            setPositionRoulis(mouvementCouEvent.getPositionRoulis(), mouvementCouEvent.getVitesseRoulis(), mouvementCouEvent.getAccelerationRoulis(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementRoulis() == MOUVEMENTS_ROULIS.ANTI_HORAIRE) {
            setPositionRoulis(-mouvementCouEvent.getPositionRoulis(), mouvementCouEvent.getVitesseRoulis(), mouvementCouEvent.getAccelerationRoulis(), mouvementCouEvent.isSynchrone());
        }
    }

    /**
     * Arrêt d'urgence : coupe les deux moteurs d'yeux sur-le-champ. Voir
     * {@code Cou.handleArretUrgenceEvent} pour le pourquoi du traitement synchrone.
     */
    @EventListener
    public void handleArretUrgenceEvent(ArretUrgenceEvent evenement) {
        arretUrgence = evenement.isActif();
        if (!running || !evenement.isActif()) {
            return;
        }
        logger.warn("Yeux : arrêt d'urgence, coupure des moteurs");
        stopperOeilGauche();
        stopperOeilDroit();
    }

    @Override
    public void arreter() {
        // Rejoint une position de repos mécaniquement stable (yeux baissés) AVANT le
        // désengagement des servos, pour éviter que les yeux ne tombent d'un coup.
        // Position relative réglable dans robot.properties (clé eyes.motor.relative.position.rest,
        // négatif = baissés) ; à défaut, la position zéro est visée (comportement historique).
        double reposRelatif = positionReposRelative();
        double reposOeilGauche = toPositionAbsolueOeilGauche(reposRelatif);
        double reposOeilDroit = toPositionAbsolueOeilDroit(reposRelatif);
        logger.info("Positions au moment de l'arrêt : gauche={} droit={} — cible de repos relative : {}",
                moteurOeilGauche.getPositionReelle(), moteurOeilDroit.getPositionReelle(), reposRelatif);
        moteurOeilGauche.setPositionCible(reposOeilGauche, null, null, false);
        moteurOeilDroit.setPositionCible(reposOeilDroit, null, null, false);
        // Attente (bornée) de l'arrivée en position de repos, à la tolérance près
        // (une comparaison stricte de doubles n'est jamais vraie et épuisait les 5 s)
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite
                && !(estAtteinte(moteurOeilGauche, reposOeilGauche)
                && estAtteinte(moteurOeilDroit, reposOeilDroit))) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Temps de stabilisation : la position remontée par le Phidget est celle de la
        // trajectoire commandée, pas celle du servo réel qui traîne un peu derrière —
        // on lui laisse le temps de se poser avant de couper.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        moteurOeilGauche.stop();
        moteurOeilDroit.stop();
        moteurOeilGauche.setSpeedRampingState(true);
        moteurOeilDroit.setSpeedRampingState(true);
        moteurOeilGauche.setEngaged(false);
        moteurOeilDroit.setEngaged(false);
        moteurOeilGauche.close();
        moteurOeilDroit.close();
    }

    /**
     * Indique si le moteur a atteint la position cible, à {@link #TOLERANCE_POSITION} près.
     */
    private static boolean estAtteinte(PhidgetsServoMotor moteur, double cible) {
        return Math.abs(moteur.getPositionReelle() - cible) < TOLERANCE_POSITION;
    }

    /**
     * Position de repos relative configurée (négatif = yeux baissés), ou position zéro si
     * absente. Bornée aux limites relatives élargies de {@link #MARGE_REPOS} — le travail
     * se fait en relatif car les bornes absolues des deux yeux sont inversées.
     */
    private double positionReposRelative() {
        Double reposConfigure = phidgetsConfig.eyeMotorRelativeRestPosition();
        double repos = reposConfigure != null ? reposConfigure : 0;
        return Math.max(phidgetsConfig.eyeMotorRelativePositionMin() - MARGE_REPOS,
                Math.min(phidgetsConfig.eyeMotorRelativePositionMax() + MARGE_REPOS, repos));
    }

    /**
     * Remet les yeux à leur position par défaut.
     */
    private void reset() {
        moteurOeilDroit.setPositionCible(phidgetsConfig.eyeRightMotorPositionZero(), null, null, false);
        moteurOeilGauche.setPositionCible(phidgetsConfig.eyeLeftMotorPositionZero(), null, null, false);
    }

    /**
     * Calcule la position absolue de l'oeil gauche à partir d'une position relative.
     *
     * @param positionRelative la position relative
     */
    private double toPositionAbsolueOeilGauche(double positionRelative) {
        return phidgetsConfig.eyeLeftMotorPositionZero() - positionRelative;
    }

    /**
     * Calcule la position relative de l'oeil gauche à partir d'une position absolue.
     *
     * @param positionAbsolue la position absolue
     */
    private double toPositionRelativeOeilGauche(double positionAbsolue) {
        return phidgetsConfig.eyeLeftMotorPositionZero() - positionAbsolue;
    }

    /**
     * Calcule la position absolue de l'oeil droit à partir d'une position relative.
     *
     * @param positionRelative la position relative
     */
    private double toPositionAbsolueOeilDroit(double positionRelative) {
        return phidgetsConfig.eyeRightMotorPositionZero() + positionRelative;
    }

    /**
     * Calcule la position relative de l'oeil droit à partir d'une position absolue.
     * Inverse de {@link #toPositionAbsolueOeilDroit} (absolue = zéro + relative).
     *
     * @param positionAbsolue la position absolue
     */
    private double toPositionRelativeOeilDroit(double positionAbsolue) {
        return positionAbsolue - phidgetsConfig.eyeRightMotorPositionZero();
    }

    /**
     * Position relative courante de l'œil gauche (0 = horizontal, cf. {@link #positionnerOeilGauche}),
     * ou {@code null} si l'organe n'est pas démarré (les moteurs ne sont créés qu'au {@code start()}).
     */
    public Double getPositionOeilGaucheCourante() {
        if (!running || moteurOeilGauche == null) {
            return null;
        }
        Double reelle = moteurOeilGauche.getPositionReelleOuNull();
        return reelle == null ? null : toPositionRelativeOeilGauche(reelle);
    }

    /**
     * Position relative courante de l'œil droit, ou {@code null} si l'organe n'est pas démarré.
     */
    public Double getPositionOeilDroitCourante() {
        if (!running || moteurOeilDroit == null) {
            return null;
        }
        Double reelle = moteurOeilDroit.getPositionReelleOuNull();
        return reelle == null ? null : toPositionRelativeOeilDroit(reelle);
    }

    /**
     * Publie la position courante des yeux en télémétrie, pour que les curseurs de contrôle
     * suivent en direct les mouvements <b>physiques</b> du robot (manette, animations,
     * comportements) et pas seulement les commandes qu'ils envoient eux-mêmes. Uniquement quand
     * une position varie sensiblement — rien n'est émis à l'arrêt, ni tant que l'organe n'est pas
     * démarré (positions indisponibles). Mêmes identifiants que {@code /api/organes}.
     */
    @Scheduled(fixedRate = 50)
    public void diffuserTelemetrie() {
        Map<String, Double> positions = new LinkedHashMap<>();
        ajouterSiPresent(positions, "oeilGauche", getPositionOeilGaucheCourante());
        ajouterSiPresent(positions, "oeilDroit", getPositionOeilDroitCourante());
        if (positions.isEmpty()) {
            return;
        }
        // Signe de vie : les deux canaux Phidget ont répondu. C'est ce relevé, et non le
        // drapeau de cycle de vie, qui atteste que les yeux sont encore joignables.
        battement();
        if (!aVarie(positions, dernieresPositionsDiffusees)) {
            return;
        }
        instantDerniereVariation = System.currentTimeMillis();
        dernieresPositionsDiffusees = positions;
        applicationEventPublisher.publishEvent(new TelemetrieOrganeEvent(idOrgane(), positions));
    }

    private static void ajouterSiPresent(Map<String, Double> valeurs, String id, Double valeur) {
        if (valeur != null) {
            valeurs.put(id, valeur);
        }
    }

    /** Vrai si l'ensemble des identifiants a changé, ou si une valeur a bougé de plus de {@link #SEUIL_VARIATION_TELEMETRIE}. */
    private static boolean aVarie(Map<String, Double> valeurs, Map<String, Double> precedentes) {
        if (!valeurs.keySet().equals(precedentes.keySet())) {
            return true;
        }
        for (Map.Entry<String, Double> entree : valeurs.entrySet()) {
            Double precedente = precedentes.get(entree.getKey());
            if (precedente == null || Math.abs(entree.getValue() - precedente) > SEUIL_VARIATION_TELEMETRIE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Intercepte les évènements d'affichage de position.
     *
     * @param displayPositionEvent évènement
     */
    @EventListener
    public void handleDisplayPositionEvent(DisplayPositionEvent displayPositionEvent) {
        if (!running) {
            return;
        }
        double positionRelativeOeilGauche = toPositionRelativeOeilGauche(moteurOeilGauche.getPositionReelle());
        double positionRelativeOeilDroit = toPositionRelativeOeilDroit(moteurOeilDroit.getPositionReelle());
        logger.debug("YEUX\tgauche = {}\tdroit = {}", positionRelativeOeilGauche, positionRelativeOeilDroit);
    }

    @Override
    public void start() {
        initialiser();
        running = true;
        logger.info("Yeux démarrés");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("Yeux arrêtés");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.ACTIONNEURS_AVEC_MOTEUR;
    }

    // --- Surveillance (watchdog + pastilles d'état de l'interface) ---

    @Override
    public String idOrgane() {
        return "yeux";
    }

    @Override
    public String libelleOrgane() {
        return "Yeux";
    }

    @Override
    public NatureOrgane nature() {
        return NatureOrgane.ACTIONNEUR;
    }

    @Override
    public boolean enService() {
        return running;
    }

    /** Les yeux exécutent des mouvements : leur silence est un motif d'arrêt d'urgence. */
    @Override
    public boolean provoqueUnMouvement() {
        return true;
    }

    /**
     * Vrai si un mouvement continu est en cours, ou si la position a varié récemment — ce second
     * critère couvre les consignes de position (animations, curseurs du HUD), qui ne laissent aucun
     * mouvement « en cours » derrière elles.
     */
    @Override
    public boolean enMouvement() {
        return mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.STOPPER
                || mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.STOPPER
                || System.currentTimeMillis() - instantDerniereVariation < REMANENCE_MOUVEMENT_MS;
    }
}
