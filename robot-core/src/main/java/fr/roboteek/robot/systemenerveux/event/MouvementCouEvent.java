package fr.roboteek.robot.systemenerveux.event;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Evènement pour bouger la tête.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
@Entity
@DiscriminatorValue(MouvementCouEvent.EVENT_TYPE)
public class MouvementCouEvent extends RobotEvent {

    public static final String EVENT_TYPE = "mouvement-cou";

    public enum MOUVEMENTS_PANORAMIQUE {TOURNER_GAUCHE, TOURNER_DROITE, STOPPER}

    public enum MOUVEMENTS_INCLINAISON {TOURNER_HAUT, TOURNER_BAS, STOPPER}

    public enum MOUVEMENTS_MONTER_DESCENDRE {MONTER, DESCENDRE, STOPPER}

    public enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER}

    public static final double POSITION_NEUTRE = 99999;

    public static final double ANGLE_NEUTRE = 99999;

    public static final double VITESSE_NEUTRE = 99999;

    public static final double ACCELERATION_NEUTRE = 99999;

    /**
     * Mouvement "Panoramique à effectuer.
     */
    @Column(name = "text_1")
    private MOUVEMENTS_PANORAMIQUE mouvementPanoramique;

    /**
     * Position "Panoramique" (0 : à gauche, 180 : à droite).
     */
    @Column(name = "double_1")
    private double positionPanoramique = POSITION_NEUTRE;

    /**
     * Angle "Panoramique".
     */
    @Column(name = "double_2")
    private double anglePanoramique = ANGLE_NEUTRE;

    /**
     * Vitesse du mouvement "Panoramique".
     */
    @Column(name = "double_3")
    private Double vitessePanoramique;

    /**
     * Accélération du mouvement "Panoramique".
     */
    @Column(name = "double_4")
    private Double accelerationPanoramique;

    /**
     * Mouvement "Inclinaison" à effectuer.
     */
    @Column(name = "text_2")
    private MOUVEMENTS_INCLINAISON mouvementInclinaison;

    /**
     * Position "Inclinaison" (0 : en bas, 180 : en haut).
     */
    @Column(name = "double_5")
    private double positionInclinaison = POSITION_NEUTRE;

    /**
     * Angle "Inclinaison".
     */
    @Column(name = "double_6")
    private double angleInclinaison = ANGLE_NEUTRE;

    /**
     * Vitesse du mouvement "Inclinaison".
     */
    @Column(name = "double_7")
    private Double vitesseInclinaison;

    /**
     * Accélération du mouvement "Inclinaison".
     */
    @Column(name = "double_8")
    private Double accelerationInclinaison;

    /**
     * Mouvement "Monter - Descendre" à effectuer.
     */
    @Column(name = "text_3")
    private MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre;

    /**
     * Position "Monter - Descendre" (0 : en bas, 180 : en haut).
     */
    @Column(name = "double_9")
    private double positionMonterDescendre = POSITION_NEUTRE;

    /**
     * Angle "Monter - Descendre".
     */
    @Column(name = "double_10")
    private double angleMonterDescendre = ANGLE_NEUTRE;

    /**
     * Vitesse du mouvement "Monter - Descendre".
     */
    @Column(name = "double_11")
    private Double vitesseMonterDescendre;

    /**
     * Accélération du mouvement "Monter - Descendre".
     */
    @Column(name = "double_12")
    private Double accelerationMonterDescendre;

    /**
     * Mouvement "Roulis" à effectuer.
     */
    @Column(name = "text_4")
    private MOUVEMENTS_ROULIS mouvementRoulis;

    /**
     * Position du roulis (-90 : en bas, 90 : en haut).
     */
    private double positionRoulis = POSITION_NEUTRE;

    /**
     * Vitesse du mouvement "Roulis".
     */
    @Column(name = "double_13")
    private Double vitesseRoulis;

    /**
     * Accélération du mouvement "Roulis".
     */
    @Column(name = "double_14")
    private Double accelerationRoulis;

    /**
     * Flag indiquant que le mouvement doit être synchrone.
     */
    @Column(name = "boolean_1")
    private boolean synchrone = false;

    public MouvementCouEvent() {
        super(EVENT_TYPE);
    }

    public MouvementCouEvent(MOUVEMENTS_PANORAMIQUE mouvementPanoramique, double positionPanoramique,
                             MOUVEMENTS_INCLINAISON mouvementInclinaison, double positionInclinaison,
                             MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre, double positionMonterDescendre,
                             MOUVEMENTS_ROULIS mouvementRoulis, double positionRoulis) {
        this();
        this.mouvementPanoramique = mouvementPanoramique;
        this.positionPanoramique = positionPanoramique;
        this.mouvementInclinaison = mouvementInclinaison;
        this.positionInclinaison = positionInclinaison;
        this.mouvementMonterDescendre = mouvementMonterDescendre;
        this.positionMonterDescendre = positionMonterDescendre;
        this.mouvementRoulis = mouvementRoulis;
        this.positionRoulis = positionRoulis;
    }

    public MouvementCouEvent(MOUVEMENTS_PANORAMIQUE mouvementPanoramique, double positionPanoramique) {
        this(mouvementPanoramique, positionPanoramique, null, POSITION_NEUTRE, null, POSITION_NEUTRE, null, POSITION_NEUTRE);
    }

    public MouvementCouEvent(MOUVEMENTS_INCLINAISON mouvementInclinaison, double positionInclinaison) {
        this(null, POSITION_NEUTRE, mouvementInclinaison, positionInclinaison, null, POSITION_NEUTRE, null, POSITION_NEUTRE);
    }

    public MouvementCouEvent(MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre, double positionMonterDescendre) {
        this(null, POSITION_NEUTRE, null, POSITION_NEUTRE, mouvementMonterDescendre, positionMonterDescendre, null, POSITION_NEUTRE);
    }

    public MouvementCouEvent(MOUVEMENTS_ROULIS mouvementRoulis, double positionRoulis) {
        this(null, POSITION_NEUTRE, null, POSITION_NEUTRE, null, POSITION_NEUTRE, mouvementRoulis, positionRoulis);
    }

    public MOUVEMENTS_PANORAMIQUE getMouvementPanoramique() {
        return mouvementPanoramique;
    }

    public void setMouvementPanoramique(MOUVEMENTS_PANORAMIQUE mouvementPanoramique) {
        this.mouvementPanoramique = mouvementPanoramique;
    }

    public MOUVEMENTS_INCLINAISON getMouvementInclinaison() {
        return mouvementInclinaison;
    }

    public void setMouvementInclinaison(MOUVEMENTS_INCLINAISON mouvementHauBas) {
        this.mouvementInclinaison = mouvementHauBas;
    }

    public MOUVEMENTS_MONTER_DESCENDRE getMouvementMonterDescendre() {
        return mouvementMonterDescendre;
    }

    public void setMouvementMonterDescendre(MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre) {
        this.mouvementMonterDescendre = mouvementMonterDescendre;
    }

    public double getPositionPanoramique() {
        return positionPanoramique;
    }

    public void setPositionPanoramique(double positionPanoramique) {
        this.positionPanoramique = positionPanoramique;
    }

    public double getPositionInclinaison() {
        return positionInclinaison;
    }

    public void setPositionInclinaison(double positionInclinaison) {
        this.positionInclinaison = positionInclinaison;
    }

    public double getPositionMonterDescendre() {
        return positionMonterDescendre;
    }

    public void setPositionMonterDescendre(double positionMonterDescendre) {
        this.positionMonterDescendre = positionMonterDescendre;
    }

    public MOUVEMENTS_ROULIS getMouvementRoulis() {
        return mouvementRoulis;
    }

    public void setMouvementRoulis(MOUVEMENTS_ROULIS mouvementRoulis) {
        this.mouvementRoulis = mouvementRoulis;
    }

    public double getPositionRoulis() {
        return positionRoulis;
    }

    public void setPositionRoulis(double positionRoulis) {
        this.positionRoulis = positionRoulis;
    }

    public boolean isSynchrone() {
        return synchrone;
    }

    public void setSynchrone(boolean synchrone) {
        this.synchrone = synchrone;
    }

    public Double getVitessePanoramique() {
        return vitessePanoramique;
    }

    public void setVitessePanoramique(Double vitessePanoramique) {
        this.vitessePanoramique = vitessePanoramique;
    }

    public Double getAccelerationPanoramique() {
        return accelerationPanoramique;
    }

    public void setAccelerationPanoramique(Double accelerationPanoramique) {
        this.accelerationPanoramique = accelerationPanoramique;
    }

    public Double getVitesseInclinaison() {
        return vitesseInclinaison;
    }

    public void setVitesseInclinaison(Double vitesseInclinaison) {
        this.vitesseInclinaison = vitesseInclinaison;
    }

    public Double getAccelerationInclinaison() {
        return accelerationInclinaison;
    }

    public void setAccelerationInclinaison(Double accelerationInclinaison) {
        this.accelerationInclinaison = accelerationInclinaison;
    }

    public Double getVitesseMonterDescendre() {
        return vitesseMonterDescendre;
    }

    public void setVitesseMonterDescendre(Double vitesseMonterDescendre) {
        this.vitesseMonterDescendre = vitesseMonterDescendre;
    }

    public Double getAccelerationMonterDescendre() {
        return accelerationMonterDescendre;
    }

    public void setAccelerationMonterDescendre(Double accelerationMonterDescendre) {
        this.accelerationMonterDescendre = accelerationMonterDescendre;
    }

    public Double getVitesseRoulis() {
        return vitesseRoulis;
    }

    public void setVitesseRoulis(Double vitesseRoulis) {
        this.vitesseRoulis = vitesseRoulis;
    }

    public Double getAccelerationRoulis() {
        return accelerationRoulis;
    }

    public void setAccelerationRoulis(Double accelerationRoulis) {
        this.accelerationRoulis = accelerationRoulis;
    }

    public double getAnglePanoramique() {
        return anglePanoramique;
    }

    public void setAnglePanoramique(double anglePanoramique) {
        this.anglePanoramique = anglePanoramique;
    }

    public double getAngleInclinaison() {
        return angleInclinaison;
    }

    public void setAngleInclinaison(double angleInclinaison) {
        this.angleInclinaison = angleInclinaison;
    }

    public double getAngleMonterDescendre() {
        return angleMonterDescendre;
    }

    public void setAngleMonterDescendre(double angleMonterDescendre) {
        this.angleMonterDescendre = angleMonterDescendre;
    }

    @Override
    public String toString() {
        return "MouvementCouEvent{" +
                "mouvementPanoramique=" + mouvementPanoramique +
                ", positionPanoramique=" + positionPanoramique +
                ", anglePanoramique=" + anglePanoramique +
                ", vitessePanoramique=" + vitessePanoramique +
                ", accelerationPanoramique=" + accelerationPanoramique +
                ", mouvementInclinaison=" + mouvementInclinaison +
                ", positionInclinaison=" + positionInclinaison +
                ", angleInclinaison=" + angleInclinaison +
                ", vitesseInclinaison=" + vitesseInclinaison +
                ", accelerationInclinaison=" + accelerationInclinaison +
                ", mouvementMonterDescendre=" + mouvementMonterDescendre +
                ", positionMonterDescendre=" + positionMonterDescendre +
                ", angleMonterDescendre=" + angleMonterDescendre +
                ", vitesseMonterDescendre=" + vitesseMonterDescendre +
                ", accelerationMonterDescendre=" + accelerationMonterDescendre +
                ", mouvementRoulis=" + mouvementRoulis +
                ", positionRoulis=" + positionRoulis +
                ", vitesseRoulis=" + vitesseRoulis +
                ", accelerationRoulis=" + accelerationRoulis +
                ", synchrone=" + synchrone +
                '}';
    }
}
