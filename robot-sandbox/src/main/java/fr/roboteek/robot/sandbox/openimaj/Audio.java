package fr.roboteek.robot.sandbox.openimaj;

import org.openimaj.audio.AudioPlayer;
import org.openimaj.audio.AudioStream;
import org.openimaj.video.xuggle.XuggleAudio;

public class Audio {

    /**
     * The file we're going to play
     */
    public final static String AUDIO_FILE = "/home/npeltier/Robot/Programme/sounds/walle.mp3";

    /**
     * Default constructor
     */
    public Audio() {
        XuggleAudio s = new XuggleAudio(AUDIO_FILE);

        playNormalSound(s);
//		playProcessedSound( s );
    }

    /**
     * Plays a sound through the audio API.
     */
    protected void playNormalSound(AudioStream s) {
        AudioPlayer ap = new AudioPlayer(s);
        ap.run();
    }

    public static void main(String args[]) {
        new Audio();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
