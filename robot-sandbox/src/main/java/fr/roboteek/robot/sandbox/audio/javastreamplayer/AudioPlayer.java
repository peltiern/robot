package fr.roboteek.robot.sandbox.audio.javastreamplayer;

import com.goxr3plus.streamplayer.stream.StreamPlayer;

import java.io.File;

public class AudioPlayer {

    public static void main(String args[]) throws Exception {
        StreamPlayer streamPlayer = new StreamPlayer();
        for(int i = 0; i < 3; i++) {
            streamPlayer.open(new File("/home/npeltier/Robot/Programme/sounds/walle.ogg"));
            streamPlayer.play();
            //Thread.sleep(500);
            //streamPlayer.stop();
            streamPlayer.open(new File("/home/npeltier/Robot/Programme/sounds/oh.ogg"));
            streamPlayer.play();
        }
    }
}
