package fr.roboteek.robot.util.phidgets;

import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.PhidgetException;
import com.phidget22.RCServo;
import com.phidget22.RCServoTargetPositionReachedEvent;
import com.phidget22.RCServoTargetPositionReachedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implémentation d'un moteur servo-moteur via le servo-contrôleur Phidgets.
 *
 * @author Java Developer
 */
public class PhidgetsServoMotor implements AttachListener, DetachListener, RCServoTargetPositionReachedListener, ErrorListener {

    private static final Logger logger = LoggerFactory.getLogger(PhidgetsServoMotor.class);

    /** Moteur Phidget associé. */
    private RCServo rcServo;

    /** Position initiale du moteur. */
    private double positionInitiale;

    /** Position minimale du moteur. */
    private double positionMin;

    /** Position maximale du moteur. */
    private double positionMax;

    /** Vitesse par défaut. */
    private double vitesseParDefaut;

    /** Accélération par défaut. */
    private double accelerationParDefaut;

    /**
     * Position à laquelle engager le servo à l'attachement : la position physique probable
     * (position de repos laissée par le dernier arrêt). Engager ailleurs ferait sauter le
     * servo à pleine vitesse matérielle — sans retour de position, le contrôleur ne peut
     * pas appliquer de rampe sur ce premier mouvement. Optionnelle : à défaut, la position
     * initiale est utilisée (comportement historique).
     */
    private Double positionEngagement;

    /** Flag indiquant que la position est atteinte. */
    private AtomicBoolean positionAtteinte = new AtomicBoolean(true);

    /**
     * Attente d'arrivée en position pour les consignes synchrones. Remplace la boucle d'attente
     * active qui occupait un coeur entier <b>et</b> gardait le verrou de l'objet : {@link #stop()}
     * ne pouvait alors pas passer, ce qui aurait bloqué un arrêt d'urgence derrière un mouvement
     * en cours. Armé à chaque consigne, ouvert à l'arrivée ou à l'arrêt.
     */
    private volatile CountDownLatch arriveeEnPosition = new CountDownLatch(0);

    /**
     * Dernière limite de vitesse effectivement écrite sur le contrôleur, {@code null} quand on
     * ignore ce qu'il a en mémoire (avant l'attache, après un {@link #stop()} ou un détachement).
     * <p>
     * Sert à ne pas réécrire une valeur inchangée : chaque écriture est un aller-retour USB, et
     * une consigne de position en coûtait trois (accélération, vitesse, position). À la cadence
     * d'une animation — cinq axes, plusieurs dizaines de consignes par seconde — c'est le débit du
     * hub qu'on dépense pour rien, et la consigne de position arrive d'autant plus tard.
     */
    private volatile Double vitesseEcrite;

    /** Dernière accélération écrite sur le contrôleur (voir {@link #vitesseEcrite}). */
    private volatile Double accelerationEcrite;

    /**
     * Bornes de vitesse et d'accélération du servo, lues sur le contrôleur à l'attache et non
     * devinées : une consigne hors bornes fait lever le Phidget et le mouvement est perdu.
     * Nulles tant que le servo n'est pas attaché — on n'écrête alors rien.
     */
    private volatile Double vitesseMinMoteur;
    private volatile Double vitesseMaxMoteur;
    private volatile Double accelerationMinMoteur;
    private volatile Double accelerationMaxMoteur;

    /** Index du moteur sur le contrôleur, conservé pour désigner le servo dans les journaux. */
    private int index;

    /** Vrai tant que les consignes de rotation sont rabotées par une butée (voir {@link #rotate}). */
    private volatile boolean consigneBornee = false;

    /**
     * Constructeur d'un moteur Phidget.
     *
     * @param index index du moteur sur le contrôleur
     */
    public PhidgetsServoMotor(int index, double positionInitiale, double positionMin, double positionMax, double vitesseParDefaut, double accelerationParDefaut) {
        this(index, positionInitiale, positionMin, positionMax, vitesseParDefaut, accelerationParDefaut, null);
    }

