package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.transmission.Transmission;
import fr.roboteek.robot.organes.actionneurs.transmission.TransmissionOeil;
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
 * Cycle de vie géré par {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#ACTIONNEURS_AVEC_MOTEUR}. Les moteurs ne sont créés
 * et engagés qu'au {@code start()} — pas à la construction du bean — pour respecter
 * l'ordre des phases (moteurs derniers démarrés, premiers arrêtés).
 *
 * @author Java Developer
 */
@Component
public class Yeux extends AbstractOrgane implements SmartLifecycle, OrganeSurveille {

    /** Moteur Gauche / Droite. */
    private PhidgetsServoMotor moteurOeilGauche;

    /** Moteur Haut / Bas. */
    private PhidgetsServoMotor moteurOeilDroit;

    /**
     * Conversion angle d'œil ↔ position moteur, une par œil.
     * <p>
     * Les deux ne sont pas les mêmes et ne le seront jamais : les servos sont montés en
     * miroir, donc une position relative qui monte l'œil gauche fait <b>descendre</b> le
     * moteur, et l'inverse à droite. C'était écrit à la main dans quatre méthodes privées.
     * <p>
     * Ce n'est pas une affine : la tringlerie est un quadrilatère articulé, dont le rapport va de
     * 1,25° d'œil par degré de servo au neutre à 3,70° en bout de course
     * ({@code robot-core/3d/mesures/MESURES.md}). Les positions qui traversent cette classe et
     * l'évènement {@link MouvementYeuxEvent} sont donc des <b>degrés d'œil</b>, plus des unités
     * moteur.
     * <p>
     * <b>Attention à ce que la mesure du 2026-09-03 prouvait vraiment.</b> Le roulis de l'image
     * avait été mesuré — la caméra est solidaire d'une coque — et 20 unités l'avaient fait tourner
     * de 30,8° là où la loi d'alors en prédisait 31,06. On y a lu une confirmation de l'échelle ;
     * c'en était une de la <b>forme</b> de la loi, à cet endroit de la course seulement. La loi
     * corrigée prédit 30,84° pour le même essai, donc encore mieux : les deux erreurs de signe et
     * d'échelle se compensaient presque exactement au voisinage du neutre, et c'est précisément ce
     * qui les a rendues invisibles un mois durant.
     */
    private Transmission transmissionOeilGauche;

    private Transmission transmissionOeilDroit;

    /** Phidgets Configuration. */
    private PhidgetsConfig phidgetsConfig;

    // Volatiles : écrits par les threads d'évènements, lus par le watchdog (enMouvement()).
    private volatile MOUVEMENTS_OEIL mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.STOPPER;
    private volatile MOUVEMENTS_OEIL mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.STOPPER;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Yeux.class);

    /** Tolérance (en degrés) pour considérer une position servo atteinte. */
    private static final double TOLERANCE_POSITION = 1.0;

    /**
     * Gain de la tringlerie à l'œil au repos, pour les vitesses qu'on ne peut pas rattacher à un
     * angle visé : les valeurs par défaut écrites sur le servo à l'attache, et les mouvements
     * continus de la manette, qui n'ont pas de cible. Le rapport variant de 1,25 à 3,70 le long de
     * la course, c'est une approximation — assumée, cf. {@link #enUnitesMoteur}.
     */
    private static final double GAIN_AU_NEUTRE = 1 / (1.2517 * PhidgetsServoMotor.DEGRES_SERVO_PAR_UNITE);

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

    /** Flag de démarrage de l'organe (cycle de vie Spring). */
    private volatile boolean running = false;

    /**
     * Arrêt d'urgence en cours : tout ordre de mouvement est refusé jusqu'au réarmement
     * (cf. {@link fr.roboteek.robot.securite.ArretUrgence}).
     */
    private volatile boolean arretUrgence = false;

    /** Constructeur. */
    public Yeux() {
        super();
        phidgetsConfig = phidgetsConfig();
    }

    @Override
    public void initialiser() {
        // Les transmissions se figent ici, au même instant que les butées qu'on en déduit :
        // relire la position zéro à chaque conversion, comme avant, la laissait diverger des
        // bornes du moteur, elles calculées une seule fois. Un étalonnage à chaud du zéro ne
        // s'appliquait donc qu'à moitié.
        // Les deux servos sont montés en MIROIR : une même commande d'organe les fait tourner en
        // sens inverse, et c'est le seul écart entre les deux yeux — la loi de la tringlerie, elle,
        // est la même des deux côtés. Le signe est donc porté ici et nulle part ailleurs.
        // Le zéro n'est PAS la position de départ : c'est celle où le dessus de la coque est de
        // niveau, et elle se mesure. Les deux ont porté le même nom jusqu'au 2026-09-07, et le
        // signe comme l'échelle étaient faux en plus — une unité Phidgets vaut 1,583° de servo et
        // non 1 — d'où une butée haute posée 14° au-delà du contact des coques, contre lequel les
        // servos forçaient à chaque animation.
        transmissionOeilGauche = new TransmissionOeil(
                phidgetsConfig.eyeLeftMotorZeroPosition(), +PhidgetsServoMotor.DEGRES_SERVO_PAR_UNITE);
        transmissionOeilDroit = new TransmissionOeil(
                phidgetsConfig.eyeRightMotorZeroPosition(), -PhidgetsServoMotor.DEGRES_SERVO_PAR_UNITE);

        // Création des moteurs au démarrage de la phase (et non à la construction du bean).
        // Chaque servo est engagé à sa position de repos — sa position physique probable,
        // laissée par le dernier arrêt — pour éviter le saut à pleine vitesse à l'engagement ;
        // le retour à la position zéro se fait ensuite en rampe douce (reset()).
        moteurOeilGauche = new PhidgetsServoMotor(
                phidgetsConfig.eyeLeftMotorIndex(),
                phidgetsConfig.eyeLeftMotorInitialPosition(),
                transmissionOeilGauche.versMoteur(phidgetsConfig.eyePositionMax()),
                transmissionOeilGauche.versMoteur(phidgetsConfig.eyePositionMin()),
                phidgetsConfig.eyeLeftSpeed() * GAIN_AU_NEUTRE,
                phidgetsConfig.eyeLeftAcceleration() * GAIN_AU_NEUTRE,
                transmissionOeilGauche.versMoteur(positionReposRelative())
        );
        moteurOeilDroit = new PhidgetsServoMotor(
                phidgetsConfig.eyeRightMotorIndex(),
                phidgetsConfig.eyeRightMotorInitialPosition(),
                transmissionOeilDroit.versMoteur(phidgetsConfig.eyePositionMax()),
                transmissionOeilDroit.versMoteur(phidgetsConfig.eyePositionMin()),
                phidgetsConfig.eyeRightSpeed() * GAIN_AU_NEUTRE,
                phidgetsConfig.eyeRightAcceleration() * GAIN_AU_NEUTRE,
                transmissionOeilDroit.versMoteur(positionReposRelative())
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

    /** Tourne l'oeil gauche vers le bas sans s'arrêter. */
    public void tournerOeilGaucheVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.TOURNER_BAS) {
            mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.TOURNER_BAS;
            moteurOeilGauche.backward(enUnitesMoteur(vitesse), enUnitesMoteur(acceleration), waitForPosition);
        }
    }

    /** Tourne l'oeil gauche vers le haut sans s'arrêter. */
    public void tournerOeilGaucheVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilGaucheEnCours != MOUVEMENTS_OEIL.TOURNER_HAUT) {
            mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.TOURNER_HAUT;
            moteurOeilGauche.forward(enUnitesMoteur(vitesse), enUnitesMoteur(acceleration), waitForPosition);
        }
    }

    /**
     * Tourne l'oeil gauche d'un certain angle.
     *
     * @param angle angle en degrés (négatif : vers le bas, positif : vers le haut)
     */
    public void tournerOeilGauche(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        // Un déplacement ne se convertit pas comme une position : le rapport de la tringlerie
        // varie, donc deux degrés d'œil ne valent pas le même nombre de degrés de servo selon
        // l'endroit de la course. D'où l'aller-retour par l'angle courant, plutôt qu'un rotate()
        // qui ajouterait un delta d'organe à une position moteur.
        double courant = transmissionOeilGauche.depuisMoteur(moteurOeilGauche.getPositionReelle());
        positionnerOeilGauche(courant + angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne l'oeil gauche à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     *
     * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilGauche(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
        if (positionRelative >= phidgetsConfig.eyePositionMin() && positionRelative <= phidgetsConfig.eyePositionMax()) {
            double positionMoteur = transmissionOeilGauche.versMoteur(positionRelative);
            moteurOeilGauche.setPositionCible(positionMoteur,
                    enUnitesMoteur(vitesse), enUnitesMoteur(acceleration), waitForPosition);
        }
    }

    /** Stoppe le mouvement de l'oeil gauche. */
    public void stopperOeilGauche() {
        moteurOeilGauche.stop();
        mouvementsOeilGaucheEnCours = MOUVEMENTS_OEIL.STOPPER;
    }

    /** Tourne l'oeil droit vers le bas sans s'arrêter. */
    public void tournerOeilDroitVersBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.TOURNER_BAS) {
            mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.TOURNER_BAS;
            moteurOeilDroit.forward(enUnitesMoteur(vitesse), enUnitesMoteur(acceleration), waitForPosition);
        }
    }

    /** Tourne l'oeil droit vers le haut sans s'arrêter. */
    public void tournerOeilDroitVersHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsOeilDroitEnCours != MOUVEMENTS_OEIL.TOURNER_HAUT) {
            mouvementsOeilDroitEnCours = MOUVEMENTS_OEIL.TOURNER_HAUT;
            moteurOeilDroit.backward(enUnitesMoteur(vitesse), enUnitesMoteur(acceleration), waitForPosition);
        }
    }

    /**
     * Tourne l'oeil droit d'un certain angle.
     *
     * @param angle angle en degrés (négatif : en bas, positif : en haut)
     */
    public void tournerOeilDroit(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        // Cf. tournerOeilGauche : un delta d'organe n'est pas un delta moteur.
        double courant = transmissionOeilDroit.depuisMoteur(moteurOeilDroit.getPositionReelle());
        positionnerOeilDroit(courant + angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne l'oeil droit à une position précise (0 : horizontal, min bas : -45, max haut : 15).
     *
     * @param positionRelative position en degrés (0 : horizontal, min bas : -45, max haut : 15)
     */
    public void positionnerOeilDroit(double positionRelative, Double vitesse, Double acceleration, boolean waitForPosition) {
        if (positionRelative >= phidgetsConfig.eyePositionMin() && positionRelative <= phidgetsConfig.eyePositionMax()) {
            double positionMoteur = transmissionOeilDroit.versMoteur(positionRelative);
            moteurOeilDroit.setPositionCible(positionMoteur,
                    enUnitesMoteur(vitesse), enUnitesMoteur(acceleration), waitForPosition);
        }
    }

    /** Stoppe le mouvement de l'oeil droit. */
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
        final double min = phidgetsConfig.eyePositionMin();
        final double max = phidgetsConfig.eyePositionMax();

        // Position relative RÉELLE courante des deux yeux (bornée à la plage utile pour absorber
        // le bruit capteur). Aucun état figé : le roulis s'applique par-dessus la position du moment.
        double positionGauche = clamp(transmissionOeilGauche.depuisMoteur(moteurOeilGauche.getPositionReelle()), min, max);
        double positionDroit = clamp(transmissionOeilDroit.depuisMoteur(moteurOeilDroit.getPositionReelle()), min, max);

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

    /** Borne une valeur dans l'intervalle [min, max]. */
    private static double clamp(double valeur, double min, double max) {
        return Math.max(min, Math.min(max, valeur));
    }

    /**
     * Traduit une vitesse ou une accélération d'œil en unités moteur, au gain du neutre.
     * <p>
     * Nécessaire depuis que les positions sont en degrés d'œil : le contrôleur écrête les
     * consignes de vitesse contre des bornes lues sur le servo, qui sont en unités moteur.
     * <p>
     * <b>Un gain fixe, et non celui de l'angle visé.</b> Échantillonner le gain à la cible
     * paraissait plus juste — c'est ce que faisait la première version — et ça a rendu les
     * animations saccadées, essai du 2026-09-04. Deux raisons, qui se cumulent :
     * <ul>
     *   <li>{@code LecteurAnimation} renvoie vitesse et accélération <b>à chaque tour</b>, en
     *   comptant sur le dédoublonnage de {@link PhidgetsServoMotor}, qui compare à l'égalité
     *   stricte. Un gain qui suit la cible change à chaque échantillon : le dédoublonnage ne joue
     *   plus, et ce sont quatre écritures USB de plus par tour — environ 48 ms sur un budget de
     *   100, au débit mesuré au banc ;</li>
     *   <li>l'accélération tombait de 60 à 29 °/s² en haut de course, là où le rapport est le plus
     *   fort. Le servo ne suivait plus l'échantillonnage à 10 Hz, traînait, puis rattrapait.</li>
     * </ul>
     * L'approximation est donc plus grossière qu'avant, et c'est délibéré : la vitesse n'a jamais
     * été un contrat ici, seulement un confort, et pendant une animation ce n'est pas elle qui
     * façonne le mouvement mais la cadence des consignes. Une vitesse vraiment constante d'œil
     * demanderait de reparamétrer la trajectoire, pas de bricoler une limite.
     */
    private static Double enUnitesMoteur(Double valeur) {
        return valeur == null ? null : valeur * GAIN_AU_NEUTRE;
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
        double reposOeilGauche = transmissionOeilGauche.versMoteur(reposRelatif);
        double reposOeilDroit = transmissionOeilDroit.versMoteur(reposRelatif);
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

    /** Indique si le moteur a atteint la position cible, à {@link #TOLERANCE_POSITION} près. */
    private static boolean estAtteinte(PhidgetsServoMotor moteur, double cible) {
        return Math.abs(moteur.getPositionReelle() - cible) < TOLERANCE_POSITION;
    }

    /**
     * Position de repos relative configurée (négatif = yeux baissés), ou position zéro si
     * absente. Bornée aux limites relatives élargies de {@link #MARGE_REPOS} — le travail
     * se fait en relatif car les bornes absolues des deux yeux sont inversées.
     */
    private double positionReposRelative() {
        Double reposConfigure = phidgetsConfig.eyeRestPosition();
        double repos = reposConfigure != null ? reposConfigure : 0;
        return Math.max(phidgetsConfig.eyePositionMin() - MARGE_REPOS,
                Math.min(phidgetsConfig.eyePositionMax() + MARGE_REPOS, repos));
    }

    /** Remet les yeux à leur position par défaut. */
    private void reset() {
        moteurOeilDroit.setPositionCible(phidgetsConfig.eyeRightMotorInitialPosition(), null, null, false);
        moteurOeilGauche.setPositionCible(phidgetsConfig.eyeLeftMotorInitialPosition(), null, null, false);
    }

    /**
     * Angle d'œil de la position de <b>démarrage</b>, celle que {@link #reset()} rejoint.
     * <p>
     * Ce n'est pas zéro, et c'est tout l'objet de la séparation {@code .init} / {@code .zero} : le
     * zéro est la coque de niveau, une mesure, tandis que le démarrage est une posture choisie —
     * yeux légèrement baissés. Le HUD en a besoin pour que son bouton « recentrer » ramène le robot
     * là où il démarre, et non à un angle qu'il ne prend jamais.
     * <p>
     * {@code null} tant que l'organe n'est pas démarré : la transmission n'est figée qu'à
     * {@code initialiser()}.
     */
    public Double getAngleInitialOeilGauche() {
        return transmissionOeilGauche == null ? null
                : transmissionOeilGauche.depuisMoteur(phidgetsConfig.eyeLeftMotorInitialPosition());
    }

    /** Angle d'œil de la position de démarrage de l'œil droit (cf. {@link #getAngleInitialOeilGauche}). */
    public Double getAngleInitialOeilDroit() {
        return transmissionOeilDroit == null ? null
                : transmissionOeilDroit.depuisMoteur(phidgetsConfig.eyeRightMotorInitialPosition());
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
        return reelle == null ? null : transmissionOeilGauche.depuisMoteur(reelle);
    }

    /** Position relative courante de l'œil droit, ou {@code null} si l'organe n'est pas démarré. */
    public Double getPositionOeilDroitCourante() {
        if (!running || moteurOeilDroit == null) {
            return null;
        }
        Double reelle = moteurOeilDroit.getPositionReelleOuNull();
        return reelle == null ? null : transmissionOeilDroit.depuisMoteur(reelle);
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
        double positionRelativeOeilGauche = transmissionOeilGauche.depuisMoteur(moteurOeilGauche.getPositionReelle());
        double positionRelativeOeilDroit = transmissionOeilDroit.depuisMoteur(moteurOeilDroit.getPositionReelle());
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
