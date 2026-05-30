package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger la tête.
 * Les champs de position et d'angle sont null quand l'axe n'est pas concerné par l'événement.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MouvementCouEvent extends RobotEvent {

    public static final String EVENT_TYPE = "mouvement-cou";

    public enum MOUVEMENTS_PANORAMIQUE {TOURNER_GAUCHE, TOURNER_DROITE, STOPPER}

    public enum MOUVEMENTS_INCLINAISON {TOURNER_HAUT, TOURNER_BAS, STOPPER}

    public enum MOUVEMENTS_MONTER_DESCENDRE {MONTER, DESCENDRE, STOPPER}

    public enum MOUVEMENTS_ROULIS {HORAIRE, ANTI_HORAIRE, STOPPER}

    /** Mouvement continu panoramique. */
    private MOUVEMENTS_PANORAMIQUE mouvementPanoramique;

    /** Position absolue panoramique en degrés. Null = pas de commande. */
    private Double positionPanoramique;

    /** Angle relatif panoramique en degrés. Null = pas de commande. */
    private Double anglePanoramique;

    private Double vitessePanoramique;
    private Double accelerationPanoramique;

    /** Mouvement continu inclinaison. */
    private MOUVEMENTS_INCLINAISON mouvementInclinaison;

    /** Position absolue inclinaison en degrés. Null = pas de commande. */
    private Double positionInclinaison;

    /** Angle relatif inclinaison en degrés. Null = pas de commande. */
    private Double angleInclinaison;

    private Double vitesseInclinaison;
    private Double accelerationInclinaison;

    /** Mouvement continu monter/descendre. */
    private MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre;

    /** Position absolue monter/descendre en degrés. Null = pas de commande. */
    private Double positionMonterDescendre;

    /** Angle relatif monter/descendre en degrés. Null = pas de commande. */
    private Double angleMonterDescendre;

    private Double vitesseMonterDescendre;
    private Double accelerationMonterDescendre;

    /** Mouvement de roulis. */
    private MOUVEMENTS_ROULIS mouvementRoulis;

    /** Position du roulis en degrés. Null = pas de commande. */
    private Double positionRoulis;

    private Double vitesseRoulis;
    private Double accelerationRoulis;

    /** Flag indiquant que le mouvement doit être synchrone. */
    private boolean synchrone = false;

    public MouvementCouEvent() {
        super(EVENT_TYPE);
    }

    public MouvementCouEvent(MOUVEMENTS_PANORAMIQUE mouvementPanoramique, Double positionPanoramique,
                              MOUVEMENTS_INCLINAISON mouvementInclinaison, Double positionInclinaison,
                              MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre, Double positionMonterDescendre,
                              MOUVEMENTS_ROULIS mouvementRoulis, Double positionRoulis) {
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

    public MouvementCouEvent(MOUVEMENTS_PANORAMIQUE mouvementPanoramique, Double positionPanoramique) {
        this(mouvementPanoramique, positionPanoramique, null, null, null, null, null, null);
    }

    public MouvementCouEvent(MOUVEMENTS_INCLINAISON mouvementInclinaison, Double positionInclinaison) {
        this(null, null, mouvementInclinaison, positionInclinaison, null, null, null, null);
    }

    public MouvementCouEvent(MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre, Double positionMonterDescendre) {
        this(null, null, null, null, mouvementMonterDescendre, positionMonterDescendre, null, null);
    }

    public MouvementCouEvent(MOUVEMENTS_ROULIS mouvementRoulis, Double positionRoulis) {
        this(null, null, null, null, null, null, mouvementRoulis, positionRoulis);
    }

    public MOUVEMENTS_PANORAMIQUE getMouvementPanoramique() { return mouvementPanoramique; }
    public void setMouvementPanoramique(MOUVEMENTS_PANORAMIQUE mouvementPanoramique) { this.mouvementPanoramique = mouvementPanoramique; }

    public Double getPositionPanoramique() { return positionPanoramique; }
    public void setPositionPanoramique(Double positionPanoramique) { this.positionPanoramique = positionPanoramique; }

    public Double getAnglePanoramique() { return anglePanoramique; }
    public void setAnglePanoramique(Double anglePanoramique) { this.anglePanoramique = anglePanoramique; }

    public Double getVitessePanoramique() { return vitessePanoramique; }
    public void setVitessePanoramique(Double vitessePanoramique) { this.vitessePanoramique = vitessePanoramique; }

    public Double getAccelerationPanoramique() { return accelerationPanoramique; }
    public void setAccelerationPanoramique(Double accelerationPanoramique) { this.accelerationPanoramique = accelerationPanoramique; }

    public MOUVEMENTS_INCLINAISON getMouvementInclinaison() { return mouvementInclinaison; }
    public void setMouvementInclinaison(MOUVEMENTS_INCLINAISON mouvementInclinaison) { this.mouvementInclinaison = mouvementInclinaison; }

    public Double getPositionInclinaison() { return positionInclinaison; }
    public void setPositionInclinaison(Double positionInclinaison) { this.positionInclinaison = positionInclinaison; }

    public Double getAngleInclinaison() { return angleInclinaison; }
    public void setAngleInclinaison(Double angleInclinaison) { this.angleInclinaison = angleInclinaison; }

    public Double getVitesseInclinaison() { return vitesseInclinaison; }
    public void setVitesseInclinaison(Double vitesseInclinaison) { this.vitesseInclinaison = vitesseInclinaison; }

    public Double getAccelerationInclinaison() { return accelerationInclinaison; }
    public void setAccelerationInclinaison(Double accelerationInclinaison) { this.accelerationInclinaison = accelerationInclinaison; }

    public MOUVEMENTS_MONTER_DESCENDRE getMouvementMonterDescendre() { return mouvementMonterDescendre; }
    public void setMouvementMonterDescendre(MOUVEMENTS_MONTER_DESCENDRE mouvementMonterDescendre) { this.mouvementMonterDescendre = mouvementMonterDescendre; }

    public Double getPositionMonterDescendre() { return positionMonterDescendre; }
    public void setPositionMonterDescendre(Double positionMonterDescendre) { this.positionMonterDescendre = positionMonterDescendre; }

    public Double getAngleMonterDescendre() { return angleMonterDescendre; }
    public void setAngleMonterDescendre(Double angleMonterDescendre) { this.angleMonterDescendre = angleMonterDescendre; }

    public Double getVitesseMonterDescendre() { return vitesseMonterDescendre; }
    public void setVitesseMonterDescendre(Double vitesseMonterDescendre) { this.vitesseMonterDescendre = vitesseMonterDescendre; }

    public Double getAccelerationMonterDescendre() { return accelerationMonterDescendre; }
    public void setAccelerationMonterDescendre(Double accelerationMonterDescendre) { this.accelerationMonterDescendre = accelerationMonterDescendre; }

    public MOUVEMENTS_ROULIS getMouvementRoulis() { return mouvementRoulis; }
    public void setMouvementRoulis(MOUVEMENTS_ROULIS mouvementRoulis) { this.mouvementRoulis = mouvementRoulis; }

    public Double getPositionRoulis() { return positionRoulis; }
    public void setPositionRoulis(Double positionRoulis) { this.positionRoulis = positionRoulis; }

    public Double getVitesseRoulis() { return vitesseRoulis; }
    public void setVitesseRoulis(Double vitesseRoulis) { this.vitesseRoulis = vitesseRoulis; }

    public Double getAccelerationRoulis() { return accelerationRoulis; }
    public void setAccelerationRoulis(Double accelerationRoulis) { this.accelerationRoulis = accelerationRoulis; }

    public boolean isSynchrone() { return synchrone; }
    public void setSynchrone(boolean synchrone) { this.synchrone = synchrone; }

    @Override
    public String toString() {
        return "MouvementCouEvent{" +
                "panoramique=" + positionPanoramique + "/" + anglePanoramique + "/" + mouvementPanoramique +
                ", inclinaison=" + positionInclinaison + "/" + angleInclinaison + "/" + mouvementInclinaison +
                ", monterDescendre=" + positionMonterDescendre + "/" + angleMonterDescendre + "/" + mouvementMonterDescendre +
                ", roulis=" + positionRoulis + "/" + mouvementRoulis +
                ", synchrone=" + synchrone + "}";
    }
}
