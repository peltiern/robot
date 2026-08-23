package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementRoueEvent;
import fr.roboteek.robot.systemenerveux.spring.RobotEventsConfig;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.phidgets.PhidgetDCMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

import static fr.roboteek.robot.configuration.Configurations.phidgetsConfig;

/**
 * Conduite différentielle (chenilles) du robot.
 * <p>
 * Cycle de vie géré par {@link SmartLifecycle} en phase
 * {@link RobotLifecyclePhases#ACTIONNEURS_AVEC_MOTEUR}. Les moteurs ne sont créés
 * et engagés qu'au {@code start()} — pas à la construction du bean — pour respecter
 * l'ordre des phases (moteurs derniers démarrés, premiers arrêtés).
 */
@Component
public class ConduiteDifferentielle extends AbstractOrgane implements SmartLifecycle {

    /** Moteur pour la roue gauche. */
    private PhidgetDCMotor moteurGauche;

    /** Moteur pour la roue droite. */
    private PhidgetDCMotor moteurDroit;

    /** Phidgets configuration. */
    private PhidgetsConfig phidgetsConfig;

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(ConduiteDifferentielle.class);

    /** Flag de démarrage de l'organe (cycle de vie Spring). */
    private volatile boolean running = false;

    /**
     * Arrêt d'urgence en cours : tout ordre de mouvement est refusé jusqu'au réarmement
     * (cf. {@link fr.roboteek.robot.securite.ArretUrgence}).
     */
    private volatile boolean arretUrgence = false;

    /**
     * Période de la sonde des moteurs (voir {@link #sonderMoteurs()}). Plus lâche que la télémétrie
     * du cou et des yeux : rien ne s'affiche à cette cadence, elle ne sert qu'à la surveillance.
     */
    private static final long PERIODE_SONDE_MS = 200;

    /**
     * Vitesse (en valeur absolue) au-delà de laquelle on considère qu'une roue tourne vraiment.
     * Non nul : la vitesse remontée par le contrôleur n'est pas rigoureusement zéro à l'arrêt.
     */
    private static final double SEUIL_VITESSE_MOUVEMENT = 0.02;

    /** Surveillance de chaque chenille, une panne pouvant parfaitement n'en toucher qu'une. */
    private final ChenilleSurveillee chenilleGauche =
            new ChenilleSurveillee("chenille-gauche", "Chenille gauche", this::isRunning);
    private final ChenilleSurveillee chenilleDroite =
            new ChenilleSurveillee("chenille-droite", "Chenille droite", this::isRunning);

    /** Constructeur. */
    public ConduiteDifferentielle() {
        super();
        phidgetsConfig = phidgetsConfig();
    }

