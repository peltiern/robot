package fr.roboteek.robot.organes.actionneurs.animation;

import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.organes.actionneurs.RobotSound;
import fr.roboteek.robot.organes.actionneurs.Yeux;
import fr.roboteek.robot.systemenerveux.event.MouvementCouEvent;
import fr.roboteek.robot.systemenerveux.event.MouvementYeuxEvent;
import fr.roboteek.robot.systemenerveux.event.PlaySoundEvent;

public class AnimationStep {

    /** Délai avant de jouer cette étape d'animation (en ms). */
    private long delay;

    /** Angle du roulis (si neutre, non pris en compte). */
    private double angleRoulis = MouvementCouEvent.ANGLE_NEUTRE;

    /** Vitesse du roulis. */
    private double vitesseRoulis = Yeux.DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE;

    /** Accélération du roulis. */
    private double accelerationRoulis = Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE;;

    /** Position de l'oeil gauche (0 : en bas, 180 : en haut). */
    private double positionOeilGauche = MouvementYeuxEvent.POSITION_NEUTRE;

    /** Vitesse de l'oeil gauche. */
    private double vitesseOeilGauche = Yeux.DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE;

    /** Accélération de l'oeil gauche. */
    private double accelerationOeilGauche = Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE;

    /** Position de l'oeil droit (0 : en bas, 180 : en haut). */
    private double positionOeilDroit = MouvementYeuxEvent.POSITION_NEUTRE;

    /** Vitesse de l'oeil droit. */
    private double vitesseOeilDroit = Yeux.DEFAULT_SPEED_MOTEUR_OEIL_DROIT;

    /** Accélération de l'oeil droit. */
    private double accelerationOeilDroit = Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_DROIT;

    /** Position du cou "Gauche - Droite" (0 : à gauche, 180 : à droite). */
    private double positionCouGaucheDroite = MouvementCouEvent.POSITION_NEUTRE;

    /** Vitesse du cou "Gauche - Droite". */
    private double vitesseCouGaucheDroite = Cou.DEFAULT_SPEED_MOTEUR_GAUCHE_DROITE;

    /** Accélération cou "Gauche - Droite" */
    private double accelerationCouGaucheDroite = Cou.DEFAULT_ACCELERATION_MOTEUR_GAUCHE_DROITE;

    /** Position "Haut - Bas" (0 : en bas, 180 : en haut). */
    private double positionCouHautBas = MouvementCouEvent.POSITION_NEUTRE;;

    /** Vitesse du cou "Haut - Bas". */
    private double vitesseCouHautBas = Cou.DEFAULT_SPEED_MOTEUR_HAUT_BAS;

    /** Accélération cou "Haut - Bas" */
    private double accelerationCouHautBas = Cou.DEFAULT_ACCELERATION_MOTEUR_HAUT_BAS;

    /** Son à jouer. */
    private RobotSound sound;

    public AnimationStep() {

    }

    /** Constructeur sans roulis. */
    public AnimationStep(long delay, double positionOeilGauche, double positionOeilDroit, double positionCouGaucheDroite, double positionCouHautBas, RobotSound sound) {
        this(delay,
                positionOeilGauche, Yeux.DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE, Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE,
                positionOeilDroit, Yeux.DEFAULT_SPEED_MOTEUR_OEIL_DROIT, Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_DROIT,
                positionCouGaucheDroite, Cou.DEFAULT_SPEED_MOTEUR_GAUCHE_DROITE, Cou.DEFAULT_ACCELERATION_MOTEUR_GAUCHE_DROITE,
                positionCouHautBas, Cou.DEFAULT_SPEED_MOTEUR_HAUT_BAS, Cou.DEFAULT_ACCELERATION_MOTEUR_HAUT_BAS,
                sound);
    }

    /**
     * Constructeur pour effectuer un roulis. */
    public AnimationStep(long delay, double angleRoulis, double positionCouGaucheDroite, double positionCouHautBas, RobotSound sound) {
        this(delay,
                angleRoulis, Yeux.DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE, Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE,
                positionCouGaucheDroite, Cou.DEFAULT_SPEED_MOTEUR_GAUCHE_DROITE, Cou.DEFAULT_ACCELERATION_MOTEUR_GAUCHE_DROITE,
                positionCouHautBas, Cou.DEFAULT_SPEED_MOTEUR_HAUT_BAS, Cou.DEFAULT_ACCELERATION_MOTEUR_HAUT_BAS,
                sound);
    }