    /**
     * Constructeur d'un moteur Phidget avec position d'engagement.
     *
     * @param index              index du moteur sur le contrôleur
     * @param positionEngagement position physique probable du servo, à laquelle l'engager
     *                           sans saut (voir {@link #positionEngagement}) ; peut être null
     */
    public PhidgetsServoMotor(int index, double positionInitiale, double positionMin, double positionMax, double vitesseParDefaut, double accelerationParDefaut, Double positionEngagement) {
        try {
            this.index = index;
            this.positionEngagement = positionEngagement;
            this.positionInitiale = positionInitiale;
            this.positionMin = positionMin;
            this.positionMax = positionMax;
            this.vitesseParDefaut = vitesseParDefaut;
            this.accelerationParDefaut = accelerationParDefaut;
            rcServo = new RCServo();
            rcServo.addAttachListener(this);
            rcServo.addDetachListener(this);
            rcServo.addErrorListener(this);
            // Ni PositionChange ni VelocityChange : le contrôleur les émet en continu sur chaque
            // canal et personne ne les consommait. On a cru un moment qu'ils grevaient le débit
            // d'écriture ; le banc du 2026-08-30 dit non — 90,56 ms par tour sans eux contre 90,54
            // avec. C'est donc du code mort qu'on retire, rien de plus : ne pas se réinscrire en
            // espérant y gagner quoi que ce soit. getPositionReelle() n'en pâtit pas, elle lit le
            // cache de la bibliothèque, que les paquets du contrôleur alimentent écouteur ou pas.
            rcServo.addTargetPositionReachedListener(this);

            // Configuration
            rcServo.setDeviceSerialNumber(561050);
            rcServo.setHubPort(0);
            rcServo.setChannel(index);

            // Ouverture du moteur
            rcServo.open(5000);

        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Récupère la position cible du moteur.
     *
     * @return la position cible du moteur
     */
    public double getPositionCible() {
        try {
            return rcServo.getTargetPosition();
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
    }

    public void setPositionCible(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        CountDownLatch attente = envoyerConsigne(position, vitesse, acceleration);
        // Attente HORS du verrou : la tenir à l'intérieur empêcherait stop() de passer.
        if (waitForPosition && attente != null) {
            try {
                if (!attente.await(10, TimeUnit.SECONDS)) {
                    logger.warn("Servo {} : position {} toujours pas atteinte au bout de 10 s", index, arrondi(position));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Écrit la consigne sur le contrôleur et rend le verrou qui s'ouvrira à l'arrivée.
     * <p>
     * Fixer accélération ET vitesse AVANT la consigne de position. Un {@link #stop()} précédent a
     * laissé la limite de vitesse à 0 sur le contrôleur. Si on envoie la consigne de position tant
     * que la vitesse vaut 0, le servo peut ne pas démarrer — le rattrapage de vitesse après coup
     * est dépendant du firmware — d'où un oeil qui, de façon intermittente, « ne repart pas » après
     * avoir été stoppé (ex. bouger l'oeil gauche seul puis lancer un roulis). C'est {@code stop()}
     * qui oublie la vitesse écrite, pour que celle-ci soit toujours rétablie ici.
     */
    private synchronized CountDownLatch envoyerConsigne(double position, Double vitesse, Double acceleration) {
        try {
            positionAtteinte.set(false);
            arriveeEnPosition = new CountDownLatch(1);
            ecrireAcceleration(acceleration != null ? acceleration : accelerationParDefaut);
            ecrireVitesse(vitesse != null ? vitesse : vitesseParDefaut);
            rcServo.setTargetPosition(position);
            return arriveeEnPosition;
        } catch (PhidgetException e) {
            logger.error("Servo {} : consigne de position {} refusée", index, arrondi(position), e);
            return null;
        }
    }

    /**
     * Consigne de position seule, sans toucher à la vitesse ni à l'accélération.
     * <p>
     * Destinée au flux d'échantillons d'une trajectoire, où vitesse et accélération sont réglées
     * une fois au départ. Ce n'est pas une micro-optimisation : le banc du 2026-08-29 a mesuré
     * <b>15 à 18 ms par écriture</b> sur ce hub, quel que soit l'axe et quel que soit le nombre
     * d'axes menés de front — le contrôleur ne draine qu'une soixantaine d'écritures par seconde,
     * toutes origines confondues. Supprimer l'écriture de vitesse fait donc passer un flux de
     * consignes du simple au double (155 ms à 90 ms pour cinq axes).
     * <p>
     * Ne pas chercher à contourner ce plafond par les consignes asynchrones : au-delà de 64
     * commandes en attente, la bibliothèque Phidget <b>jette silencieusement</b> les suivantes
     * (« Command queue is full; dropping entry »). Essayé le 2026-08-29 à 30 Hz sur cinq axes,
     * 600 consignes de position perdues sur 1500 — le servo finit ailleurs qu'où on le croit.
     */
    public synchronized void setPositionCible(double position) {
        try {
            positionAtteinte.set(false);
            rcServo.setTargetPosition(position);
        } catch (PhidgetException e) {
            logger.error("Servo {} : consigne de position {} refusée", index, arrondi(position), e);
        }
    }

    /**
     * Écrit la limite de vitesse, et seulement si elle change (voir {@link #vitesseEcrite}).
     * La valeur est écrêtée aux bornes du servo : au-delà, le Phidget lève et le mouvement est
     * perdu, alors qu'aller aussi vite que possible est ce qu'on voulait.
     */
    private void ecrireVitesse(double vitesse) throws PhidgetException {
        double valeur = borner(vitesse, vitesseMinMoteur, vitesseMaxMoteur);
        if (vitesseEcrite != null && vitesseEcrite == valeur) {
            return;
        }
        rcServo.setVelocityLimit(valeur);
        vitesseEcrite = valeur;
    }

    /** Écrit l'accélération, et seulement si elle change (voir {@link #ecrireVitesse}). */
    private void ecrireAcceleration(double acceleration) throws PhidgetException {
        double valeur = borner(acceleration, accelerationMinMoteur, accelerationMaxMoteur);
        if (accelerationEcrite != null && accelerationEcrite == valeur) {
            return;
        }
        rcServo.setAcceleration(valeur);
        accelerationEcrite = valeur;
    }

    /** Écrête une consigne aux bornes du servo, quand celles-ci sont connues. */
    private static double borner(double valeur, Double min, Double max) {
        double borne = valeur;
        if (min != null) {
            borne = Math.max(borne, min);
        }
        if (max != null) {
            borne = Math.min(borne, max);
        }
        return borne;
    }

    /**
     * Récupère la position réelle du moteur.
     *
     * @return la position réelle du moteur
     */
    public double getPositionReelle() {
        try {
            return rcServo.getPosition();
        } catch (PhidgetException e) {
            logger.error("Impossible de lire la position réelle du servo", e);
            return 0;
        }
    }

    /**
     * Position réelle, ou {@code null} si le servo ne l'a pas encore publiée (erreur Phidget 0x33,
     * fréquente juste après l'attache, tant que la première valeur n'a pas été reçue). Contrairement
     * à {@link #getPositionReelle()}, ne journalise pas et ne renvoie pas un 0 trompeur : destiné à
     * la télémétrie, qui doit simplement ignorer une position pas encore disponible.
     */
    public Double getPositionReelleOuNull() {
        try {
            return rcServo.getPosition();
        } catch (PhidgetException e) {
            return null;
        }
    }

    /**
     * Tourne d'un angle relatif à la position courante, <b>bornée aux butées logicielles</b>.
     * <p>
     * Le bornage n'est pas décoratif : contrairement au positionnement absolu, qui vérifie ses
     * limites chez l'appelant, une rotation relative part d'une position qu'on ne choisit pas.
     * Une consigne hors bornes ferait lever le Phidget et le mouvement serait perdu ; borner
     * fait tourner aussi loin que possible, ce qu'on veut d'un geste qui vise quelque chose.
     */
    public void rotate(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        double demande = getPositionReelle() + angle;
        double cible = Math.clamp(demande, positionMin, positionMax);
        // Sans ces lignes, un servo collé à sa butée est indiscernable d'un servo qui obéit mal :
        // l'appelant redemande, rien ne bouge, et rien ne le dit. Vu sur le cou,
        // vingt consignes de suite dans le vide, prises pour un défaut de réglage.
        // Journalisé aux CHANGEMENTS d'état seulement : un axe qui reste en butée écrirait sinon
        // une ligne par consigne, soit une par seconde pour le regard.
        if (cible != demande && !consigneBornee) {
            consigneBornee = true;
            logger.info("Servo {} : consigne {} bornée à {} (butée {} - {}) — en butée",
                    index, arrondi(demande), arrondi(cible), positionMin, positionMax);
        } else if (cible == demande && consigneBornee) {
            consigneBornee = false;
            logger.info("Servo {} : sorti de butée", index);
        }
        setPositionCible(cible, vitesse, acceleration, waitForPosition);
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 10) / 10d;
    }

    public void forward(Double vitesse, Double acceleration, boolean waitForPosition) {
        setPositionCible(positionMax, vitesse, acceleration, waitForPosition);
    }

    public void backward(Double vitesse, Double acceleration, boolean waitForPosition) {
        setPositionCible(positionMin, vitesse, acceleration, waitForPosition);
    }

    public void stop() {
        try {
            rcServo.setVelocityLimit(0);
            // La limite de vitesse reste à 0 sur le contrôleur : on oublie ce qu'on croyait y avoir
            // écrit pour que la prochaine consigne la rétablisse, au lieu de la juger inchangée et
            // de laisser le servo immobile (voir envoyerConsigne).
            vitesseEcrite = null;
            positionAtteinte.set(true);
            arriveeEnPosition.countDown();
        } catch (PhidgetException e) {
            logger.error("Servo {} : arrêt refusé", index, e);
        }
    }

    public void close() {
        try {
            rcServo.close();
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public double getPositionMax() {
        return positionMax;
    }

    public void setPositionMax(double position) {
        this.positionMax = position;
    }

    public double getPositionMin() {
        return positionMin;
    }

    public void setPositionMin(double position) {
        this.positionMin = position;
    }

    public boolean isEngaged() {
        try {
            return rcServo.getEngaged();
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public void setEngaged(boolean state) {
        try {
            rcServo.setEngaged(state);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isSpeedRampingState() {
        try {
            return rcServo.getSpeedRampingState();
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public void setSpeedRampingState(boolean state) {
        try {
            rcServo.setSpeedRampingState(state);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isStopped() {
        try {
            return positionAtteinte.get() && rcServo.getVelocity() == 0;
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void setVitesse(Double vitesse) {
        try {
            ecrireVitesse(vitesse != null ? vitesse : vitesseParDefaut);
        } catch (PhidgetException e) {
            logger.error("Servo {} : vitesse {} refusée", index, vitesse, e);
        }
    }

    /** Vitesse maximale acceptée par le servo, ou {@code null} tant qu'il n'est pas attaché. */
    public Double getVitesseMaxMoteur() {
        return vitesseMaxMoteur;
    }

    /** Accélération maximale acceptée par le servo, ou {@code null} tant qu'il n'est pas attaché. */
    public Double getAccelerationMaxMoteur() {
        return accelerationMaxMoteur;
    }

    @Override
    public synchronized void onAttach(AttachEvent attachEvent) {
        // Synchronisé comme les consignes : l'attache arrive sur un thread Phidget et amorce la
        // mémoire des valeurs écrites, qu'une consigne concurrente fausserait. Sans risque de
        // blocage, l'attente d'arrivée en position se faisant hors du verrou.
        try {
            if (attachEvent.getSource().equals(rcServo)) {
                lireBornesDuServo();
                ecrireAcceleration(accelerationParDefaut);
                // Engagement à la position physique probable pour éviter un saut à pleine
                // vitesse matérielle (la rampe ne s'applique qu'entre deux consignes,
                // jamais sur le rattrapage initial de la position réelle, inconnue)
                rcServo.setTargetPosition(positionEngagement != null ? positionEngagement : positionInitiale);
                ecrireVitesse(vitesseParDefaut);
                rcServo.setEngaged(true);
                logger.debug("Servo {} attaché", rcServo.getChannel());
            }
        } catch (PhidgetException e) {
            logger.error("Servo {} : attachement incomplet", index, e);
        }

    }

    /**
     * Relève les bornes de vitesse et d'accélération du servo. Journalisées parce qu'elles
     * décident de ce qu'une animation peut demander, et qu'elles ne se devinent pas depuis la
     * configuration : celle-ci ne porte que les vitesses <b>de travail</b>, pas les maximums du
     * matériel.
     */
    private void lireBornesDuServo() {
        try {
            vitesseMinMoteur = rcServo.getMinVelocityLimit();
            vitesseMaxMoteur = rcServo.getMaxVelocityLimit();
            accelerationMinMoteur = rcServo.getMinAcceleration();
            accelerationMaxMoteur = rcServo.getMaxAcceleration();
            logger.info("Servo {} : vitesse {} à {} °/s, accélération {} à {} °/s²",
                    index, vitesseMinMoteur, vitesseMaxMoteur, accelerationMinMoteur, accelerationMaxMoteur);
        } catch (PhidgetException e) {
            logger.warn("Servo {} : bornes illisibles, les consignes ne seront pas écrêtées", index, e);
        }
    }

    @Override
    public void onTargetPositionReached(RCServoTargetPositionReachedEvent event) {
        if (event.getSource().equals(rcServo)) {
            positionAtteinte.set(true);
            arriveeEnPosition.countDown();
        }
    }

    @Override
    public void onError(ErrorEvent errorEvent) {
        logger.error("Erreur servo : {}", errorEvent.getDescription());
    }

    @Override
    public void onDetach(DetachEvent event) {
        if (event.getSource().equals(rcServo)) {
            // Le contrôleur a perdu son état : ce qu'on croyait y avoir écrit ne vaut plus rien,
            // et tout serait à réécrire au rattachement.
            vitesseEcrite = null;
            accelerationEcrite = null;
            arriveeEnPosition.countDown();
            logger.debug("Servo {} détaché", index);
        }

    }
}
