package fr.roboteek.robot.organes.capteurs;

import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.util.respeaker.MicArrayV2;

public class CapteurActiviteSon extends AbstractOrgane {

    private MicArrayV2 micArrayV2;

    /** Flag indiquant de stopper le thread. */
    private boolean stopperThread = false;

    private int doaAngleCourant;

    private boolean voiceActivityCourant;

    private boolean speechDetectedCourant;

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
        boolean angleDifferent = Math.abs(angle - doaAngleCourant) > 5;
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
        if (angleDifferent || voiceActivityDifferent || speechDetectedDifferent) {
            // TODO envoyer un évènement
            System.out.println("Angle = " + doaAngleCourant + "\t\tVoix = " + voiceActivityCourant + "\t\tSpeech = " + speechDetectedCourant);
        }
    }

    public static void main(String args[]) {
        CapteurActiviteSon capteurActiviteSon = new CapteurActiviteSon();
        capteurActiviteSon.initialiser();
    }
}