    public void avancer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        logger.debug("vitesseFormatee = {}, accelerationFormatee = {}", vitesseFormatee, accelerationFormatee);
        moteurGauche.forward(vitesseFormatee, accelerationFormatee);
        moteurDroit.forward(vitesseFormatee, accelerationFormatee);
    }

    public void reculer(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        moteurGauche.forward(-vitesseFormatee, accelerationFormatee);
        moteurDroit.forward(-vitesseFormatee, accelerationFormatee);
    }

    public void pivoterAGauche(Double vitesse, Double acceleration) {
        tournerRoueGauche(-vitesse, acceleration);
        tournerRoueDroite(vitesse, acceleration);
    }

    public void pivoterADroite(Double vitesse, Double acceleration) {
        tournerRoueGauche(vitesse, acceleration);
        tournerRoueDroite(-vitesse, acceleration);
    }

    public void differentiel(Double vitesseRoueGauche, Double accelerationRoueGauche, Double vitesseRoueDroite, Double accelerationRoueDroite) {
        tournerRoueGauche(vitesseRoueGauche, accelerationRoueGauche);
        tournerRoueDroite(vitesseRoueDroite, accelerationRoueDroite);
    }

    public void tournerRoueGauche(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        moteurGauche.forward(vitesseFormatee, accelerationFormatee);
    }

    public void tournerRoueDroite(Double vitesse, Double acceleration) {
        double vitesseFormatee = toVitesse(vitesse);
        double accelerationFormatee = toAcceleration(acceleration);
        moteurDroit.forward(vitesseFormatee, accelerationFormatee);
    }

    public void stopperRoues() {
        moteurGauche.stop();
        moteurDroit.stop();
    }

    /**
     * Intercepte les évènements de mouvements.
     *
     * @param mouvementRoueEvent évènement de mouvements
     */
    @EventListener
    @Async(RobotEventsConfig.ROBOT_EVENT_EXECUTOR)
    public void handleMouvementRoueEvent(MouvementRoueEvent mouvementRoueEvent) {
        if (!running || arretUrgence) {
            return;
        }
        logger.debug("{}", mouvementRoueEvent);
        if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.AVANCER) {
            avancer(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.RECULER) {
            reculer(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_GAUCHE) {
            pivoterAGauche(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.PIVOTER_DROIT) {
            pivoterADroite(mouvementRoueEvent.getVitesseGlobale(), mouvementRoueEvent.getAccelerationGlobale());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.DIFFERENTIEL) {
            differentiel(mouvementRoueEvent.getVitesseRoueGauche(), mouvementRoueEvent.getAccelerationRoueGauche(),
                    mouvementRoueEvent.getVitesseRoueDroite(), mouvementRoueEvent.getAccelerationRoueDroite());
        } else if (mouvementRoueEvent.getMouvementRoue() == MouvementRoueEvent.MOUVEMENTS_ROUE.STOPPER) {
            stopperRoues();
        }
    }

    /**
     * Arrêt d'urgence : coupe les deux moteurs de chenilles sur-le-champ. Voir
     * {@code Cou.handleArretUrgenceEvent} pour le pourquoi du traitement synchrone.
     * <p>
     * C'est l'organe le plus concerné : un robot qui roule est la seule chose ici qui puisse
     * partir loin toute seule.
     */
    @EventListener
    public void handleArretUrgenceEvent(ArretUrgenceEvent evenement) {
        arretUrgence = evenement.isActif();
        if (!running || !evenement.isActif()) {
            return;
        }
        logger.warn("ConduiteDifferentielle : arrêt d'urgence, coupure des moteurs");
        stopperRoues();
    }

    @Override
    public void initialiser() {
        reset();
    }

    @Override
    public void arreter() {
        reset();
        moteurGauche.close();
        moteurDroit.close();
    }

    /** Arrête les moteurs. */
    private void reset() {
        moteurGauche.stop();
        moteurDroit.stop();
    }

    private double toVitesse(Double vitesse) {
        return (vitesse == null ? 1 : Math.abs(vitesse) > 1 ? (int) vitesse.intValue() : vitesse) * phidgetsConfig.differentialDrivingMotorMaxSpeed();
    }

    private double toAcceleration(Double acceleration) {
        return acceleration == null || acceleration < 0.1 || acceleration > 100 ? phidgetsConfig.differentialDrivingMotorAcceleration() : acceleration;
    }

    @Override
    public void start() {
        // Création des moteurs au démarrage de la phase (et non à la construction du bean)
        moteurGauche = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingLeftMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());
        moteurDroit = new PhidgetDCMotor(phidgetsConfig.hubSerialNumber(), phidgetsConfig.differentialDrivingRightMotorPort(), phidgetsConfig.differentialDrivingMotorAcceleration());
        initialiser();
        running = true;
        logger.info("ConduiteDifferentielle démarrée");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("ConduiteDifferentielle arrêtée");
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

    /**
     * Interroge les deux moteurs, <b>chacun pour son compte</b> : un signe de vie si le canal
     * répond, et le constat que la chenille tourne (ou non).
     * <p>
     * Contrairement au cou et aux yeux, cet organe n'a ni boucle ni télémétrie — il ne fait que
     * réagir à des évènements — et n'aurait donc rien à offrir comme preuve de vie. D'où cette
     * sonde. Elle mesure la vitesse <b>réelle</b> plutôt que de mémoriser le dernier ordre reçu :
     * c'est exactement la question que pose le watchdog avant de couper (« est-ce que quelque
     * chose bouge ? »), et une lecture qui aboutit prouve au passage que le hub Phidget répond.
     */
    @Scheduled(fixedRate = PERIODE_SONDE_MS)
    public void sonderMoteurs() {
        if (!running) {
            return;
        }
        sonder(chenilleGauche, moteurGauche);
        sonder(chenilleDroite, moteurDroit);
    }

    /**
     * Si la lecture échoue, aucun battement n'est déclaré (le canal est bien muet) et le dernier
     * constat de mouvement est <b>conservé</b> : dans le doute, une chenille qu'on croyait en marche
     * reste réputée en marche — c'est le sens prudent.
     */
    private static void sonder(ChenilleSurveillee chenille, PhidgetDCMotor moteur) {
        Double vitesse = moteur.getVitesse();
        if (vitesse == null) {
            return;
        }
        chenille.constater(Math.abs(vitesse) > SEUIL_VITESSE_MOUVEMENT);
    }

    /**
     * Beans de surveillance, ramassés par {@code RegistreSante} au même titre que les organes.
     * Déclarés ici en mode « lite » plutôt que dans une classe de configuration dédiée : ils n'ont
     * de sens qu'attachés à cette conduite, dont ils lisent le cycle de vie et les moteurs.
     */
    @Bean
    public ChenilleSurveillee chenilleGauche() {
        return chenilleGauche;
    }

    @Bean
    public ChenilleSurveillee chenilleDroite() {
        return chenilleDroite;
    }

    /**
     * Une chenille du point de vue de la surveillance.
     * <p>
     * <b>Pourquoi deux organes et non un seul.</b> Les deux chenilles ont leur moteur, donc leur
     * panne : rien ne les fait tomber ensemble. Et c'est le cas <i>dissymétrique</i> qui est
     * dangereux — un robot dont une seule chenille répond encore ne s'arrête pas, il pivote sur
     * place. Avec un organe unique, la pastille disait « Chenilles » sans dire lequel des deux
     * côtés lâchait, et la sonde abandonnait les <b>deux</b> mesures dès qu'<i>un</i> canal ne
     * répondait plus : la chenille survivante était déclarée muette alors qu'elle tournait encore.
     * <p>
     * Le découpage ne concerne que la surveillance : la conduite reste un seul organe, une chenille
     * n'ayant aucun sens sans l'autre.
     */
    public static final class ChenilleSurveillee implements OrganeSurveille {

        private final String id;
        private final String libelle;

        /** Cycle de vie de la conduite : une chenille n'est en service que si l'organe l'est. */
        private final BooleanSupplier enService;

        private volatile long dernierBattement = 0L;
        private volatile boolean enMouvement = false;

        private ChenilleSurveillee(String id, String libelle, BooleanSupplier enService) {
            this.id = id;
            this.libelle = libelle;
            this.enService = enService;
        }

        /** Relevé d'une sonde qui a abouti : le canal répond, et voici ce qu'il dit du mouvement. */
        private void constater(boolean tourne) {
            enMouvement = tourne;
            dernierBattement = System.currentTimeMillis();
        }

        @Override
        public String idOrgane() {
            return id;
        }

        @Override
        public String libelleOrgane() {
            return libelle;
        }

        @Override
        public NatureOrgane nature() {
            return NatureOrgane.ACTIONNEUR;
        }

        @Override
        public boolean enService() {
            return enService.getAsBoolean();
        }

        @Override
        public long dernierBattement() {
            return dernierBattement;
        }

        /**
         * C'est l'organe le plus concerné : un robot qui roule est la seule chose ici qui puisse
         * partir loin toute seule.
         */
        @Override
        public boolean provoqueUnMouvement() {
            return true;
        }

        @Override
        public boolean enMouvement() {
            return enMouvement;
        }
    }
}
