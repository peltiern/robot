package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import fr.roboteek.robot.util.respeaker.MicArrayV2;

public class CapteurActiviteSon extends AbstractOrgane {

    private MicArrayV2 micArrayV2;

    /** Flag indiquant de stopper le thread. */
    private boolean stopperThread = false;

    private int doaAngleCourant;

    private boolean voiceActivityCourant;

    private boolean speechDetectedCourant;

    private int angleVoixCourant;

    @Override
    public void initialiser() {
        micArrayV2 = MicArrayV2.getInstance();
        doaAngleCourant = micArrayV2.getDoaAngle();
        voiceActivityCourant = micArrayV2.isVoiceActivity();
        speechDetectedCourant = micArrayV2.isSpeechDetected();

        final Thread threadCapteur = new Thread("SoundActivity") {
            @Override
            public void run() {
                while (!stopperThread) {
                    int angle = micArrayV2.getDoaAngle();
                    boolean voiceActivity = micArrayV2.isVoiceActivity();
                    boolean speechDetected = micArrayV2.isSpeechDetected();
                    traiterValeurs(angle, voiceActivity, speechDetected);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO
                        e.printStackTrace();
                    }
                }
            }
        };
        threadCapteur.start();
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
       // System.out.println("Angle = " + doaAngleCourant + "\t\tVoix = " + voiceActivityCourant + "\t\tSpeech = " + speechDetectedCourant);
        if (angleDifferent || voiceActivityDifferent || speechDetectedDifferent) {
            boolean angleVoixDifferent = Math.abs(calculerDifferenceAngles(angle, angleVoixCourant)) > 0;
            if (angleVoixDifferent) {
                angleVoixCourant = angle;
            }
            if (voiceActivityCourant && angleVoixDifferent)
            // TODO envoyer un évènement
            tournerCou();
        }
    }

    private void tournerCou() {
        if (angleVoixCourant < 60 || angleVoixCourant > 300) {
            MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
            mouvementCouEvent.setAccelerationGaucheDroite(100D);
            mouvementCouEvent.setVitesseGaucheDroite(60D);
            if (angleVoixCourant < 60) {
                mouvementCouEvent.setPositionGaucheDroite(angleVoixCourant);
            } else {
                mouvementCouEvent.setPositionGaucheDroite(angleVoixCourant - 360);
            }
            mouvementCouEvent.setSynchrone(false);
            RobotEventBus.getInstance().publishAsync(mouvementCouEvent);
        }
    }

    private int calculerDifferenceAngles(int angle1, int angle2) {
        int phi = Math.abs(angle1 - angle2) % 360;       // This is either the distance or 360 - distance
        int difference = phi > 180 ? 360 - phi : phi;
        //System.out.println("Courant = " + angle2 + ", angle = " + angle1 + ", difference = " + difference);
        return difference;
    }

    public static void main(String args[]) {
        CapteurActiviteSon capteurActiviteSon = new CapteurActiviteSon();
        capteurActiviteSon.initialiser();
    }
}
