package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.transmission.Transmission;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_MONTER_DESCENDRE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_PANORAMIQUE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_INCLINAISON;
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
 * Classe représentant le cou du robot.
 * <p>
 * Cycle de vie géré par {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#ACTIONNEURS_AVEC_MOTEUR}. Les moteurs ne sont créés
 * et engagés qu'au {@code start()} — pas à la construction du bean — pour respecter
 * l'ordre des phases (moteurs derniers démarrés, premiers arrêtés).
 *
 * @author Java Developer
 */
@Component
public class Cou extends AbstractOrgane implements SmartLifecycle, OrganeSurveille {

    /** Moteur "Panoramique". */
    private PhidgetsServoMotor moteurPanoramique;

    /** Moteur "Inclinaison". */
    private PhidgetsServoMotor moteurInclinaison;

    /** Moteur "Monter - Descendre". */
    private PhidgetsServoMotor moteurMonterDescendre;

    /**
     * Conversion angle de tête ↔ position moteur, une par axe.
     * <p>
     * Les trois sont décroissantes — {@code init - position}, formule qui était recopiée neuf
     * fois dans cette classe, trois fois à l'aller et six fois au retour.
     * <p>
     * Rapport de 1 pour l'instant, et ce n'est pas le vrai : le panoramique passe par un couple
     * de pignons et l'inclinaison par une boîte Stingray-2. La fiche des servos et le RCC1000
     * donnent 1,583 °/unité pour le premier et 4,75 pour la seconde. Les poser ici ne suffit
     * pas — {@code robot.regard.*.commande.par.degre.vu} les compense déjà, mêlés à un champ de
     * vision de caméra probablement surestimé de 40 %. Les trois se corrigent ensemble ou pas
     * du tout, sinon la tête se met à dépasser. Le monter/descendre passe par une tringlerie
     * qui n'a jamais été mesurée : son rapport de 1 est une ignorance, pas un relevé.
     */
    private Transmission transmissionPanoramique;

    private Transmission transmissionInclinaison;

    private Transmission transmissionMonterDescendre;

    /** Phidgets configuration. */
    private PhidgetsConfig phidgetsConfig;

    // Volatiles : écrits par les threads d'évènements, lus par le watchdog (enMouvement()).
    private volatile MOUVEMENTS_PANORAMIQUE mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
    private volatile MOUVEMENTS_INCLINAISON mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
    private volatile MOUVEMENTS_MONTER_DESCENDRE mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(Cou.class);

    /** Tolérance (en degrés) pour considérer une position servo atteinte. */
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
     * de 50 ms, et le cou paraîtrait immobile par intermittence en plein mouvement.
     */
    private static final long REMANENCE_MOUVEMENT_MS = 500;

    /** Instant de la dernière variation de position constatée. */
    private volatile long instantDerniereVariation = 0L;

    /** Flag de démarrage de l'organe (cycle de vie Spring). */
    private volatile boolean running = false;

    /**
     * Arrêt d'urgence en cours : tout ordre de mouvement est refusé jusqu'au réarmement
     * (cf. {@link fr.roboteek.robot.securite.ArretUrgence}). Mémorisé ici plutôt que lu dans un
     * service partagé : l'organe ne connaît que l'évènement, comme pour tout le reste.
     */
    private volatile boolean arretUrgence = false;

    /** Constructeur. */
    public Cou() {
        super();
        phidgetsConfig = phidgetsConfig();
    }

    @Override
    public void initialiser() {
        // Les transmissions se figent ici, en même temps que les moteurs : relire la position
        // initiale à chaque conversion, comme avant, la laissait diverger des butées du moteur,
        // elles fixées une seule fois. Un étalonnage à chaud ne s'appliquait donc qu'à moitié.
        transmissionPanoramique = Transmission.affine(phidgetsConfig.neckLeftRightMotorInitialPosition(), -1);
        transmissionInclinaison = Transmission.affine(phidgetsConfig.neckTiltMotorInitialPosition(), -1);
        transmissionMonterDescendre = Transmission.affine(phidgetsConfig.neckUpDownMotorInitialPosition(), -1);

        // Création des moteurs au démarrage de la phase (et non à la construction du bean).
        // Chaque servo est engagé à sa position de repos — sa position physique probable,
        // laissée par le dernier arrêt — pour éviter le saut à pleine vitesse à l'engagement ;
        // le retour à la position initiale se fait ensuite en rampe douce (reset()).
        moteurPanoramique = new PhidgetsServoMotor(
                phidgetsConfig.neckLeftRightMotorIndex(),
                phidgetsConfig.neckLeftRightMotorInitialPosition(),
                phidgetsConfig.neckLeftRightMotorMinPosition(),
                phidgetsConfig.neckLeftRightMotorMaxPosition(),
                phidgetsConfig.neckLeftRightMotorSpeed(),
                phidgetsConfig.neckLeftRightMotorAcceleration(),
                positionReposPanoramique()
        );
        moteurInclinaison = new PhidgetsServoMotor(
                phidgetsConfig.neckTiltMotorIndex(),
                phidgetsConfig.neckTiltMotorInitialPosition(),
                phidgetsConfig.neckTiltMotorMinPosition(),
                phidgetsConfig.neckTiltMotorMaxPosition(),
                phidgetsConfig.neckTiltMotorSpeed(),
                phidgetsConfig.neckTiltMotorAcceleration(),
                positionReposInclinaison()
        );
        moteurMonterDescendre = new PhidgetsServoMotor(
                phidgetsConfig.neckUpDownMotorIndex(),
                phidgetsConfig.neckUpDownMotorInitialPosition(),
                phidgetsConfig.neckUpDownMotorMinPosition(),
                phidgetsConfig.neckUpDownMotorMaxPosition(),
                phidgetsConfig.neckUpDownMotorSpeed(),
                phidgetsConfig.neckUpDownMotorAcceleration(),
                positionReposMonterDescendre()
        );

        moteurPanoramique.setEngaged(true);
        moteurPanoramique.setSpeedRampingState(true);

        moteurInclinaison.setEngaged(true);
        moteurInclinaison.setSpeedRampingState(true);

        moteurMonterDescendre.setEngaged(true);
        moteurMonterDescendre.setSpeedRampingState(true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reset();
        logger.info("Cou : fin initialisation");
    }

    /** Tourne la tête à gauche sans s'arrêter. */
    public void tournerAGauche(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE) {
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE;
            moteurPanoramique.backward(vitesse, acceleration, waitForPosition);
        } else {
            moteurPanoramique.setVitesse(vitesse);
        }
    }

    /** Tourne la tête à droite sans s'arrêter. */
    public void tournerADroite(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE) {
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE;
            moteurPanoramique.forward(vitesse, acceleration, waitForPosition);
        } else {
            moteurPanoramique.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête sur le plan "Gauche / Droite" d'un certain angle.
     *
     * @param angle angle en degrés (négatif : à droite, positif : à gauche)
     */
    public void tournerTeteGaucheDroite(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        // Une consigne d'angle ou de position laisse le servo ARRÊTÉ sur sa cible : le mouvement
        // continu éventuellement en cours est terminé, et le drapeau doit le dire. Sans ça, un
        // ordre du regard reçu pendant que le joystick était maintenu laissait le drapeau sur
        // TOURNER_DROITE, et les ordres suivants du joystick — jugés « déjà en cours » — ne
        // faisaient plus que régler une vitesse : la tête restait plantée jusqu'à ce qu'on
        // recentre le stick. Le watchdog n'y perd rien, enMouvement() couvre déjà les consignes
        // de position par la variation constatée.
        mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
        // ATTENTION, convention opposée à positionnerTeteGaucheDroite : rotate() ajoute à la
        // position MOTEUR, alors que positionner() inverse le signe. Un angle positif ne fait
        // donc pas tourner la tête du même côté selon la porte qu'on emprunte. Les deux marchent
        // parce que personne ne les compare : le regard, seul appelant de rotate(), a eu son
        // sens étalonné sur le robot (robot.regard.panoramique.sens.inverse), et ce réglage
        // absorbe l'écart sans le nommer — exactement comme commande.par.degre.vu absorbe le
        // rapport de transmission. Faire passer ce delta par la transmission redresserait le
        // signe, mais changerait le mouvement : ça se fait avec le vrai rapport et une
        // validation sur le robot, pas ici. Idem tournerTeteHautBas et monterDescendreTete.
        moteurPanoramique.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Gauche / Droite" à une position précise (0 : à gauche, 180 : à droite).
     *
     * @param position position en degrés (0 : à gauche, 180 : à droite)
     */
    public void positionnerTeteGaucheDroite(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = transmissionPanoramique.versMoteur(position);
        if (positionMoteur >= moteurPanoramique.getPositionMin() && positionMoteur <= moteurPanoramique.getPositionMax()) {
            logger.debug("POS_GD = {}", positionMoteur);
        // Consigne atteinte, servo arrêté : plus aucun mouvement continu en cours (cf.
        // tournerTeteGaucheDroite).
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
            moteurPanoramique.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /** Stoppe le mouvement de la tête sur le plan "Gauche / Droite". */
    public void stopperTeteGaucheDroite() {
        moteurPanoramique.stop();
        mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
    }

    /** Tourne la tête en bas sans s'arrêter. */
    public void tournerEnBas(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsInclinaisonEnCours != MOUVEMENTS_INCLINAISON.TOURNER_BAS) {
            logger.debug("BAS");
            mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.TOURNER_BAS;
            moteurInclinaison.forward(vitesse, acceleration, waitForPosition);
        } else {
            logger.debug("BAS VITESSE");
            moteurInclinaison.setVitesse(vitesse);
        }
    }

    /** Tourne la tête en haut sans s'arrêter. */
    public void tournerEnHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsInclinaisonEnCours != MOUVEMENTS_INCLINAISON.TOURNER_HAUT) {
            logger.debug("HAUT");
            mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.TOURNER_HAUT;
            moteurInclinaison.backward(vitesse, acceleration, waitForPosition);
        } else {
            logger.debug("HAUT VITESSE");
            moteurInclinaison.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête sur le plan "Haut / Bas" d'un certain angle.
     *
     * @param angle angle en degrés (négatif : en bas, positif : en haut)
     */
    public void tournerTeteHautBas(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        // Consigne atteinte, servo arrêté : plus aucun mouvement continu en cours (cf.
        // tournerTeteGaucheDroite).
        mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
        moteurInclinaison.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Haut / Bas" à une position précise (0 : en bas, 180 : en haut).
     *
     * @param position position en degrés (0 : en bas, 180 : en haut)
     */
    public void positionnerTeteHautBas(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = transmissionInclinaison.versMoteur(position);
        if (positionMoteur >= moteurInclinaison.getPositionMin() && positionMoteur <= moteurInclinaison.getPositionMax()) {
            logger.debug("POS_HB = {}", positionMoteur);
        // Consigne atteinte, servo arrêté : plus aucun mouvement continu en cours (cf.
        // tournerTeteGaucheDroite).
            mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
            moteurInclinaison.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /** Stoppe le mouvement de la tête sur le plan "Haut / Bas". */
    public void stopperTeteHautBas() {
        logger.debug("STOP HAUT BAS");
        moteurInclinaison.stop();
        mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
    }

    /** Descend la tête sans s'arrêter. */
    public void descendre(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsMonterDescendreEnCours != MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE) {
            logger.debug("DESCENDRE");
            mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE;
            moteurMonterDescendre.forward(vitesse, acceleration, waitForPosition);
        } else {
            logger.debug("DESCENDRE VITESSE");
            moteurMonterDescendre.setVitesse(vitesse);
        }
    }

    /** Monte la tête sans s'arrêter. */
    public void monter(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsMonterDescendreEnCours != MOUVEMENTS_MONTER_DESCENDRE.MONTER) {
            logger.debug("MONTER");
            mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.MONTER;
            moteurMonterDescendre.backward(vitesse, acceleration, waitForPosition);
        } else {
            logger.debug("MONTER VITESSE");
            moteurMonterDescendre.setVitesse(vitesse);
        }
    }

    /**
     * Monte ou descend la tête d'un certain angle.
     *
     * @param angle angle en degrés (négatif : descend, positif : monte)
     */
    public void monterDescendreTete(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        // Consigne atteinte, servo arrêté : plus aucun mouvement continu en cours (cf.
        // tournerTeteGaucheDroite).
        mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;
        moteurMonterDescendre.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Monte ou descend la tête à une position précise (0 : en bas, 180 : en haut).
     *
     * @param position position en degrés (0 : en bas, 180 : en haut)
     */
    public void positionnerTeteMonterDescendre(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = transmissionMonterDescendre.versMoteur(position);
        if (positionMoteur >= moteurMonterDescendre.getPositionMin() && positionMoteur <= moteurMonterDescendre.getPositionMax()) {
            logger.debug("POS_MD = {}", positionMoteur);
        // Consigne atteinte, servo arrêté : plus aucun mouvement continu en cours (cf.
        // tournerTeteGaucheDroite).
            mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;
            moteurMonterDescendre.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /** Stoppe le mouvement de la tête sur le plan "Monter - Descendre". */
    public void stopperTeteMonterDescendre() {
        logger.debug("STOP MonterDescendre");
        moteurMonterDescendre.stop();
        mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;
    }

    /**
     * Intercepte les évènements de mouvements.
     *
     * @param mouvementCouEvent évènement de mouvements
     */
    @EventListener
    @Async(RobotEventsConfig.COU_EVENT_EXECUTOR)
    public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        if (!running || arretUrgence) {
            return;
        }
        logger.debug("COU : Event = {}", mouvementCouEvent);
        if (mouvementCouEvent.getPositionPanoramique() != MouvementCouEvent.POSITION_NEUTRE) {
            // TODO ne pas mettre en synchrone si Haut/Bas en synchrone ==> A corriger
            positionnerTeteGaucheDroite(mouvementCouEvent.getPositionPanoramique(), mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getAnglePanoramique() != MouvementCouEvent.ANGLE_NEUTRE) {
            // TODO ne pas mettre en synchrone si Haut/Bas en synchrone ==> A corriger
            tournerTeteGaucheDroite(mouvementCouEvent.getAnglePanoramique(), mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementPanoramique() != null) {
            if (mouvementCouEvent.getMouvementPanoramique() == MOUVEMENTS_PANORAMIQUE.STOPPER) {
                stopperTeteGaucheDroite();
            } else if (mouvementCouEvent.getMouvementPanoramique() == MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE) {
                tournerAGauche(mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
            } else if (mouvementCouEvent.getMouvementPanoramique() == MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE) {
                tournerADroite(mouvementCouEvent.getVitessePanoramique(), mouvementCouEvent.getAccelerationPanoramique(), mouvementCouEvent.isSynchrone());
            }
        }
        if (mouvementCouEvent.getPositionInclinaison() != MouvementCouEvent.POSITION_NEUTRE) {
            positionnerTeteHautBas(mouvementCouEvent.getPositionInclinaison(), mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getAngleInclinaison() != MouvementCouEvent.ANGLE_NEUTRE) {
            tournerTeteHautBas(mouvementCouEvent.getAngleInclinaison(), mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementInclinaison() != null) {
            if (mouvementCouEvent.getMouvementInclinaison() == MOUVEMENTS_INCLINAISON.STOPPER) {
                stopperTeteHautBas();
            } else if (mouvementCouEvent.getMouvementInclinaison() == MOUVEMENTS_INCLINAISON.TOURNER_HAUT) {
                tournerEnHaut(mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
            } else if (mouvementCouEvent.getMouvementInclinaison() == MOUVEMENTS_INCLINAISON.TOURNER_BAS) {
                tournerEnBas(mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
            }
        }
        if (mouvementCouEvent.getPositionMonterDescendre() != MouvementCouEvent.POSITION_NEUTRE) {
            positionnerTeteMonterDescendre(mouvementCouEvent.getPositionMonterDescendre(), mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getAngleMonterDescendre() != MouvementCouEvent.ANGLE_NEUTRE) {
            monterDescendreTete(mouvementCouEvent.getAngleMonterDescendre(), mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
        } else if (mouvementCouEvent.getMouvementMonterDescendre() != null) {
            if (mouvementCouEvent.getMouvementMonterDescendre() == MOUVEMENTS_MONTER_DESCENDRE.STOPPER) {
                stopperTeteMonterDescendre();
            } else if (mouvementCouEvent.getMouvementMonterDescendre() == MOUVEMENTS_MONTER_DESCENDRE.MONTER) {
                monter(mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
            } else if (mouvementCouEvent.getMouvementMonterDescendre() == MOUVEMENTS_MONTER_DESCENDRE.DESCENDRE) {
                descendre(mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
            }
        }
    }

    /**
     * Arrêt d'urgence : coupe les trois moteurs du cou sur-le-champ.
     * <p>
     * Volontairement <b>sans</b> {@code @Async}, contrairement aux mouvements : le traitement a
     * lieu sur le thread qui publie, sans passer par une file d'exécution qui peut être occupée à
     * dérouler des mouvements. Le drapeau est levé <b>avant</b> de couper, pour qu'un ordre déjà
     * en attente dans l'exécuteur soit refusé au lieu de relancer un moteur qu'on vient d'arrêter.
     * <p>
     * Les servos ne sont pas désengagés : sans couple, la tête tomberait
     * (cf. {@link fr.roboteek.robot.securite.ArretUrgence}).
     */
    @EventListener
    public void handleArretUrgenceEvent(ArretUrgenceEvent evenement) {
        arretUrgence = evenement.isActif();
        if (!running || !evenement.isActif()) {
            return;
        }
        logger.warn("Cou : arrêt d'urgence, coupure des moteurs");
        stopperTeteGaucheDroite();
        stopperTeteHautBas();
        stopperTeteMonterDescendre();
    }

    @Override
    public void arreter() {
        // Rejoint une position de repos mécaniquement stable (tête baissée) AVANT le
        // désengagement des servos, pour éviter que la tête ne tombe d'un coup.
        // Positions réglables dans robot.properties (clés *.position.rest) ; à défaut,
        // la position initiale est visée (comportement historique).
        double reposPanoramique = positionReposPanoramique();
        double reposInclinaison = positionReposInclinaison();
        double reposMonterDescendre = positionReposMonterDescendre();
        // Aide au réglage des positions de repos : placer la tête comme souhaité (manette)
        // puis relever ces valeurs dans les logs à l'arrêt
        logger.info("Positions au moment de l'arrêt : pan={} tilt={} up_down={} — cibles de repos : pan={} tilt={} up_down={}",
                moteurPanoramique.getPositionReelle(), moteurInclinaison.getPositionReelle(),
                moteurMonterDescendre.getPositionReelle(), reposPanoramique, reposInclinaison, reposMonterDescendre);
        moteurPanoramique.setPositionCible(reposPanoramique, null, null, false);
        moteurInclinaison.setPositionCible(reposInclinaison, null, null, false);
        moteurMonterDescendre.setPositionCible(reposMonterDescendre, null, null, false);
        // Attente (bornée) de l'arrivée en position de repos, à la tolérance près
        // (une comparaison stricte de doubles n'est jamais vraie et épuisait les 5 s)
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite
                && !(estAtteinte(moteurPanoramique, reposPanoramique)
                && estAtteinte(moteurInclinaison, reposInclinaison)
                && estAtteinte(moteurMonterDescendre, reposMonterDescendre))) {
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
        moteurPanoramique.stop();
        moteurInclinaison.stop();
        moteurMonterDescendre.stop();
        moteurPanoramique.setSpeedRampingState(true);
        moteurInclinaison.setSpeedRampingState(true);
        moteurMonterDescendre.setSpeedRampingState(true);
        moteurPanoramique.setEngaged(false);
        moteurInclinaison.setEngaged(false);
        moteurMonterDescendre.setEngaged(false);
        moteurPanoramique.close();
        moteurInclinaison.close();
        moteurMonterDescendre.close();
    }

    /**
     * Position de repos configurée, ou position de secours si absente. Bornée aux limites
     * de mouvement élargies de {@link #MARGE_REPOS} (les butées logicielles protègent le
     * mouvement ; l'appui stable de repos peut être juste au-delà).
     */
    private static double positionRepos(Double positionConfiguree, double positionParDefaut, double positionMin, double positionMax) {
        double position = positionConfiguree != null ? positionConfiguree : positionParDefaut;
        return Math.max(positionMin - MARGE_REPOS, Math.min(positionMax + MARGE_REPOS, position));
    }

    private double positionReposPanoramique() {
        return positionRepos(phidgetsConfig.neckLeftRightMotorRestPosition(), phidgetsConfig.neckLeftRightMotorInitialPosition(),
                phidgetsConfig.neckLeftRightMotorMinPosition(), phidgetsConfig.neckLeftRightMotorMaxPosition());
    }

    private double positionReposInclinaison() {
        return positionRepos(phidgetsConfig.neckTiltMotorRestPosition(), phidgetsConfig.neckTiltMotorInitialPosition(),
                phidgetsConfig.neckTiltMotorMinPosition(), phidgetsConfig.neckTiltMotorMaxPosition());
    }

    private double positionReposMonterDescendre() {
        return positionRepos(phidgetsConfig.neckUpDownMotorRestPosition(), phidgetsConfig.neckUpDownMotorInitialPosition(),
                phidgetsConfig.neckUpDownMotorMinPosition(), phidgetsConfig.neckUpDownMotorMaxPosition());
    }

    /** Indique si le moteur a atteint la position cible, à {@link #TOLERANCE_POSITION} près. */
    private static boolean estAtteinte(PhidgetsServoMotor moteur, double cible) {
        return Math.abs(moteur.getPositionReelle() - cible) < TOLERANCE_POSITION;
    }

    /** Remet la tête à sa position par défaut. */
    private void reset() {
        moteurInclinaison.setPositionCible(phidgetsConfig.neckTiltMotorInitialPosition(), null, null, false);
        moteurPanoramique.setPositionCible(phidgetsConfig.neckLeftRightMotorInitialPosition(), null, null, false);
        moteurMonterDescendre.setPositionCible(phidgetsConfig.neckUpDownMotorInitialPosition(), null, null, false);
    }

    /**
     * Position panoramique courante dans le repère « logique » exposé aux clients
     * (position = init − position moteur, cf. {@link #positionnerTeteGaucheDroite}), ou
     * {@code null} si l'organe n'est pas démarré (les moteurs ne sont créés qu'au {@code start()}).
     */
    public Double getPositionPanoramiqueCourante() {
        if (!running || moteurPanoramique == null) {
            return null;
        }
        Double reelle = moteurPanoramique.getPositionReelleOuNull();
        return reelle == null ? null : transmissionPanoramique.depuisMoteur(reelle);
    }

    /**
     * Position d'inclinaison (haut / bas) courante dans le repère logique, ou {@code null}
     * si l'organe n'est pas démarré.
     */
    public Double getPositionInclinaisonCourante() {
        if (!running || moteurInclinaison == null) {
            return null;
        }
        Double reelle = moteurInclinaison.getPositionReelleOuNull();
        return reelle == null ? null : transmissionInclinaison.depuisMoteur(reelle);
    }

    /**
     * Position monter / descendre courante dans le repère logique, ou {@code null}
     * si l'organe n'est pas démarré.
     */
    public Double getPositionMonterDescendreCourante() {
        if (!running || moteurMonterDescendre == null) {
            return null;
        }
        Double reelle = moteurMonterDescendre.getPositionReelleOuNull();
        return reelle == null ? null : transmissionMonterDescendre.depuisMoteur(reelle);
    }

    /**
     * Publie la position courante des articulations du cou en télémétrie, pour que les curseurs
     * de contrôle suivent en direct les mouvements <b>physiques</b> du robot (manette, animations,
     * comportements) et pas seulement les commandes qu'ils envoient eux-mêmes. Uniquement quand
     * une position varie sensiblement — rien n'est émis à l'arrêt, ni tant que l'organe n'est pas
     * démarré (positions indisponibles). Mêmes identifiants que {@code /api/organes}.
     */
    @Scheduled(fixedRate = 50)
    public void diffuserTelemetrie() {
        Map<String, Double> positions = new LinkedHashMap<>();
        ajouterSiPresent(positions, "pan", getPositionPanoramiqueCourante());
        ajouterSiPresent(positions, "tilt", getPositionInclinaisonCourante());
        ajouterSiPresent(positions, "upDown", getPositionMonterDescendreCourante());
        if (positions.isEmpty()) {
            return;
        }
        // Signe de vie : les trois canaux Phidget ont répondu. C'est ce relevé, et non le
        // drapeau de cycle de vie, qui atteste que le cou est encore joignable.
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
        double positionGaucheDroite = transmissionPanoramique.depuisMoteur(moteurPanoramique.getPositionReelle());
        double positionHautBas = transmissionInclinaison.depuisMoteur(moteurInclinaison.getPositionReelle());
        double positionMonterDescendre = transmissionMonterDescendre.depuisMoteur(moteurMonterDescendre.getPositionReelle());
        logger.debug("COU\tGD = {}\tHB = {}\tMD = {}", positionGaucheDroite, positionHautBas, positionMonterDescendre);
    }

    @Override
    public void start() {
        initialiser();
        running = true;
        logger.info("Cou démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("Cou arrêté");
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
        return "cou";
    }

    @Override
    public String libelleOrgane() {
        return "Cou";
    }

    @Override
    public NatureOrgane nature() {
        return NatureOrgane.ACTIONNEUR;
    }

    @Override
    public boolean enService() {
        return running;
    }

    /** Le cou exécute des mouvements : son silence est un motif d'arrêt d'urgence. */
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
        return mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.STOPPER
                || mouvementsInclinaisonEnCours != MOUVEMENTS_INCLINAISON.STOPPER
                || mouvementsMonterDescendreEnCours != MOUVEMENTS_MONTER_DESCENDRE.STOPPER
                || System.currentTimeMillis() - instantDerniereVariation < REMANENCE_MOUVEMENT_MS;
    }

}
