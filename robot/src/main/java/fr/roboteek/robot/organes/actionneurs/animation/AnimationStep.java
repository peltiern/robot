package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.RobotSound;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;

public class AnimationStep {

    /** Délai avant de jouer cette étape d'animation (en ms). */
    private long delay;

    /** Angle du roulis (si neutre, non pris en compte). */
    private double angleRoulis;

    /** Position de l'oeil gauche (0 : en bas, 180 : en haut). */
    private double positionOeilGauche;

    /** Position de l'oeil droit (0 : en bas, 180 : en haut). */
    private double positionOeilDroit;

    /** Position du cou "Gauche - Droite" (0 : à gauche, 180 : à droite). */
    private double positionCouGaucheDroite;

    /** Position "Haut - Bas" (0 : en bas, 180 : en haut). */
    private double positionCouHautBas;

    /** Son à jouer. */
    private RobotSound sound;

    /** Constructeur sans roulis. */
    public AnimationStep(long delay, double positionOeilGauche, double positionOeilDroit, double positionCouGaucheDroite, double positionCouHautBas, RobotSound sound) {
        this.delay = delay;
        this.angleRoulis = MouvementCouEvent.ANGLE_NEUTRE;
        this.positionOeilGauche = positionOeilGauche;
        this.positionOeilDroit = positionOeilDroit;
        this.positionCouGaucheDroite = positionCouGaucheDroite;
        this.positionCouHautBas = positionCouHautBas;
        this.sound = sound;
    }

    /**
     * Constructeur pour effectuer un roulis. */
    public AnimationStep(long delay, double angleRoulis, double positionCouGaucheDroite, double positionCouHautBas, RobotSound sound) {
        this.delay = delay;
        this.angleRoulis = angleRoulis;
        this.positionOeilGauche = MouvementYeuxEvent.POSITION_NEUTRE;
        this.positionOeilDroit = MouvementYeuxEvent.POSITION_NEUTRE;
        this.positionCouGaucheDroite = positionCouGaucheDroite;
        this.positionCouHautBas = positionCouHautBas;
        this.sound = sound;
    }

    public MouvementYeuxEvent buildMouvementYeuxEvent() {
        MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
        mouvementYeuxEvent.setPositionOeilGauche(positionOeilGauche);
        mouvementYeuxEvent.setPositionOeilDroit(positionOeilDroit);
        return mouvementYeuxEvent;
    }

    public MouvementCouEvent buildMouvementCouEvent() {
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        if (angleRoulis != MouvementCouEvent.ANGLE_NEUTRE) {
            mouvementCouEvent.setMouvementRoulis(MouvementCouEvent.MOUVEMENTS_ROULIS.HORAIRE);
            mouvementCouEvent.setPositionRoulis(angleRoulis);
        } else {
            mouvementCouEvent.setPositionGaucheDroite(positionCouGaucheDroite);
            mouvementCouEvent.setPositionHautBas(positionCouHautBas);
        }
        return mouvementCouEvent;
    }

    public PlaySoundEvent buildPlaySoundEvent() {
        if (sound != null) {
            PlaySoundEvent playSoundEvent = new PlaySoundEvent();
            playSoundEvent.setSound(sound);
            return playSoundEvent;
        } else {
            return null;
        }
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }


    @Override
    public String toString() {
        return "AnimationStep{" +
                "delay=" + delay +
                ", angleRoulis=" + angleRoulis +
                ", positionOeilGauche=" + positionOeilGauche +
                ", positionOeilDroit=" + positionOeilDroit +
                ", positionCouGaucheDroite=" + positionCouGaucheDroite +
                ", positionCouHautBas=" + positionCouHautBas +
                ", sound=" + sound +
                '}';
    }
}
