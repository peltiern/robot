package fr.roboteek.robot.util.phidgets;

import com.phidget22.AttachEvent;
import com.phidget22.AttachListener;
import com.phidget22.DetachEvent;
import com.phidget22.DetachListener;
import com.phidget22.ErrorEvent;
import com.phidget22.ErrorListener;
import com.phidget22.PhidgetException;
import com.phidget22.RCServo;
import com.phidget22.RCServoPositionChangeEvent;
import com.phidget22.RCServoPositionChangeListener;
import com.phidget22.RCServoTargetPositionReachedEvent;
import com.phidget22.RCServoTargetPositionReachedListener;
import com.phidget22.RCServoVelocityChangeEvent;
import com.phidget22.RCServoVelocityChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implémentation d'un moteur servo-moteur via le servo-contrôleur Phidgets.
 *
 * @author Java Developer
 */
public class PhidgetsServoMotor implements AttachListener, DetachListener, RCServoPositionChangeListener, RCServoTargetPositionReachedListener, ErrorListener, RCServoVelocityChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(PhidgetsServoMotor.class);

    /**
     * Moteur Phidget associé.
     */
    private RCServo rcServo;

    /**
     * Position initiale du moteur.
     */
    private double positionInitiale;

    /**
     * Position minimale du moteur.
     */
    private double positionMin;

    /**
     * Position maximale du moteur.
     */
    private double positionMax;

    /**
     * Vitesse par défaut.
     */
    private double vitesseParDefaut;

    /**
     * Accélération par défaut.
     */
    private double accelerationParDefaut;

    /**
     * Position à laquelle engager le servo à l'attachement : la position physique probable
     * (position de repos laissée par le dernier arrêt). Engager ailleurs ferait sauter le
     * servo à pleine vitesse matérielle — sans retour de position, le contrôleur ne peut
     * pas appliquer de rampe sur ce premier mouvement. Optionnelle : à défaut, la position
     * initiale est utilisée (comportement historique).
     */
    private Double positionEngagement;

    /**
     * Flag indiquant que la position est atteinte.
     */
    private AtomicBoolean positionAtteinte = new AtomicBoolean(true);

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
            rcServo.addPositionChangeListener(this);
            rcServo.addTargetPositionReachedListener(this);
            rcServo.addVelocityChangeListener(this);

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

    public synchronized void setPositionCible(double position, Double vitesse, Double acceleration, boolean waitForPosition) {
        try {
            positionAtteinte.set(false);
            // Fixer accélération ET vitesse AVANT la consigne de position. Un stop() précédent a pu
            // laisser la limite de vitesse à 0 (stop() fait setVelocityLimit(0) sans jamais la
            // restaurer). Si on envoie la consigne de position tant que la vitesse vaut 0, le servo
            // peut ne pas démarrer — le rattrapage de vitesse après coup est dépendant du firmware —
            // d'où un oeil qui, de façon intermittente, « ne repart pas » après avoir été stoppé
            // (ex. bouger l'oeil gauche seul puis lancer un roulis). En réglant la vitesse d'abord,
            // la consigne de position part toujours avec une limite de vitesse non nulle.
            setAcceleration(acceleration);
            setVitesse(vitesse);
            rcServo.setTargetPosition(position);
            while (waitForPosition && !positionAtteinte.get()) ;
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    public void rotate(double angle, Double vitesse, Double acceleration, boolean waitForPosition) {
        setPositionCible(getPositionReelle() + angle, vitesse, acceleration, waitForPosition);
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
            // TODO A voir si nécessaire
            //rcServo.setTargetPosition(rcServo.getPosition());
            positionAtteinte.set(true);
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    public void setVitesse(Double vitesse) {
        try {
            if (vitesse != null) {
                rcServo.setVelocityLimit(vitesse.doubleValue());
            } else {
                rcServo.setVelocityLimit(vitesseParDefaut);
            }
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setAcceleration(Double acceleration) {
        try {
            if (acceleration != null) {
                rcServo.setAcceleration(acceleration.doubleValue());
            } else {
                rcServo.setAcceleration(accelerationParDefaut);
            }
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        // Une fois que le moteur est attaché, on l'active
        try {
            if (attachEvent.getSource().equals(rcServo)) {
                //rcServo.setDataInterval(32);
                rcServo.setAcceleration(accelerationParDefaut);
                // Engagement à la position physique probable pour éviter un saut à pleine
                // vitesse matérielle (la rampe ne s'applique qu'entre deux consignes,
                // jamais sur le rattrapage initial de la position réelle, inconnue)
                rcServo.setTargetPosition(positionEngagement != null ? positionEngagement : positionInitiale);
                rcServo.setVelocityLimit(vitesseParDefaut);
                rcServo.setEngaged(true);
                logger.debug("Servo {} attaché", rcServo.getChannel());
            }
        } catch (PhidgetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onTargetPositionReached(RCServoTargetPositionReachedEvent event) {
        if (event.getSource().equals(rcServo)) {
            //try {
            positionAtteinte.set(true);
//			} catch (PhidgetException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
        }
        ;
    }

    @Override
    public void onPositionChange(RCServoPositionChangeEvent event) {
        if (event.getSource().equals(rcServo)) {
//			// Envoi d'un évènement à l'ensemble des écouteurs
//			if (listeEcouteursChangementPosition != null && !listeEcouteursChangementPosition.isEmpty()) {
//				final MotorPositionChangeEvent evenement = new MotorPositionChangeEvent(this, event.getPosition());
//				for (MotorPositionChangeListener ecouteur : listeEcouteursChangementPosition) {
//					ecouteur.onPositionchanged(evenement);
//				}
//			}
        }

    }

    @Override
    public void onError(ErrorEvent errorEvent) {
        logger.error("Erreur servo : {}", errorEvent.getDescription());
    }

    @Override
    public void onDetach(DetachEvent event) {
        if (event.getSource().equals(rcServo)) {
            try {
                logger.debug("Servo {} détaché", rcServo.getChannel());
            } catch (PhidgetException e) {
                logger.error("Erreur lors du détachement du servo", e);
            }
        }

    }

    public static void main(String[] args) {
        final PhidgetsServoMotor moteurG = new PhidgetsServoMotor(0, 90, 50, 150, 100, 2000);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        moteurG.setSpeedRampingState(true);
        moteurG.setPositionCible(130, null, null, true);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        moteurG.setSpeedRampingState(true);
        moteurG.setPositionCible(60, null, null, true);
        System.exit(0);
    }

    @Override
    public void onVelocityChange(RCServoVelocityChangeEvent event) {
        // Événement à haute fréquence : aucun log (évite d'inonder la sortie)
    }
}
