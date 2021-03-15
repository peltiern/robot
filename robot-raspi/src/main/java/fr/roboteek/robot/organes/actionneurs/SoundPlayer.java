package fr.roboteek.robot.organes.actionneurs;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.organes.AbstractOrgane;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;
import fr.roboteek.robot.systemenerveux.event.ReconnaissanceVocaleControleEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class SoundPlayer extends AbstractOrgane {

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(SoundPlayer.class);

    /**
     * Constructeur.
     */
    public SoundPlayer() {
        super();
    }

    /**
     * Joue un son.
     *
     * @param sound le son à jouer
     */
    public void play(RobotSound sound) {
        if (sound != null) {

            // Envoi d'un évènement pour mettre en pause la reconnaissance vocale
            final ReconnaissanceVocaleControleEvent eventPause = new ReconnaissanceVocaleControleEvent();
            eventPause.setControle(ReconnaissanceVocaleControleEvent.CONTROLE.METTRE_EN_PAUSE);
            RobotEventBus.getInstance().publishAsync(eventPause);

            System.out.println("Lecture son :\t" + sound);

            String[] params = {"play", Constantes.DOSSIER_SONS + File.separator + sound.getFileName()};
            try {
                Process p = Runtime.getRuntime().exec(params);
                p.waitFor();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
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
     *
     * @param playSoundEvent évènement pour jouer un son
     */
    @Subscribe
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
        soundPlayer.play(RobotSound.OH);
        soundPlayer.play(RobotSound.WOW);
        soundPlayer.play(RobotSound.SAD);
    }
}