    /** Constructeur avec vitesse et accélération sans roulis. */
    public AnimationStep(long delay,
                         double positionOeilGauche, double vitesseOeilGauche, double accelerationOeilGauche,
                         double positionOeilDroit, double vitesseOeilDroit, double accelerationOeilDroit,
                         double positionCouGaucheDroite, double vitesseCouGaucheDroite, double accelerationCouGaucheDroite,
                         double positionCouHautBas, double vitesseCouHautBas, double accelerationCouHautBas,
                         RobotSound sound) {
        this.delay = delay;
        this.angleRoulis = MouvementCouEvent.ANGLE_NEUTRE;
        this.vitesseRoulis = Yeux.DEFAULT_SPEED_MOTEUR_OEIL_GAUCHE;
        this.accelerationRoulis = Yeux.DEFAULT_ACCELERATION_MOTEUR_OEIL_GAUCHE;
        this.positionOeilGauche = positionOeilGauche;
        this.vitesseOeilGauche = vitesseOeilGauche;
        this.accelerationOeilGauche = accelerationOeilGauche;
        this.positionOeilDroit = positionOeilDroit;
        this.vitesseOeilDroit = vitesseOeilDroit;
        this.accelerationOeilDroit = accelerationOeilDroit;
        this.positionCouGaucheDroite = positionCouGaucheDroite;
        this.vitesseCouGaucheDroite = vitesseCouGaucheDroite;
        this.accelerationCouGaucheDroite = accelerationCouGaucheDroite;
        this.positionCouHautBas = positionCouHautBas;
        this.vitesseCouHautBas = vitesseCouHautBas;
        this.accelerationCouHautBas = accelerationCouHautBas;
        this.sound = sound;
    }

    /** Constructeur pour effectuer un roulis. */
    public AnimationStep(long delay,
                         double angleRoulis, double vitesseRoulis, double accelerationRoulis,
                         double positionCouGaucheDroite, double vitesseCouGaucheDroite, double accelerationCouGaucheDroite,
                         double positionCouHautBas, double vitesseCouHautBas, double accelerationCouHautBas,
                         RobotSound sound) {
        this.delay = delay;
        this.angleRoulis = angleRoulis;
        this.vitesseRoulis = vitesseRoulis;
        this.accelerationRoulis = accelerationRoulis;
        this.positionOeilGauche = MouvementYeuxEvent.POSITION_NEUTRE;
        this.positionOeilDroit = MouvementYeuxEvent.POSITION_NEUTRE;
        this.positionCouGaucheDroite = positionCouGaucheDroite;
        this.vitesseCouGaucheDroite = vitesseCouGaucheDroite;
        this.accelerationCouGaucheDroite = accelerationCouGaucheDroite;
        this.positionCouHautBas = positionCouHautBas;
        this.vitesseCouHautBas = vitesseCouHautBas;
        this.accelerationCouHautBas = accelerationCouHautBas;
        this.sound = sound;
    }

    public MouvementYeuxEvent buildMouvementYeuxEvent() {
        MouvementYeuxEvent mouvementYeuxEvent = new MouvementYeuxEvent();
        mouvementYeuxEvent.setPositionOeilGauche(positionOeilGauche);
        mouvementYeuxEvent.setVitesseOeilGauche(vitesseOeilGauche);
        mouvementYeuxEvent.setAccelerationOeilGauche(accelerationOeilGauche);
        mouvementYeuxEvent.setPositionOeilDroit(positionOeilDroit);
        mouvementYeuxEvent.setVitesseOeilDroit(vitesseOeilDroit);
        mouvementYeuxEvent.setAccelerationOeilDroit(accelerationOeilDroit);
        return mouvementYeuxEvent;
    }

    public MouvementCouEvent buildMouvementCouEvent() {
        MouvementCouEvent mouvementCouEvent = new MouvementCouEvent();
        if (angleRoulis != MouvementCouEvent.ANGLE_NEUTRE) {
            mouvementCouEvent.setMouvementRoulis(MouvementCouEvent.MOUVEMENTS_ROULIS.HORAIRE);
            mouvementCouEvent.setPositionRoulis(angleRoulis);
            mouvementCouEvent.setVitesseRoulis(vitesseRoulis);
            mouvementCouEvent.setAccelerationRoulis(accelerationRoulis);
        } else {
            mouvementCouEvent.setPositionGaucheDroite(positionCouGaucheDroite);
            mouvementCouEvent.setVitesseGaucheDroite(vitesseCouGaucheDroite);
            mouvementCouEvent.setAccelerationGaucheDroite(accelerationCouGaucheDroite);
            mouvementCouEvent.setPositionHautBas(positionCouHautBas);
            mouvementCouEvent.setVitesseHautBas(vitesseCouHautBas);
            mouvementCouEvent.setAccelerationHautBas(accelerationCouHautBas);
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
