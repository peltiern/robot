package fr.roboteek.robot.organes.actionneurs;

import fr.roboteek.robot.configuration.phidgets.PhidgetsConfig;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.transmission.Transmission;
import fr.roboteek.robot.securite.NatureOrgane;
import fr.roboteek.robot.securite.OrganeSurveille;
import fr.roboteek.robot.systemenerveux.event.ArretUrgenceEvent;
import fr.roboteek.robot.systemenerveux.event.PostureDeDepartEvent;
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
     * L'inclinaison et le monter/descendre sont décroissants — {@code init - position}, formule
     * qui était recopiée neuf fois dans cette classe, trois fois à l'aller et six fois au retour.
     * Le panoramique, lui, est croissant : voir {@code initialiser()}.
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
     * <p>
     * <b>Un demi-degré, et non 0,2</b>, depuis que ces positions sont des degrés d'axe et non des
     * unités moteur : le HUD arrondit à l'entier, donc sous la moitié d'un degré rien ne change à
     * l'écran et le message est envoyé pour rien. Laisser 0,2 revenait à multiplier le débit par
     * cinq sur l'inclinaison, dont une unité moteur vaut désormais 4,97 degrés — et cette
     * télémétrie partage le WebSocket avec le flux vidéo.
     */
    private static final double SEUIL_VARIATION_TELEMETRIE = 0.5;

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

    /** Degrés de tête par unité de position moteur, signés. Voir le commentaire d'{@code initialiser()}. */
    public static final double RAPPORT_PANORAMIQUE = +PhidgetsServoMotor.DEGRES_SERVO_PAR_UNITE;

    public static final double RAPPORT_INCLINAISON = -4.97;

    public static final double RAPPORT_MONTER_DESCENDRE = -1;

    /**
     * Course autorisée d'un axe, en degrés de tête — les butées de la configuration passées par la
     * transmission. Sert au HUD, qui n'a pas à refaire la conversion : c'est exactement l'endroit
     * où sept conversions écrites à la main vivaient encore.
     */
    public PlageAngulaire plagePanoramique() {
        return plage(phidgetsConfig.neckLeftRightMotorInitialPosition(), RAPPORT_PANORAMIQUE,
                phidgetsConfig.neckLeftRightMotorMinPosition(), phidgetsConfig.neckLeftRightMotorMaxPosition());
    }

    /** Course de l'inclinaison, en degrés de tête (cf. {@link #plagePanoramique}). */
    public PlageAngulaire plageInclinaison() {
        return plage(phidgetsConfig.neckTiltMotorInitialPosition(), RAPPORT_INCLINAISON,
                phidgetsConfig.neckTiltMotorMinPosition(), phidgetsConfig.neckTiltMotorMaxPosition());
    }

    /** Course du monter/descendre (cf. {@link #plagePanoramique}). */
    public PlageAngulaire plageMonterDescendre() {
        return plage(phidgetsConfig.neckUpDownMotorInitialPosition(), RAPPORT_MONTER_DESCENDRE,
                phidgetsConfig.neckUpDownMotorMinPosition(), phidgetsConfig.neckUpDownMotorMaxPosition());
    }

    /**
     * La conversion se refait ici plutôt que d'emprunter la transmission de l'organe : celle-ci
     * n'existe qu'après {@code initialiser()}, et le HUD demande les butées dès qu'il se connecte —
     * y compris pendant les trente secondes de démarrage du robot. Une NullPointerException dans
     * {@code /api/organes} y aurait laissé le panneau vide sans que rien ne le dise.
     */
    private static PlageAngulaire plage(double zeroMoteur, double rapport, double moteurMin, double moteurMax) {
        Transmission conversion = Transmission.affine(zeroMoteur, rapport);
        return PlageAngulaire.entre(conversion.depuisMoteur(moteurMin), conversion.depuisMoteur(moteurMax));
    }

    /**
     * Vitesse, accélération ou déplacement relatif, converti en unités moteur.
     * <p>
     * Le gain est pris <b>au neutre</b> et non à l'angle visé : sur ces trois axes la transmission
     * est affine, donc c'est la même valeur partout. La méthode existe quand même, pour que le jour
     * où l'un d'eux gagne une tringlerie le point de conversion soit déjà nommé.
     * <p>
     * {@code null} traverse : c'est la façon dont un appelant dit « garde la vitesse en cours ».
     */
    private static Double enUnitesMoteur(Double valeur, Transmission transmission) {
        return valeur == null ? null : valeur * transmission.gain(0);
    }

    @Override
    public void initialiser() {
        // Les transmissions se figent ici, en même temps que les moteurs : relire la position
        // initiale à chaque conversion, comme avant, la laissait diverger des butées du moteur,
        // elles fixées une seule fois. Un étalonnage à chaud ne s'appliquait donc qu'à moitié.
        // Panoramique en +1 quand les deux autres sont en -1 : le servo est monté dans l'autre
        // sens, et c'est mesuré, pas supposé. Essai du 2026-09-02 : curseur du HUD à gauche,
        // donc consigne -60, donc moteur 155 — et la tête part à DROITE. Le repère logique
        // valait « positif = à gauche », à l'envers de tout curseur horizontal, et personne ne
        // l'avait vu parce que le regard n'emprunte pas cette porte-là (il passe par rotate()).
        // Les rapports sont ceux des axes, plus 1 : une unité de position Phidgets ne vaut pas un
        // degré, et chaque axe a en plus sa propre démultiplication. Tout ce qui traverse cette
        // classe — butées, positions, vitesses, télémétrie — est donc en DEGRÉS DE TÊTE depuis le
        // 2026-09-08, comme les yeux le sont depuis la veille.
        // Panoramique : goBILDA 2000 en prise directe. 1,583 tient de deux déterminations
        // indépendantes — la fiche (10,56 µs par unité × 0,150 °/µs) et les deux butées dures des
        // yeux, qui portent le même servo. La corrélation de phase du 2026-09-06 donnait 1,56 :
        // c'est une corde, elle inclut le jeu au changement de sens, pas la pente.
        // Inclinaison : goBILDA Stingray-2, 900° sur 500-2500 µs. La fiche donne 4,75, la mesure
        // 4,97 ; on garde la MESURE, seule déterminée deux fois sur le robot et faite sur décor
        // fixe. C'est elle qui rend le gain de boucle du regard exactement 0,9.
        // Monter/descendre : jamais mesuré, et pour cause — c'est une translation, la caméra n'y
        // pivote pas (0,0 px de déplacement, deux fois). Laissé à 1, ses « degrés » restent des
        // unités moteur et le disent.
        transmissionPanoramique = Transmission.affine(phidgetsConfig.neckLeftRightMotorInitialPosition(), RAPPORT_PANORAMIQUE);
        transmissionInclinaison = Transmission.affine(phidgetsConfig.neckTiltMotorInitialPosition(), RAPPORT_INCLINAISON);
        transmissionMonterDescendre = Transmission.affine(phidgetsConfig.neckUpDownMotorInitialPosition(), RAPPORT_MONTER_DESCENDRE);

        // Création des moteurs au démarrage de la phase (et non à la construction du bean).
        // Chaque servo est engagé à sa position de repos — sa position physique probable,
        // laissée par le dernier arrêt — pour éviter le saut à pleine vitesse à l'engagement ;
        // le retour à la position initiale se fait ensuite en rampe douce (reset()).
        moteurPanoramique = new PhidgetsServoMotor(
                phidgetsConfig.neckLeftRightMotorIndex(),
                phidgetsConfig.neckLeftRightMotorInitialPosition(),
                phidgetsConfig.neckLeftRightMotorMinPosition(),
                phidgetsConfig.neckLeftRightMotorMaxPosition(),
                enUnitesMoteur(phidgetsConfig.neckLeftRightMotorSpeed(), transmissionPanoramique),
                enUnitesMoteur(phidgetsConfig.neckLeftRightMotorAcceleration(), transmissionPanoramique),
                positionReposPanoramique()
        );
        moteurInclinaison = new PhidgetsServoMotor(
                phidgetsConfig.neckTiltMotorIndex(),
                phidgetsConfig.neckTiltMotorInitialPosition(),
                phidgetsConfig.neckTiltMotorMinPosition(),
                phidgetsConfig.neckTiltMotorMaxPosition(),
                enUnitesMoteur(phidgetsConfig.neckTiltMotorSpeed(), transmissionInclinaison),
                enUnitesMoteur(phidgetsConfig.neckTiltMotorAcceleration(), transmissionInclinaison),
                positionReposInclinaison()
        );
        moteurMonterDescendre = new PhidgetsServoMotor(
                phidgetsConfig.neckUpDownMotorIndex(),
                phidgetsConfig.neckUpDownMotorInitialPosition(),
                phidgetsConfig.neckUpDownMotorMinPosition(),
                phidgetsConfig.neckUpDownMotorMaxPosition(),
                enUnitesMoteur(phidgetsConfig.neckUpDownMotorSpeed(), transmissionMonterDescendre),
                enUnitesMoteur(phidgetsConfig.neckUpDownMotorAcceleration(), transmissionMonterDescendre),
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
            moteurPanoramique.backward(enUnitesMoteur(vitesse, transmissionPanoramique), enUnitesMoteur(acceleration, transmissionPanoramique), waitForPosition);
        } else {
            moteurPanoramique.setVitesse(enUnitesMoteur(vitesse, transmissionPanoramique));
        }
    }

    /** Tourne la tête à droite sans s'arrêter. */
    public void tournerADroite(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsPanoramiqueEnCours != MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE) {
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.TOURNER_DROITE;
            moteurPanoramique.forward(enUnitesMoteur(vitesse, transmissionPanoramique), enUnitesMoteur(acceleration, transmissionPanoramique), waitForPosition);
        } else {
            moteurPanoramique.setVitesse(enUnitesMoteur(vitesse, transmissionPanoramique));
        }
    }

    /**
     * Tourne la tête sur le plan "Gauche / Droite" d'un certain angle.
     *
     * @param angle angle en degrés (négatif : à gauche, positif : à droite)
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
        // rotate() ajoute à la position MOTEUR sans passer par la transmission. Sur cet axe ce
        // n'est plus un piège depuis que le panoramique est croissant : les deux portes vont
        // enfin du même côté. Sur tournerTeteHautBas et monterDescendreTete, si : leur
        // transmission est décroissante, donc un angle positif n'y fait pas tourner la tête du
        // même côté selon la porte empruntée. Ça ne se voit pas parce que le regard, seul
        // appelant de rotate(), a eu son sens étalonné sur le robot
        // (robot.regard.inclinaison.sens.inverse) et absorbe l'écart sans le nommer — comme
        // commande.par.degre.vu absorbe le rapport de transmission. Redresser ces deux-là
        // change le mouvement : ça se fait avec leur vrai rapport et une validation, pas ici.
        moteurPanoramique.rotate(enUnitesMoteur(angle, transmissionPanoramique), enUnitesMoteur(vitesse, transmissionPanoramique), enUnitesMoteur(acceleration, transmissionPanoramique), waitForPosition);
    }

    /**
     * Positionne la tête sur le plan "Gauche / Droite", en degrés relatifs au repos.
     *
     * @param position position en degrés (0 : de face, négatif : à gauche, positif : à droite)
     */
    public void positionnerTeteGaucheDroite(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        double positionMoteur = transmissionPanoramique.versMoteur(position);
        if (positionMoteur >= moteurPanoramique.getPositionMin() && positionMoteur <= moteurPanoramique.getPositionMax()) {
            logger.debug("POS_GD = {}", positionMoteur);
        // Consigne atteinte, servo arrêté : plus aucun mouvement continu en cours (cf.
        // tournerTeteGaucheDroite).
            mouvementsPanoramiqueEnCours = MOUVEMENTS_PANORAMIQUE.STOPPER;
            moteurPanoramique.setPositionCible(positionMoteur, enUnitesMoteur(vitesse, transmissionPanoramique), enUnitesMoteur(acceleration, transmissionPanoramique), waitForPosition);
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
            moteurInclinaison.forward(enUnitesMoteur(vitesse, transmissionInclinaison), enUnitesMoteur(acceleration, transmissionInclinaison), waitForPosition);
        } else {
            logger.debug("BAS VITESSE");
            moteurInclinaison.setVitesse(enUnitesMoteur(vitesse, transmissionInclinaison));
        }
    }

    /** Tourne la tête en haut sans s'arrêter. */
    public void tournerEnHaut(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsInclinaisonEnCours != MOUVEMENTS_INCLINAISON.TOURNER_HAUT) {
            logger.debug("HAUT");
            mouvementsInclinaisonEnCours = MOUVEMENTS_INCLINAISON.TOURNER_HAUT;
            moteurInclinaison.backward(enUnitesMoteur(vitesse, transmissionInclinaison), enUnitesMoteur(acceleration, transmissionInclinaison), waitForPosition);
        } else {
            logger.debug("HAUT VITESSE");
            moteurInclinaison.setVitesse(enUnitesMoteur(vitesse, transmissionInclinaison));
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
        moteurInclinaison.rotate(enUnitesMoteur(angle, transmissionInclinaison), enUnitesMoteur(vitesse, transmissionInclinaison), enUnitesMoteur(acceleration, transmissionInclinaison), waitForPosition);
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
            moteurInclinaison.setPositionCible(positionMoteur, enUnitesMoteur(vitesse, transmissionInclinaison), enUnitesMoteur(acceleration, transmissionInclinaison), waitForPosition);
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
            moteurMonterDescendre.forward(enUnitesMoteur(vitesse, transmissionMonterDescendre), enUnitesMoteur(acceleration, transmissionMonterDescendre), waitForPosition);
        } else {
            logger.debug("DESCENDRE VITESSE");
            moteurMonterDescendre.setVitesse(enUnitesMoteur(vitesse, transmissionMonterDescendre));
        }
    }

    /** Monte la tête sans s'arrêter. */
    public void monter(Double vitesse, Double acceleration, boolean waitForPosition) {
        if (mouvementsMonterDescendreEnCours != MOUVEMENTS_MONTER_DESCENDRE.MONTER) {
            logger.debug("MONTER");
            mouvementsMonterDescendreEnCours = MOUVEMENTS_MONTER_DESCENDRE.MONTER;
            moteurMonterDescendre.backward(enUnitesMoteur(vitesse, transmissionMonterDescendre), enUnitesMoteur(acceleration, transmissionMonterDescendre), waitForPosition);
        } else {
            logger.debug("MONTER VITESSE");
            moteurMonterDescendre.setVitesse(enUnitesMoteur(vitesse, transmissionMonterDescendre));
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
        moteurMonterDescendre.rotate(enUnitesMoteur(angle, transmissionMonterDescendre), enUnitesMoteur(vitesse, transmissionMonterDescendre), enUnitesMoteur(acceleration, transmissionMonterDescendre), waitForPosition);
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
            moteurMonterDescendre.setPositionCible(positionMoteur, enUnitesMoteur(vitesse, transmissionMonterDescendre), enUnitesMoteur(acceleration, transmissionMonterDescendre), waitForPosition);
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

    /**
     * Ramène la tête à la posture de départ, sur demande de n'importe quel client — manette, HUD, ou
     * une activité. L'appelant dit « rentre », pas « va à telle position » : c'est ici que vit la
     * connaissance de l'endroit.
     */
    @EventListener
    @Async(RobotEventsConfig.COU_EVENT_EXECUTOR)
    public void handlePostureDeDepartEvent(PostureDeDepartEvent postureDeDepartEvent) {
        if (!running || arretUrgence) {
            return;
        }
        reset();
    }

    /** Remet la tête à sa position par défaut. */
    private void reset() {
        moteurInclinaison.setPositionCible(phidgetsConfig.neckTiltMotorInitialPosition(), null, null, false);
        moteurPanoramique.setPositionCible(phidgetsConfig.neckLeftRightMotorInitialPosition(), null, null, false);
        moteurMonterDescendre.setPositionCible(phidgetsConfig.neckUpDownMotorInitialPosition(), null, null, false);
    }

    /**
     * Position panoramique courante dans le repère « logique » exposé aux clients
     * (position = position moteur − init, cf. {@link #positionnerTeteGaucheDroite}), ou
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
