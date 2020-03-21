package fr.roboteek.robot.sandbox.audio;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class AudioPlayerTest {

    public static void main(String args[]) throws Exception {
        InputStream in = new FileInputStream(new File("/home/npeltier/Robot/Programme/sounds/walle.ogg"));
        AudioStream audioStream = new AudioStream(in);
        AudioPlayer.player.start(audioStream);
    }
}
