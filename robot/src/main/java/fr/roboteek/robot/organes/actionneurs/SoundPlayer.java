package fr.roboteek.robot.organes.actionneurs;

import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerException;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import net.engio.mbassy.listener.Handler;
import org.apache.log4j.Logger;

import java.io.File;

public class SoundPlayer extends AbstractOrgane {

    private StreamPlayer streamPlayer;

    /** Logger. */
    private Logger logger = Logger.getLogger(OrganeParoleEspeak.class);

    /** Constructeur. */
    public SoundPlayer() {
        super();
        streamPlayer = new StreamPlayer();
    }

    /**
     * Joue un son.
     * @param sound le son à jouer
     */
    public void play(RobotSound sound) {
        if(sound != null) {
            if (streamPlayer.isPlaying()) {
                streamPlayer.stop();
            }

            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE);
            RobotEventBus.getInstance().publishAsync(eventPause);

            System.out.println("Lecture son :\t" + sound);

            try {
                streamPlayer.open(new File(Constantes.DOSSIER_SONS + File.separator + sound.getFileName()));
                streamPlayer.play();
            } catch (StreamPlayerException e) {
                logger.error("Ereur lors de la lecture d'un son", e);
                e.printStackTrace();
            }

            logger.debug("Fin lecture :\t" + sound);

            // Envoi d'un évènement pour redémarrer la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventRedemarrage = new ReconnaissanceVocaleControleEvent();
            eventRedemarrage.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.DEMARRER);
            RobotEventBus.getInstance().publish(eventRedemarrage);
        }
    }

    /**
     * Intercepte les évènements pour jouer un son.
     * @param playSoundEvent évènement pour jouer un son
     */
    @Handler
    public void handlePlaySoundEvent(PlaySoundEvent playSoundEvent) {
        if (playSoundEvent.getSound() != null) {
            play(playSoundEvent.getSound());
        }
    }

    @Override
    public void initialiser() {

    }

    @Override
    public void arreter() {

    }

    public static void main(String[] args) {
        final SoundPlayer soundPlayer = new SoundPlayer();
        soundPlayer.play(RobotSound.WALLE);
    }
}
