package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.organes.AbstractOrganeWithThread;
import fr.roboteek.robot.systemenerveux.spring.RobotLifecyclePhases;
import fr.roboteek.robot.util.respeaker.MicArrayV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Capteur d'activité sonore basé sur le micro-array ReSpeaker (détection de voix
 * et direction d'arrivée du son).
 * <p>
 * <b>Désactivé par défaut.</b> Pour l'activer :
 * {@code robot.capteurs.activite-son.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "robot.capteurs.activite-son.enabled", havingValue = "true")
public class CapteurActiviteSon extends AbstractOrganeWithThread implements SmartLifecycle {

    private MicArrayV2 micArrayV2;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(CapteurActiviteSon.class);

    /**
     * Flag indiquant de stopper le thread.
     */
    private volatile boolean stopperThread = false;

    /**
     * Flag de démarrage de l'organe (cycle de vie Spring).
     */
    private volatile boolean running = false;

    private int doaAngleCourant;

    private boolean voiceActivityCourant;

    private boolean speechDetectedCourant;

    private int angleVoixCourant;

    public CapteurActiviteSon() {
        super("SoundActivity");
    }

    @Override
    public void initialiser() {
        micArrayV2 = MicArrayV2.getInstance();
        doaAngleCourant = micArrayV2.getDoaAngle();
        voiceActivityCourant = micArrayV2.isVoiceActivity();
        speechDetectedCourant = micArrayV2.isSpeechDetected();
    }

    @Override
    public void loop() {
        while (!stopperThread) {
            int angle = micArrayV2.getDoaAngle();
            boolean voiceActivity = micArrayV2.isVoiceActivity();
            boolean speechDetected = micArrayV2.isSpeechDetected();
            traiterValeurs(angle, voiceActivity, speechDetected);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Restitution du flag d'interruption ; la boucle s'arrête via stopperThread
                Thread.currentThread().interrupt();
                stopperThread = true;
            }
        }
    }

    @Override
    public void arreter() {
        stopperThread = true;
    }

    private void traiterValeurs(int angle, boolean voiceActivity, boolean speechDetected) {
        // TODO gérer la synchronisation
        // TODO gérer le calcul de la différence d'angle
        boolean angleDifferent = Math.abs(calculerDifferenceAngles(angle, doaAngleCourant)) > 0;
        if (angleDifferent) {
            doaAngleCourant = angle;
        }
        boolean voiceActivityDifferent = voiceActivity != voiceActivityCourant;
        if (voiceActivityDifferent) {
            voiceActivityCourant = voiceActivity;
        }
        boolean speechDetectedDifferent = speechDetected != speechDetectedCourant;
        if (speechDetectedDifferent) {
            speechDetectedCourant = speechDetected;
        }
        logger.debug("Angle = {}\t\tVoix = {}\t\tSpeech = {}", doaAngleCourant, voiceActivityCourant, speechDetectedCourant);
        if (angleDifferent || voiceActivityDifferent || speechDetectedDifferent) {
            boolean angleVoixDifferent = Math.abs(calculerDifferenceAngles(angle, angleVoixCourant)) > 0;
            if (angleVoixDifferent) {
                angleVoixCourant = angle;
            }
//            if (voiceActivityCourant && angleVoixDifferent)
//            // TODO envoyer un évènement
//            tournerCou();
        }
    }

//    private void tournerCou() {
//        if (angleVoixCourant < 60 || angleVoixCourant > 300) {
//            MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
//            mouvementCouEvent.setAccelerationGaucheDroite(100D);
//            mouvementCouEvent.setVitesseGaucheDroite(60D);
//            if (angleVoixCourant < 60) {
//                mouvementCouEvent.setPositionGaucheDroite(angleVoixCourant);
//            } else {
//                mouvementCouEvent.setPositionGaucheDroite(angleVoixCourant - 360);
//            }
//            mouvementCouEvent.setSynchrone(false);
//            RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
//        }
//    }

    private int calculerDifferenceAngles(int angle1, int angle2) {
        int phi = Math.abs(angle1 - angle2) % 360;       // This is either the distance or 360 - distance
        return phi > 180 ? 360 - phi : phi;
    }

    @Override
    public void start() {
        initialiser();
        super.start();
        running = true;
        logger.info("CapteurActiviteSon démarré");
    }

    @Override
    public void stop() {
        running = false;
        arreter();
        logger.info("CapteurActiviteSon arrêté");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return RobotLifecyclePhases.CAPTEURS;
    }
}
