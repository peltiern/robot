package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.DisplayPositionEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_MONTER_DESCENDRE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_PANORAMIQUE;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent.MOUVEMENTS_INCLINAISON;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.phidgets.PhidgetsServoMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Classe représentant le cou du robot.
 * <p>
 * Migré en bean Spring : cycle de vie géré par {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#ACTIONNEURS_AVEC_MOTEUR}. Les moteurs ne sont créés
 * et engagés qu'au {@code start()} — pas à la construction du bean — pour respecter
 * l'ordre des phases (moteurs derniers démarrés, premiers arrêtés).
 *
 * @author Java Developer
 */
@Component
public class Cou extends AbstractOrgane implements SmartLifecycle {

    /**
     * Moteur "Panoramique".
     */
    private PhidgetsServoMotor moteurPanoramique;

    /**
     * Moteur "Inclinaison".
     */
    private PhidgetsServoMotor moteurInclinaison;

    /**
     * Moteur "Monter - Descendre".
     */
    private PhidgetsServoMotor moteurMonterDescendre;

    /**
     * Phidgets configuration.
     */
    private PhidgetsConfig phidgetsConfig;

    private MOUVEMENTS_PANORAMIQUE mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
    private MOUVEMENTS_INCLINAISON mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
    private MOUVEMENTS_MONTER_DESCENDRE mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.STOPPER;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(Cou.class);

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
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    /**
     * Constructeur.
     */
    public Cou() {
        super();
        phidgetsConfig = phidgetsConfig();
    }

    @Override
    public void initialiser() {
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

    /**
     * Tourne la tête à gauche sans s'arrêter.
     */
    public void tournerAGauche(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE) {
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.TOURNER_GAUCHE;
            moteurPanoramique.backward(vitesse, acceleration, waitForPosition);
        } else {
            moteurPanoramique.setVitesse(vitesse);
        }
    }

    /**
     * Tourne la tête à droite sans s'arrêter.
     */
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
        moteurPanoramique.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Gauche / Droite" à une position précise (0 : à gauche, 180 : à droite).
     *
     * @param position position en degrés (0 : à gauche, 180 : à droite)
     */
    public void positionnerTeteGaucheDroite(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = phidgetsConfig.neckLeftRightMotorInitialPosition() - position;
        if (positionMoteur >= moteurPanoramique.getPositionMin() && positionMoteur <= moteurPanoramique.getPositionMax()) {
            logger.debug("POS_GD = {}", positionMoteur);
            moteurPanoramique.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de la tête sur le plan "Gauche / Droite".
     */
    public void stopperTeteGaucheDroite() {
        moteurPanoramique.stop();
        mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
    }

    /**
     * Tourne la tête en bas sans s'arrêter.
     */
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

    /**
     * Tourne la tête en haut sans s'arrêter.
     */
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
        moteurInclinaison.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Haut / Bas" à une position précise (0 : en bas, 180 : en haut).
     *
     * @param position position en degrés (0 : en bas, 180 : en haut)
     */
    public void positionnerTeteHautBas(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = phidgetsConfig.neckTiltMotorInitialPosition() - position;
        if (positionMoteur >= moteurInclinaison.getPositionMin() && positionMoteur <= moteurInclinaison.getPositionMax()) {
            logger.debug("POS_HB = {}", positionMoteur);
            moteurInclinaison.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de la tête sur le plan "Haut / Bas".
     */
    public void stopperTeteHautBas() {
        logger.debug("STOP HAUT BAS");
        moteurInclinaison.stop();
        mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.STOPPER;
    }

    /**
     * Descend la tête sans s'arrêter.
     */
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

    /**
     * Monte la tête sans s'arrêter.
     */
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
        moteurMonterDescendre.rotate(angle, vitesse, acceleration, waitForPosition);
    }

    /**
     * Monte ou descend la tête à une position précise (0 : en bas, 180 : en haut).
     *
     * @param position position en degrés (0 : en bas, 180 : en haut)
     */
    public void positionnerTeteMonterDescendre(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = phidgetsConfig.neckUpDownMotorInitialPosition() - position;
        if (positionMoteur >= moteurMonterDescendre.getPositionMin() && positionMoteur <= moteurMonterDescendre.getPositionMax()) {
            logger.debug("POS_MD = {}", positionMoteur);
            moteurMonterDescendre.setPositionCible(positionMoteur, vitesse, acceleration, waitForPosition);
        }
    }

    /**
     * Stoppe le mouvement de la tête sur le plan "Monter - Descendre".
     */
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
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleMouvementCouEvent(MouvementCouEvent mouvementCouEvent) {
        if (!running) {
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
            tournerTeteHautBas(mouvementCouEvent.getPositionInclinaison(), mouvementCouEvent.getVitesseInclinaison(), mouvementCouEvent.getAccelerationInclinaison(), mouvementCouEvent.isSynchrone());
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
            monterDescendreTete(mouvementCouEvent.getPositionMonterDescendre(), mouvementCouEvent.getVitesseMonterDescendre(), mouvementCouEvent.getAccelerationMonterDescendre(), mouvementCouEvent.isSynchrone());
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

    /**
     * Indique si le moteur a atteint la position cible, à {@link #TOLERANCE_POSITION} près.
     */
    private static boolean estAtteinte(PhidgetsServoMotor moteur, double cible) {
        return Math.abs(moteur.getPositionReelle() - cible) < TOLERANCE_POSITION;
    }

    /**
     * Remet la tête à sa position par défaut.
     */
    private void reset() {
        moteurInclinaison.setPositionCible(phidgetsConfig.neckTiltMotorInitialPosition(), null, null, false);
        moteurPanoramique.setPositionCible(phidgetsConfig.neckLeftRightMotorInitialPosition(), null, null, false);
        moteurMonterDescendre.setPositionCible(phidgetsConfig.neckUpDownMotorInitialPosition(), null, null, false);
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
        double positionGaucheDroite = phidgetsConfig.neckLeftRightMotorInitialPosition() - moteurPanoramique.getPositionReelle();
        double positionHautBas = phidgetsConfig.neckTiltMotorInitialPosition() - moteurInclinaison.getPositionReelle();
        double positionMonterDescendre = phidgetsConfig.neckUpDownMotorInitialPosition() - moteurMonterDescendre.getPositionReelle();
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

}
